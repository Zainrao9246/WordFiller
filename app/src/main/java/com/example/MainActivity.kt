package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.FillwordsTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FillwordsViewModel

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
        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
        val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
        val gameState by viewModel.gameState.collectAsStateWithLifecycle()

        Crossfade(
          targetState = currentScreen,
          animationSpec = tween(durationMillis = 220),
          modifier = Modifier.fillMaxSize(),
          label = "screenCrossfade",
        ) { screen ->
          when (screen) {
            AppScreen.Home -> HomeScreen(
              progressMap = allProgress,
              stats = playerStats,
              onPlayClick = viewModel::playNextOrContinue,
              onPlaySignatureClick = viewModel::playSignatureClassicBoard,
              onBrowseLevelsClick = viewModel::navigateToLevelSelect,
              onStartEndlessClick = viewModel::startEndlessPuzzle,
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
                  onSolveWordHintClick = viewModel::useSolveWordHint,
                  onToggleHaptics = viewModel::toggleHaptics,
                  onNextLevelClick = viewModel::playNextLevel,
                  onBrowseLevelsClick = { viewModel.navigateToLevelSelect() },
                  onBack = viewModel::navigateHome,
                )
              }
            }
          }
        }
      }
    }
  }
}
