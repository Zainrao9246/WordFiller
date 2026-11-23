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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WordTilePalette
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

private val chainStartWords: List<String> =
  BonusDictionary.words.filter { it.length in 3..4 }

private fun newChainStartWord(): String =
  chainStartWords[Random.nextInt(chainStartWords.size)]

private const val CHAIN_ROUND_SECONDS = 90
private const val CHAIN_BONUS_SECONDS = 8

@Composable
fun WordChainScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

  var chain by remember { mutableStateOf(listOf(newChainStartWord())) }
  var input by remember { mutableStateOf("") }
  var timeLeft by remember { mutableIntStateOf(CHAIN_ROUND_SECONDS) }
  var gameOver by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var lastWordAccepted by remember { mutableStateOf(true) }
  var newRecord by remember { mutableStateOf(false) }
  var best by remember { mutableIntStateOf(MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_WORD_CHAIN_BEST)) }
  var session by remember { mutableIntStateOf(0) }

  val score = chain.size - 1
  val lastLetter = chain.last().last()

  LaunchedEffect(session) {
    while (timeLeft > 0) {
      delay(1_000)
      timeLeft -= 1
    }
    if (score > 0) {
      newRecord = MiniGameScoresStore.saveBest(context, MiniGameScoresStore.KEY_WORD_CHAIN_BEST, score)
      best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_WORD_CHAIN_BEST)
    }
    gameOver = true
  }

  fun submitWord() {
    val word = input.trim().uppercase(Locale.ROOT)
    when {
      word.isEmpty() -> error = "Type a word first"
      word.length < 3 -> error = "Minimum 3 letters"
      !word.all { it in 'A'..'Z' } -> error = "Letters only"
      word.first() != lastLetter -> error = "Must start with '$lastLetter'"
      word in chain -> error = "Already used"
      word !in BonusDictionary.words -> error = "Not in the dictionary"
      else -> {
        chain = chain + word
        input = ""
        error = null
        lastWordAccepted = true
        timeLeft += CHAIN_BONUS_SECONDS
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
          text = "Word Chain",
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ScorePill(label = "Time", value = "$timeLeft", tint = AccentCoral, modifier = Modifier.weight(1f))
        ScorePill(label = "Chain", value = "$score", tint = AccentEmerald, modifier = Modifier.weight(1f))
        ScorePill(label = "Best", value = if (best > 0) "$best" else "—", tint = AccentGold, modifier = Modifier.weight(1f))
      }

      Spacer(Modifier.height(20.dp))

      // Input row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = input,
          onValueChange = {
            input = it.uppercase(Locale.ROOT).filter { ch -> ch in 'A'..'Z' }.take(12)
            error = null
          },
          modifier = Modifier
            .weight(1f)
            .testTag("word_chain_input"),
          singleLine = true,
          enabled = !gameOver,
          textStyle = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
            color = TextPrimary,
          ),
          label = { Text("Starts with '$lastLetter'") },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          isError = error != null,
        )
        Button(
          onClick = { submitWord() },
          enabled = !gameOver,
          modifier = Modifier.height(56.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PillWhite,
            contentColor = PillNavyInk,
          ),
        ) {
          Text("Add", style = MaterialTheme.typography.titleMedium)
        }
      }

      if (error != null) {
        Text(
          text = error!!,
          style = MaterialTheme.typography.labelMedium,
          color = AccentCoral,
          modifier = Modifier.padding(top = 6.dp),
        )
      } else if (lastWordAccepted && score > 0) {
        Text(
          text = "+$CHAIN_BONUS_SECONDS seconds added!",
          style = MaterialTheme.typography.labelMedium,
          color = AccentEmerald,
          modifier = Modifier.padding(top = 6.dp),
        )
      }

      Spacer(Modifier.height(16.dp))

      // Chain history
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        chain.forEachIndexed { index, word ->
          val isLatest = index == chain.lastIndex
          Surface(
            color = if (isLatest) WordTilePalette[index % WordTilePalette.size] else SlateSurface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isLatest) PillNavyInk.copy(alpha = 0.7f) else TextSecondary,
              )
              Spacer(Modifier.size(12.dp))
              Text(
                text = word,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = OutfitFontFamily,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.5.sp,
                ),
                color = if (isLatest) PillNavyInk else TextPrimary,
              )
            }
          }
        }
      }
    }

    if (gameOver) {
      GameOverOverlay(
        title = "Time's Up!",
        line1 = "Chain of $score word${if (score == 1) "" else "s"}",
        line2 = if (newRecord) "New personal best!" else "Best: ${if (best > 0) "$best" else "—"}",
        bestTint = if (newRecord) AccentEmerald else AccentGold,
        onPlayAgain = {
          chain = listOf(newChainStartWord())
          input = ""
          error = null
          timeLeft = CHAIN_ROUND_SECONDS
          newRecord = false
          lastWordAccepted = true
          session += 1
          gameOver = false
        },
        onBack = onBack,
        tag = "word_chain_game_over",
      )
    }
  }
}
