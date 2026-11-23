package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.MiniGameScoresStore
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MiniGameType

@Composable
fun MiniGamesHubScreen(
  onBack: () -> Unit,
  onSelectGame: (MiniGameType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

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
        .navigationBarsPadding(),
    ) {
      // Top bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("games_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
          )
        }
        Column {
          Text(
            text = "Mini Games",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
          )
          Text(
            text = "Five quick games • Best scores saved",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("games_hub_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          GameCard(
            icon = Icons.Filled.Shuffle,
            iconTint = AccentGold,
            title = "Anagram Rush",
            description = "Unscramble as many words as you can in 60 seconds.",
            bestLabel = bestScoreLine(context, MiniGameScoresStore.KEY_ANAGRAM_BEST, "words"),
            tag = "game_card_anagram",
            onClick = { onSelectGame(MiniGameType.ANAGRAM_RUSH) },
          )
        }
        item {
          GameCard(
            icon = Icons.Filled.Link,
            iconTint = AccentSky,
            title = "Word Chain",
            description = "Each new word must start with the last letter of the previous one.",
            bestLabel = bestScoreLine(context, MiniGameScoresStore.KEY_WORD_CHAIN_BEST, "words"),
            tag = "game_card_word_chain",
            onClick = { onSelectGame(MiniGameType.WORD_CHAIN) },
          )
        }
        item {
          GameCard(
            icon = Icons.Filled.HelpOutline,
            iconTint = AccentCoral,
            title = "Word Guess",
            description = "Guess the hidden word one letter at a time — 6 wrong guesses and it's over.",
            bestLabel = bestScoreLine(context, MiniGameScoresStore.KEY_HANGMAN_BEST, "streak"),
            tag = "game_card_hangman",
            onClick = { onSelectGame(MiniGameType.HANGMAN) },
          )
        }
        item {
          GameCard(
            icon = Icons.Filled.Style,
            iconTint = AccentEmerald,
            title = "Memory Match",
            description = "Flip tiles and pair up matching words in as few moves as possible.",
            bestLabel = bestMovesLine(context),
            tag = "game_card_memory",
            onClick = { onSelectGame(MiniGameType.MEMORY_MATCH) },
          )
        }
        item {
          GameCard(
            icon = Icons.Filled.GridOn,
            iconTint = AccentSky,
            title = "Tic Tac Toe",
            description = "Classic X vs O against a sharp board AI — build the longest win streak.",
            bestLabel = bestStreakLine(context),
            tag = "game_card_tic_tac_toe",
            onClick = { onSelectGame(MiniGameType.TIC_TAC_TOE) },
          )
        }
      }
    }
  }
}

@Composable
private fun GameCard(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  description: String,
  bestLabel: String,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(1.2.dp, iconTint.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag(tag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(iconTint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(24.dp),
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
          ),
          color = TextPrimary,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Text(
          text = bestLabel,
          style = MaterialTheme.typography.labelMedium,
          color = iconTint,
        )
      }
    }
  }
}

@Composable
private fun bestScoreLine(context: android.content.Context, key: String, unit: String): String {
  val best = MiniGameScoresStore.best(context, key)
  return if (best > 0) "Best: $best $unit" else "Best: —"
}

@Composable
private fun bestMovesLine(context: android.content.Context): String {
  val best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_MEMORY_BEST)
  return if (best > 0) "Best: $best moves" else "Best: —"
}

@Composable
private fun bestStreakLine(context: android.content.Context): String {
  val best = MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_TICTACTOE_BEST)
  return if (best > 0) "Best streak: $best" else "Best streak: —"
}
