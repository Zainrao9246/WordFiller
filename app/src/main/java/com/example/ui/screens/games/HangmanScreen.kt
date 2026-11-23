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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.puzzle.BonusDictionary
import com.example.data.repository.MiniGameScoresStore
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BoardContainerNavy
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import kotlin.random.Random

private const val MAX_WRONG = 6

private val hangmanWords: List<String> =
  BonusDictionary.words.filter { it.length in 5..8 }

private fun newHangmanWord(): String =
  hangmanWords[Random.nextInt(hangmanWords.size)]

@Composable
fun HangmanScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

  var word by remember { mutableStateOf(newHangmanWord()) }
  var guessed by remember { mutableStateOf(setOf<Char>()) }
  var streak by remember { mutableIntStateOf(0) }
  var best by remember { mutableIntStateOf(MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_HANGMAN_BEST)) }
  var justWon by remember { mutableStateOf(false) }

  val wrongLetters = guessed.filter { it !in word }.sorted()
  val wrongCount = wrongLetters.size
  val lost = wrongCount >= MAX_WRONG
  val won = word.all { it in guessed } && !lost

  // Award the streak when the word is solved
  androidx.compose.runtime.LaunchedEffect(won) {
    if (won && !justWon) {
      justWon = true
      streak += 1
      MiniGameScoresStore.saveBest(context, MiniGameScoresStore.KEY_HANGMAN_BEST, streak)
      best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_HANGMAN_BEST)
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
          text = "Word Guess",
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        Surface(
          color = SlateSurface,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
            Text(
              text = "$streak (best $best)",
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
            )
          }
        }
      }

      // Lives
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(MAX_WRONG) { index ->
          val alive = index < MAX_WRONG - wrongCount
          Icon(
            imageVector = if (alive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = if (alive) AccentCoral else TextMuted,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Spacer(Modifier.height(22.dp))

      // Hidden word display
      Surface(
        color = BoardContainerNavy,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, UnsolvedTileStroke.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp, horizontal = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            word.forEach { letter ->
              val revealed = lost || letter in guessed
              Box(
                modifier = Modifier
                  .size(width = 30.dp, height = 46.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(SlateSurfaceElevated),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = if (revealed) letter.toString() else "",
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                  ),
                  color = when {
                    won -> AccentEmerald
                    lost -> AccentCoral
                    else -> TextPrimary
                  },
                )
              }
            }
          }
          Text(
            text = when {
              won -> "Solved! Streak: $streak"
              lost -> "It was $word — better luck next time"
              else -> "${word.length} letters • ${MAX_WRONG - wrongCount} lives left"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when {
              won -> AccentEmerald
              lost -> AccentCoral
              else -> TextSecondary
            },
          )
        }
      }

      // Wrong letters
      if (wrongLetters.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
          text = "Wrong: ${wrongLetters.joinToString(" ")}",
          style = MaterialTheme.typography.bodyMedium,
          color = AccentCoral,
        )
      }

      Spacer(Modifier.height(18.dp))

      if (won || lost) {
        Button(
          onClick = {
            word = newHangmanWord()
            guessed = emptySet()
            justWon = false
            if (lost) streak = 0
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("hangman_next_word"),
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(
            containerColor = PillWhite,
            contentColor = PillNavyInk,
          ),
        ) {
          Text(
            text = if (won) "Next Word" else "Try Again",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      } else {
        // A-Z keyboard
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ('A'..'Z').chunked(7).forEach { rowLetters ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              rowLetters.forEach { letter ->
                val isGuessed = letter in guessed
                HangmanKey(
                  letter = letter,
                  enabled = !isGuessed,
                  revealedInWord = isGuessed && letter in word,
                  onClick = { guessed = guessed + letter },
                )
              }
            }
          }
        }
      }

      Spacer(Modifier.weight(1f))
    }
  }
}

@Composable
private fun HangmanKey(
  letter: Char,
  enabled: Boolean,
  revealedInWord: Boolean,
  onClick: () -> Unit,
) {
  val container = when {
    !enabled && revealedInWord -> AccentEmerald.copy(alpha = 0.25f)
    !enabled -> AccentCoral.copy(alpha = 0.18f)
    else -> SlateSurfaceElevated
  }
  val content = when {
    !enabled && revealedInWord -> AccentEmerald
    !enabled -> AccentCoral
    else -> TextPrimary
  }
  Box(
    modifier = Modifier
      .size(width = 42.dp, height = 46.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(container)
      .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .testTag("hangman_key_$letter"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = letter.toString(),
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = content,
    )
  }
}
