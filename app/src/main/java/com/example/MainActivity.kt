package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.ui.components.PinGateOverlay
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.games.AnagramRushScreen
import com.example.ui.screens.games.HangmanScreen
import com.example.ui.screens.games.MemoryMatchScreen
import com.example.ui.screens.games.MiniGamesHubScreen
import com.example.ui.screens.games.WordChainScreen
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.FillwordsTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FillwordsViewModel
import com.example.ui.viewmodel.MiniGameType
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val darkBarColor = DeepNavyBackground.toArgb()
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(darkBarColor),
      navigationBarStyle = SystemBarStyle.dark(darkBarColor),
    )
    setContent {
      FillwordsTheme {
        val viewModel: FillwordsViewModel = viewModel()
        val vaultViewModel: VaultViewModel = viewModel()

        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
        val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
        val gameState by viewModel.gameState.collectAsStateWithLifecycle()

        val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
        val hasPinConfigured by vaultViewModel.hasPinConfigured.collectAsStateWithLifecycle()
        val hasCustomDestructPin by vaultViewModel.hasCustomDestructPin.collectAsStateWithLifecycle()
        val blockScreenshots by vaultViewModel.blockScreenshots.collectAsStateWithLifecycle()
        val vaultMedia by vaultViewModel.mediaItems.collectAsStateWithLifecycle()
        val vaultMessages by vaultViewModel.messages.collectAsStateWithLifecycle()

        val isSensitiveScreenActive =
          currentScreen == AppScreen.Vault || vaultUiState.pinModalOpen

        // Hide Recent Apps thumbnails when Vault or PIN is open without blacking out live display.
        // Apply FLAG_SECURE only if the user explicitly opts in inside Vault -> Security.
        LaunchedEffect(isSensitiveScreenActive, blockScreenshots) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!isSensitiveScreenActive)
          }
          if (isSensitiveScreenActive && blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
          } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
          }
        }

        // Auto-lock Vault back to Fillwords Home instantly on minimize, app-switch, or background
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            val isLeaving = event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP
            if (isLeaving && vaultViewModel.shouldAutoLockOnStop()) {
              if (viewModel.currentScreen.value == AppScreen.Vault || vaultViewModel.uiState.value.pinModalOpen) {
                vaultViewModel.lockVault()
                viewModel.navigateHome()
              }
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 180),
            modifier = Modifier.fillMaxSize(),
            label = "screenCrossfade",
          ) { screen ->
            when (screen) {
              AppScreen.Home -> HomeScreen(
                progressMap = allProgress,
                stats = playerStats,
                todayDateKey = FillwordsViewModel.todayDateKey(),
                onHintBulbTap = vaultViewModel::onTopHintBulbTapped,
                onResetBulbTapSequence = vaultViewModel::resetBulbTapSequence,
                onPlayClick = viewModel::playNextOrContinue,
                onPlaySignatureClick = viewModel::playSignatureClassicBoard,
                onDailyChallengeClick = viewModel::startDailyChallenge,
                onBrowseLevelsClick = viewModel::navigateToLevelSelect,
                onStartEndlessClick = viewModel::startEndlessPuzzle,
                onMiniGamesClick = viewModel::navigateToMiniGames,
              )

              is AppScreen.LevelSelect -> LevelSelectScreen(
                initialPack = screen.selectedPack,
                progressMap = allProgress,
                onSelectPuzzle = { puzzle -> viewModel.startCuratedPuzzle(puzzle) },
                onBack = viewModel::navigateHome,
              )

              AppScreen.Playing -> {
                val activeGame = gameState
                if (activeGame != null) {
                  GameScreen(
                    state = activeGame,
                    stats = playerStats,
                    onTraceStart = viewModel::onTraceStart,
                    onTraceStep = viewModel::onTraceStep,
                    onTraceEnd = viewModel::onTraceEnd,
                    onUndoClick = viewModel::undoLastWord,
                    onResetClick = viewModel::resetBoard,
                    onLetterHintClick = viewModel::useLetterHint,
                    onHintSpecificWord = viewModel::hintSpecificWord,
                    onSolveWordHintClick = viewModel::useSolveWordHint,
                    onToggleHaptics = viewModel::toggleHaptics,
                    onNextLevelClick = viewModel::playNextLevel,
                    onBrowseLevelsClick = { viewModel.navigateToLevelSelect() },
                    onBack = viewModel::navigateHome,
                  )
                }
              }

              AppScreen.Vault -> {
                if (vaultUiState.isUnlocked) {
                  VaultScreen(
                    uiState = vaultUiState,
                    mediaItems = vaultMedia,
                    messages = vaultMessages,
                    hasPinConfigured = hasPinConfigured,
                    hasCustomDestructPin = hasCustomDestructPin,
                    onBeginSystemAction = vaultViewModel::beginExternalSystemAction,
                    onEndSystemAction = vaultViewModel::endExternalSystemAction,
                    onImportMediaUris = vaultViewModel::importMediaFromPicker,
                    onDeleteConfirmationResult = vaultViewModel::onOriginalDeleteConfirmationResult,
                    onClearPendingDelete = vaultViewModel::clearPendingGalleryDelete,
                    onConfirmSafDelete = vaultViewModel::confirmSafDocumentOriginalsDelete,
                    onDismissSafDelete = vaultViewModel::dismissSafDocumentOriginalsDelete,
                    onZeroTraceDeleteMedia = vaultViewModel::zeroTraceDeleteMedia,
                    onSaveMessage = vaultViewModel::saveMessage,
                    onZeroTraceDeleteMessage = vaultViewModel::zeroTraceDeleteMessage,
                    onChangePin = vaultViewModel::changePin,
                    onChangeDestructPin = vaultViewModel::changeDestructPin,
                    onResetDestructPin = vaultViewModel::resetDestructPinToDefault,
                    onToggleBlockScreenshots = vaultViewModel::toggleBlockScreenshots,
                    onWipeAllVault = {
                      vaultViewModel.zeroTraceWipeEverything {
                        viewModel.navigateHome()
                      }
                    },
                    onConsumeStatus = vaultViewModel::consumeStatusMessage,
                    onLockAndExit = {
                      vaultViewModel.lockVault()
                      viewModel.navigateHome()
                    },
                  )
                } else {
                  // Transient state (e.g. during auto-lock crossfade): never render vault
                  // content without an unlocked session. Show only the dark backdrop and
                  // bounce back to Home so the vault can never linger on screen.
                  LaunchedEffect(Unit) {
                    delay(700)
                    if (!vaultViewModel.uiState.value.isUnlocked) {
                      viewModel.navigateHome()
                    }
                  }
                  Box(modifier = Modifier.fillMaxSize().background(DeepNavyBackground))
                }
              }

              AppScreen.MiniGames -> MiniGamesHubScreen(
                onBack = viewModel::navigateHome,
                onSelectGame = viewModel::openMiniGame,
              )

              is AppScreen.MiniGamePlaying -> when (screen.game) {
                MiniGameType.ANAGRAM_RUSH -> AnagramRushScreen(onBack = viewModel::navigateToMiniGames)
                MiniGameType.WORD_CHAIN -> WordChainScreen(onBack = viewModel::navigateToMiniGames)
                MiniGameType.HANGMAN -> HangmanScreen(onBack = viewModel::navigateToMiniGames)
                MiniGameType.MEMORY_MATCH -> MemoryMatchScreen(onBack = viewModel::navigateToMiniGames)
              }
            }
          }

          AnimatedVisibility(
            visible = vaultUiState.pinModalOpen,
            enter = fadeIn(tween(140)),
            exit = ExitTransition.None,
          ) {
            PinGateOverlay(
              isSetupMode = vaultUiState.isSetupMode,
              errorMessage = vaultUiState.pinError,
              errorNonce = vaultUiState.pinErrorNonce,
              lockoutSeconds = vaultUiState.lockoutSeconds,
              onClearError = vaultViewModel::clearPinError,
              onSetupPin = { pin ->
                vaultViewModel.setupNewPin(pin) {
                  viewModel.navigateToVault()
                }
              },
              onSubmitPin = { pin ->
                vaultViewModel.submitUnlockPin(pin) {
                  viewModel.navigateToVault()
                }
              },
              onDismiss = vaultViewModel::dismissPinPrompt,
            )
          }
        }
      }
    }
  }
}
