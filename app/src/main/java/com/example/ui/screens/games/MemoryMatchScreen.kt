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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.puzzle.BonusDictionary
import com.example.data.repository.MiniGameScoresStore
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val MEMORY_PAIRS = 8
private const val MEMORY_COLUMNS = 4

private val memoryWords: List<String> =
  BonusDictionary.words.filter { it.length in 3..4 }

private data class MemoryTile(
  val id: Int,
  val word: String,
  val pairId: Int,
)

private fun newDeck(): List<MemoryTile> {
  val words = memoryWords.shuffled().take(MEMORY_PAIRS)
  val tiles = mutableListOf<MemoryTile>()
  var id = 0
  words.forEachIndexed { pairId, word ->
    repeat(2) {
      tiles.add(MemoryTile(id = id++, word = word, pairId = pairId))
    }
  }
  return tiles.shuffled()
}

@Composable
fun MemoryMatchScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

  var tiles by remember { mutableStateOf(newDeck()) }
  var faceUp by remember { mutableStateOf(setOf<Int>()) }
  var matchedPairIds by remember { mutableStateOf(setOf<Int>()) }
  var moves by remember { mutableIntStateOf(0) }
  var locked by remember { mutableStateOf(false) }
  var finished by remember { mutableStateOf(false) }
  var newRecord by remember { mutableStateOf(false) }
  var best by remember { mutableIntStateOf(MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_MEMORY_BEST)) }

  // Flip mismatched tiles back after a short pause
  LaunchedEffect(locked, moves) {
    if (locked) {
      delay(750)
      faceUp = emptySet()
      locked = false
    }
  }

  // Finish: save best (fewest moves wins)
  LaunchedEffect(finished) {
    if (finished) {
      newRecord = MiniGameScoresStore.saveBest(
        context,
        MiniGameScoresStore.KEY_MEMORY_BEST,
        moves,
        lowerIsBetter = true,
      )
      best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_MEMORY_BEST)
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
          text = "Memory Match",
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ScorePill(label = "Moves", value = "$moves", tint = AccentSky, modifier = Modifier.weight(1f))
        ScorePill(label = "Pairs", value = "${matchedPairIds.size}/$MEMORY_PAIRS", tint = AccentEmerald, modifier = Modifier.weight(1f))
        ScorePill(label = "Best", value = if (best > 0) "$best" else "—", tint = AccentGold, modifier = Modifier.weight(1f))
      }

      Spacer(Modifier.height(20.dp))

      // 4x4 tile grid
      tiles.chunked(MEMORY_COLUMNS).forEach { rowTiles ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rowTiles.forEach { tile ->
            val isFaceUp = tile.id in faceUp
            val isMatched = tile.pairId in matchedPairIds
            val revealed = isFaceUp || isMatched
            val accent = WordTilePalette[tile.pairId % WordTilePalette.size]
            Box(
              modifier = Modifier
                .weight(1f)
                .aspectRatio(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                  when {
                    isMatched -> accent.copy(alpha = 0.22f)
                    revealed -> PillWhite
                    else -> SlateSurfaceElevated
                  }
                )
                .border(
                  width = if (isMatched) 2.dp else 1.dp,
                  color = when {
                    isMatched -> AccentEmerald
                    revealed -> Color.Transparent
                    else -> UnsolvedTileStroke.copy(alpha = 0.55f)
                  },
                  shape = RoundedCornerShape(14.dp),
                )
                .clickable {
                  if (locked || finished || revealed) return@clickable
                  val nowUp = faceUp + tile.id
                  faceUp = nowUp
                  if (nowUp.size == 2) {
                    moves += 1
                    val first = tiles.first { it.id == nowUp.first() }
                    val second = tiles.first { it.id == nowUp.last() }
                    if (first.pairId == second.pairId) {
                      matchedPairIds = matchedPairIds + first.pairId
                      faceUp = emptySet()
                      if (matchedPairIds.size == MEMORY_PAIRS) finished = true
                    } else {
                      locked = true
                    }
                  }
                }
                .testTag("memory_tile_${tile.id}"),
              contentAlignment = Alignment.Center,
            ) {
              if (revealed) {
                Text(
                  text = tile.word,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                  ),
                  textAlign = TextAlign.Center,
                  color = if (isMatched) AccentEmerald else PillNavyInk,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.HelpOutline,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }
        }
        Spacer(Modifier.height(10.dp))
      }

      Spacer(Modifier.weight(1f))

      Text(
        text = "Tap two tiles to flip them — find all $MEMORY_PAIRS matching pairs",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(14.dp))
    }

    if (finished) {
      GameOverOverlay(
        title = "All Pairs Found!",
        line1 = "Solved in $moves move${if (moves == 1) "" else "s"}",
        line2 = if (newRecord) "New personal best!" else "Best: ${if (best > 0) "$best moves" else "—"}",
        bestTint = if (newRecord) AccentEmerald else AccentGold,
        onPlayAgain = {
          tiles = newDeck()
          faceUp = emptySet()
          matchedPairIds = emptySet()
          moves = 0
          locked = false
          newRecord = false
          finished = false
        },
        onBack = onBack,
        tag = "memory_game_over",
      )
    }
  }
}
