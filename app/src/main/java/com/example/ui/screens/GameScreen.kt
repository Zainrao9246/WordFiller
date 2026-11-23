package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.FillwordsDatabase
import com.example.data.local.PlayerStatsEntity
import com.example.data.model.GridPos
import com.example.data.model.LevelType
import com.example.ui.components.FillwordBoard
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette
import com.example.ui.viewmodel.FeedbackTone
import com.example.ui.viewmodel.GameSessionState
import com.example.ui.viewmodel.TraceStepOutcome

import androidx.compose.material.icons.filled.LocalFireDepartment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
  state: GameSessionState,
  stats: PlayerStatsEntity,
  onTraceStart: (GridPos) -> Boolean,
  onTraceStep: (GridPos) -> TraceStepOutcome,
  onTraceEnd: () -> Boolean,
  onUndoClick: () -> Unit,
  onResetClick: () -> Unit,
  onLetterHintClick: () -> Unit,
  onHintSpecificWord: (String) -> Unit,
  onSolveWordHintClick: () -> Unit,
  onToggleHaptics: () -> Unit,
  onNextLevelClick: () -> Unit,
  onBrowseLevelsClick: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showHelpDialog by remember { mutableStateOf(false) }
  var showResetConfirm by remember { mutableStateOf(false) }
  var showBucketDialog by remember { mutableStateOf(false) }

  BackHandler {
    when {
      showHelpDialog -> showHelpDialog = false
      showResetConfirm -> showResetConfirm = false
      showBucketDialog -> showBucketDialog = false
      else -> onBack()
    }
  }

  val foundCount = state.foundWordKeys.size
  val totalWords = state.puzzle.placements.size
  val progressFraction = if (state.totalCellCount > 0) {
    state.solvedCellCount.toFloat() / state.totalCellCount.toFloat()
  } else {
    0f
  }

  val isBlitz = state.puzzle.levelType == LevelType.BLITZ && state.puzzle.targetSeconds > 0
  val blitzRemaining = state.blitzRemainingSeconds
  val timerUrgent = isBlitz && blitzRemaining in 1..15

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DeepNavyBackground),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("game_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = TextPrimary,
            )
          }

          Column(modifier = Modifier.padding(end = 6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = if (state.puzzle.levelNumber > 0) "Lv ${state.puzzle.levelNumber} • ${state.puzzle.title}" else state.puzzle.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "${state.puzzle.cols}×${state.puzzle.rows} • $foundCount/$totalWords words • ${state.puzzle.levelType.badgeLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (state.comboCount >= 2 && !state.isCompleted) {
            Surface(
              color = AccentCoral.copy(alpha = 0.22f),
              shape = RoundedCornerShape(50),
              modifier = Modifier.border(1.dp, AccentCoral.copy(alpha = 0.6f), RoundedCornerShape(50)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocalFireDepartment,
                  contentDescription = "Combo streak",
                  tint = AccentCoral,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "${state.comboCount}x",
                  style = MaterialTheme.typography.labelLarge,
                  color = AccentCoral,
                )
              }
            }
          }

          Surface(
            color = if (timerUrgent) AccentCoral.copy(alpha = 0.25f) else SlateSurfaceElevated,
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(
              width = 1.dp,
              color = if (isBlitz) AccentCoral.copy(alpha = 0.6f) else UnsolvedTileStroke.copy(alpha = 0.3f),
              shape = RoundedCornerShape(50),
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = if (isBlitz) Icons.Filled.Bolt else Icons.Filled.Timer,
                contentDescription = null,
                tint = if (isBlitz) AccentCoral else AccentSky,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (isBlitz && blitzRemaining > 0) formatSeconds(blitzRemaining) else formatSeconds(state.elapsedSeconds),
                style = MaterialTheme.typography.labelLarge,
                color = if (timerUrgent) AccentCoral else TextPrimary,
              )
            }
          }

          IconButton(
            onClick = onToggleHaptics,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("game_haptics_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Vibration,
              contentDescription = if (stats.hapticsEnabled) {
                stringResource(R.string.cd_haptics_on)
              } else {
                stringResource(R.string.cd_haptics_off)
              },
              tint = if (stats.hapticsEnabled) AccentEmerald else TextMuted,
            )
          }

          IconButton(
            onClick = { showHelpDialog = true },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("game_help_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.HelpOutline,
              contentDescription = stringResource(R.string.cd_help),
              tint = TextSecondary,
            )
          }
        }
      }

      // Tile Fill Progress + Interactive Extra Words Bucket Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        LinearProgressIndicator(
          progress = { progressFraction },
          modifier = Modifier
            .weight(1f)
            .height(7.dp)
            .clip(RoundedCornerShape(50)),
          color = AccentEmerald,
          trackColor = SlateSurfaceElevated,
        )

        Surface(
          color = SlateSurfaceElevated,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable { showBucketDialog = true }
            .testTag("extra_words_bucket_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = "Open extra words bucket",
              tint = AccentEmerald,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Bucket ${stats.extraWordsBucketCount}/${FillwordsDatabase.BUCKET_TARGET_WORDS}",
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary,
            )
          }
        }
      }

      // Live Trace Word / Cheerful Feedback Pill
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp),
        contentAlignment = Alignment.Center,
      ) {
        val tracedPreview = state.tracedWordPreview
        AnimatedContent(
          targetState = when {
            tracedPreview.isNotEmpty() -> "TRACE:$tracedPreview"
            state.feedback != null -> "FB:${state.feedback.id}:${state.feedback.message}:${state.feedback.tone}"
            else -> "IDLE"
          },
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "traceFeedback",
        ) { key ->
          when {
            key.startsWith("TRACE:") -> {
              val word = key.removePrefix("TRACE:")
              val pillColor = WordTilePalette[state.activeTraceColorIndex % WordTilePalette.size]
              Surface(
                color = pillColor,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = word.toCharArray().joinToString(" "),
                  fontFamily = OutfitFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp,
                  letterSpacing = 1.2.sp,
                  color = TextPrimary,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
              }
            }
            key.startsWith("FB:") -> {
              val fb = state.feedback
              val (bgColor, fgColor) = when (fb?.tone) {
                FeedbackTone.REWARD -> AccentGold.copy(alpha = 0.24f) to AccentGold
                FeedbackTone.BONUS -> AccentSky.copy(alpha = 0.22f) to AccentSky
                FeedbackTone.POSITIVE -> AccentEmerald.copy(alpha = 0.22f) to AccentEmerald
                FeedbackTone.WARNING, null -> AccentCoral.copy(alpha = 0.2f) to AccentCoral
              }
              Surface(
                color = bgColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, fgColor.copy(alpha = 0.48f), RoundedCornerShape(50)),
              ) {
                Text(
                  text = fb?.message.orEmpty(),
                  style = MaterialTheme.typography.labelLarge,
                  color = fgColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
              }
            }
            else -> {
              Text(
                text = "Drag connected letters • Extra words fill your Hint Bucket!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Main Interactive Board
      FillwordBoard(
        puzzle = state.puzzle,
        charGrid = state.charGrid,
        solvedCellMap = state.solvedCellMap,
        foundPlacements = state.foundPlacements,
        activeTrace = state.activeTrace,
        activeTraceColorIndex = state.activeTraceColorIndex,
        hintedCell = state.hintedCell,
        shakeTrigger = state.shakeTrigger,
        hapticsEnabled = stats.hapticsEnabled,
        onTraceStart = onTraceStart,
        onTraceStep = onTraceStep,
        onTraceEnd = onTraceEnd,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
      )

      // Remaining / Found Word Clues Row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(state.puzzle.placements, key = { it.word }) { placement ->
          val isSolved = placement.word in state.foundWordKeys
          val isHinted = state.hintedWord == placement.word
          val chipColor = WordTilePalette[placement.colorIndex % WordTilePalette.size]

          Surface(
            color = when {
              isSolved -> chipColor.copy(alpha = 0.22f)
              isHinted -> AccentGold.copy(alpha = 0.18f)
              else -> SlateSurface
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .border(
                width = 1.dp,
                color = when {
                  isSolved -> chipColor.copy(alpha = 0.7f)
                  isHinted -> AccentGold
                  else -> UnsolvedTileStroke.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(10.dp),
              )
              .clickable(enabled = !isSolved && !state.isCompleted) {
                onHintSpecificWord(placement.word)
              },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (isSolved) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  tint = chipColor,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = placement.word,
                  style = MaterialTheme.typography.labelMedium,
                  color = TextPrimary,
                )
              } else {
                val masked = if (isHinted) {
                  "${placement.word.first()}${"•".repeat(placement.word.length - 1)}"
                } else {
                  "•".repeat(placement.word.length)
                }
                Text(
                  text = "$masked (${placement.word.length})",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isHinted) AccentGold else TextSecondary,
                )
              }
            }
          }
        }
      }

      // Bottom Action Dock
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GameActionButton(
          icon = Icons.AutoMirrored.Filled.Undo,
          label = stringResource(R.string.game_undo),
          badge = null,
          enabled = state.foundWordKeys.isNotEmpty() && !state.isCompleted,
          accentColor = AccentSky,
          onClick = onUndoClick,
          modifier = Modifier
            .weight(1f)
            .testTag("game_undo_button"),
        )
        GameActionButton(
          icon = Icons.Filled.Refresh,
          label = stringResource(R.string.game_reset),
          badge = null,
          enabled = !state.isCompleted,
          accentColor = AccentCoral,
          onClick = {
            if (state.foundWordKeys.isNotEmpty()) {
              showResetConfirm = true
            } else {
              onResetClick()
            }
          },
          modifier = Modifier
            .weight(1f)
            .testTag("game_reset_button"),
        )
        GameActionButton(
          icon = Icons.Filled.Lightbulb,
          label = "${stringResource(R.string.game_hint_letter)} (1)",
          badge = "${stats.hintsRemaining}",
          enabled = !state.isCompleted,
          accentColor = AccentGold,
          onClick = onLetterHintClick,
          modifier = Modifier
            .weight(1.15f)
            .testTag("game_letter_hint_button"),
        )
        GameActionButton(
          icon = Icons.Filled.AutoFixHigh,
          label = "${stringResource(R.string.game_hint_word)} (2)",
          badge = null,
          enabled = !state.isCompleted,
          accentColor = AccentEmerald,
          onClick = onSolveWordHintClick,
          modifier = Modifier
            .weight(1.15f)
            .testTag("game_solve_hint_button"),
        )
      }
    }

    // Extra Words Bucket Dialog
    if (showBucketDialog) {
      AlertDialog(
        onDismissRequest = { showBucketDialog = false },
        containerColor = SlateSurface,
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = AccentEmerald)
            Text("Extra Words Bucket", color = TextPrimary)
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = "Every ${FillwordsDatabase.BUCKET_TARGET_WORDS} valid extra words you discover on any board earn +1 Free Hint!",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )

            Surface(
              color = SlateSurfaceElevated,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    "Bucket Progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                  )
                  Text(
                    "${stats.extraWordsBucketCount} / ${FillwordsDatabase.BUCKET_TARGET_WORDS}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentEmerald,
                  )
                }
                LinearProgressIndicator(
                  progress = {
                    stats.extraWordsBucketCount.toFloat() / FillwordsDatabase.BUCKET_TARGET_WORDS.toFloat()
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                  color = AccentEmerald,
                  trackColor = DeepNavyBackground,
                )
                Text(
                  text = "Lifetime extra words found: ${stats.totalExtraWordsFound}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                )
              }
            }

            if (state.bonusWordsFound.isEmpty()) {
              Text(
                text = "No extra words on this board yet — keep an eye out for hidden 3+ letter words!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
              )
            } else {
              Text(
                text = "Found on this board (${state.bonusWordsFound.size}):",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                state.bonusWordsFound.forEach { word ->
                  Surface(
                    color = AccentSky.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, AccentSky.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                  ) {
                    Text(
                      text = word,
                      style = MaterialTheme.typography.labelMedium,
                      color = TextPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = { showBucketDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
          ) {
            Text("Keep Playing", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm) {
      AlertDialog(
        onDismissRequest = { showResetConfirm = false },
        containerColor = SlateSurface,
        title = { Text("Reset Board?", color = TextPrimary) },
        text = {
          Text(
            "Clear all solved words on this board and restart the timer?",
            color = TextSecondary,
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showResetConfirm = false
              onResetClick()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
          ) {
            Text("Reset", color = TextPrimary)
          }
        },
        dismissButton = {
          TextButton(onClick = { showResetConfirm = false }) {
            Text("Cancel", color = TextSecondary)
          }
        },
      )
    }

    // How To Play Dialog
    if (showHelpDialog) {
      AlertDialog(
        onDismissRequest = { showHelpDialog = false },
        containerColor = SlateSurface,
        title = {
          Text(
            stringResource(R.string.game_how_to_play),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpRuleRow("1", "Drag orthogonally (up, down, left, right) across letters to spell hidden words. No diagonals!")
            HelpRuleRow("2", "Every single tile on the grid belongs to a word. Fill 100% of the board to win.")
            HelpRuleRow("3", "Any valid extra 3+ letter English word you find goes into your Extra Words Bucket — collect 5 to earn +1 Free Hint!")
            HelpRuleRow("4", "Timed Blitz boards reward extra stars and +100 speed bonus if you beat the countdown.")
          }
        },
        confirmButton = {
          Button(
            onClick = { showHelpDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
          ) {
            Text("Got it", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    // Level Complete Celebration Dialog
    if (state.isCompleted) {
      AlertDialog(
        onDismissRequest = {},
        containerColor = SlateSurface,
        title = {
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.size(180.dp, 64.dp)) {
              val sparkles = listOf(
                Offset(size.width * 0.12f, size.height * 0.25f) to AccentGold,
                Offset(size.width * 0.88f, size.height * 0.22f) to AccentEmerald,
                Offset(size.width * 0.20f, size.height * 0.80f) to AccentSky,
                Offset(size.width * 0.80f, size.height * 0.78f) to AccentCoral,
                Offset(size.width * 0.05f, size.height * 0.55f) to WordTilePalette[4],
                Offset(size.width * 0.95f, size.height * 0.52f) to AccentGold,
              )
              sparkles.forEach { (pt, color) ->
                drawCircle(color = color.copy(alpha = 0.85f), radius = 6f, center = pt)
              }
            }
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..3) {
                  Icon(
                    imageVector = if (i <= state.earnedStars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (i <= state.earnedStars) AccentGold else TextMuted,
                    modifier = Modifier.size(34.dp),
                  )
                }
              }
              Text(
                text = state.victoryHeadline,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
              )
            }
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "${state.puzzle.title} solved in ${formatSeconds(state.elapsedSeconds)}",
              style = MaterialTheme.typography.bodyLarge,
              color = TextPrimary,
              textAlign = TextAlign.Center,
            )
            Text(
              text = state.victorySubmessage,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              textAlign = TextAlign.Center,
            )
            Surface(
              color = SlateSurfaceElevated,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  Text(
                    "+${state.scoreEarned}",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentEmerald,
                  )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("BONUS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  Text(
                    if (state.isDailyChallenge) "+2 Hints" else "+1 Hint",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentGold,
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = onNextLevelClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("victory_next_button"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
          ) {
            Text(
              text = when {
                state.isCurated -> "Next Level"
                state.isDailyChallenge -> "Back to Home"
                else -> "New Endless Board"
              },
              fontFamily = OutfitFontFamily,
              fontWeight = FontWeight.Bold,
            )
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = onBrowseLevelsClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(50),
          ) {
            Text("All Levels", color = TextPrimary)
          }
        },
      )
    }
  }
}

@Composable
private fun GameActionButton(
  icon: ImageVector,
  label: String,
  badge: String?,
  enabled: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (enabled) SlateSurface else SlateSurface.copy(alpha = 0.5f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(14.dp))
      .border(
        width = 1.dp,
        color = if (enabled) UnsolvedTileStroke.copy(alpha = 0.45f) else UnsolvedTileStroke.copy(alpha = 0.15f),
        shape = RoundedCornerShape(14.dp),
      )
      .clickable(enabled = enabled, onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) accentColor else TextMuted,
          modifier = Modifier.size(18.dp),
        )
        if (badge != null) {
          Text(
            text = badge,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (enabled) AccentGold else TextMuted,
          )
        }
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (enabled) TextPrimary else TextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HelpRuleRow(number: String, text: String) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(AccentSky.copy(alpha = 0.22f)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = number,
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = AccentSky,
      )
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary,
      modifier = Modifier.weight(1f),
    )
  }
}
