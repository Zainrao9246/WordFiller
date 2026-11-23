package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.puzzle.BonusDictionary
import com.example.data.repository.MiniGameScoresStore
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileBg
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette
import kotlinx.coroutines.delay
import kotlin.random.Random

private val anagramWords: List<String> =
  BonusDictionary.words.filter { it.length in 5..7 }

private fun newTargetWord(): String =
  anagramWords[Random.nextInt(anagramWords.size)]

private fun shuffledLetters(word: String): List<Char> {
  var letters = word.toList()
  var guard = 0
  while (letters == word.toList() && guard < 12) {
    letters = letters.shuffled()
    guard++
  }
  return letters
}

@Composable
fun AnagramRushScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

  var target by remember { mutableStateOf(newTargetWord()) }
  var round by remember { mutableIntStateOf(0) }
  var poolLetters by remember(round) { mutableStateOf(shuffledLetters(target)) }
  var pickedIndices by remember { mutableStateOf(listOf<Int>()) }
  var score by remember { mutableIntStateOf(0) }
  var timeLeft by remember { mutableIntStateOf(60) }
  var gameOver by remember { mutableStateOf(false) }
  var correctFlash by remember { mutableStateOf(false) }
  var wrongFlash by remember { mutableStateOf(false) }
  var newRecord by remember { mutableStateOf(false) }
  var best by remember { mutableIntStateOf(MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_ANAGRAM_BEST)) }
  var session by remember { mutableIntStateOf(0) }

  // 60-second countdown (re-runs for every Play Again session)
  LaunchedEffect(session) {
    while (timeLeft > 0) {
      delay(1_000)
      timeLeft -= 1
    }
    if (score > 0) {
      newRecord = MiniGameScoresStore.saveBest(context, MiniGameScoresStore.KEY_ANAGRAM_BEST, score)
      best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_ANAGRAM_BEST)
    }
    gameOver = true
  }

  // Check the built word once every letter tile is picked
  LaunchedEffect(pickedIndices.size, round) {
    if (pickedIndices.size == target.length) {
      val attempt = pickedIndices.map { poolLetters[it] }.joinToString("")
      if (attempt == target) {
        correctFlash = true
        score += 1
        delay(450)
        correctFlash = false
        target = newTargetWord()
        round += 1
        pickedIndices = emptyList()
      } else {
        wrongFlash = true
        delay(650)
        wrongFlash = false
        pickedIndices = emptyList()
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DeepNavyBackground),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
          )
        }
        Text(
          text = "Anagram Rush",
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ScorePill(label = "Time", value = "$timeLeft", tint = AccentCoral, modifier = Modifier.weight(1f))
        ScorePill(label = "Solved", value = "$score", tint = AccentEmerald, modifier = Modifier.weight(1f))
        ScorePill(label = "Best", value = if (best > 0) "$best" else "—", tint = AccentGold, modifier = Modifier.weight(1f))
      }

      Spacer(Modifier.height(22.dp))

      // Answer slots
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        target.forEachIndexed { index, _ ->
          val pickedChar = pickedIndices.getOrNull(index)?.let { poolLetters[it] }
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (pickedChar != null) WordTilePalette[index % WordTilePalette.size] else UnsolvedTileBg)
              .border(
                width = 1.5.dp,
                color = when {
                  correctFlash -> AccentEmerald
                  wrongFlash -> AccentCoral
                  else -> UnsolvedTileStroke.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(12.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = pickedChar?.toString() ?: "",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = PillNavyInk,
            )
          }
        }
      }

      Spacer(Modifier.height(8.dp))

      Text(
        text = "Tap the letters in the right order",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )

      Spacer(Modifier.height(22.dp))

      // Letter pool tiles (4 per row)
      poolLetters.withIndex().toList().chunked(4).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          rowItems.forEach { (originalIndex, rowLetter) ->
            LetterPoolTile(
              letter = rowLetter,
              used = originalIndex in pickedIndices,
              onClick = {
                if (!gameOver && originalIndex !in pickedIndices && pickedIndices.size < target.length) {
                  pickedIndices = pickedIndices + originalIndex
                }
              },
            )
          }
        }
        Spacer(Modifier.height(10.dp))
      }

      Spacer(Modifier.height(8.dp))

      OutlinedButton(
        onClick = { pickedIndices = emptyList() },
        enabled = pickedIndices.isNotEmpty() && !gameOver,
        shape = RoundedCornerShape(50),
      ) {
        Text("Clear")
      }

      Spacer(Modifier.weight(1f))
    }

    if (gameOver) {
      GameOverOverlay(
        title = "Time's Up!",
        line1 = "Solved $score word${if (score == 1) "" else "s"}",
        line2 = if (newRecord) "New personal best!" else "Best: ${if (best > 0) "$best" else "—"}",
        bestTint = if (newRecord) AccentEmerald else AccentGold,
        onPlayAgain = {
          score = 0
          timeLeft = 60
          newRecord = false
          target = newTargetWord()
          round += 1
          pickedIndices = emptyList()
          session += 1
          gameOver = false
        },
        onBack = onBack,
        tag = "anagram_game_over",
      )
    }
  }
}

@Composable
private fun LetterPoolTile(letter: Char, used: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(64.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(if (used) UnsolvedTileBg.copy(alpha = 0.45f) else SlateSurfaceElevated)
      .border(1.dp, UnsolvedTileStroke.copy(alpha = if (used) 0.2f else 0.6f), RoundedCornerShape(14.dp))
      .clickable(enabled = !used, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = letter.toString(),
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = if (used) TextMuted else TextPrimary,
    )
  }
}

@Composable
internal fun ScorePill(label: String, value: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = OutfitFontFamily,
          fontWeight = FontWeight.Bold,
        ),
        color = tint,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
      )
    }
  }
}

@Composable
internal fun GameOverOverlay(
  title: String,
  line1: String,
  line2: String,
  bestTint: androidx.compose.ui.graphics.Color,
  onPlayAgain: () -> Unit,
  onBack: () -> Unit,
  tag: String,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBackground.copy(alpha = 0.92f)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = SlateSurface,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp)
        .border(1.5.dp, bestTint.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
        .testTag(tag),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
        )
        Text(
          text = line1,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Text(
          text = line2,
          style = MaterialTheme.typography.bodyMedium,
          color = bestTint,
        )
        Spacer(Modifier.height(6.dp))
        Button(
          onClick = onPlayAgain,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(
            containerColor = PillWhite,
            contentColor = PillNavyInk,
          ),
        ) {
          Text("Play Again", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
          onClick = onBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(50),
        ) {
          Text("Back to Mini Games", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}
