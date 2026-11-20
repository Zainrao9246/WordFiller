package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LevelProgressEntity
import com.example.data.model.DifficultyPack
import com.example.data.model.FillwordPuzzle
import com.example.data.model.LevelType
import com.example.data.puzzle.CuratedPuzzles
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette

@Composable
fun LevelSelectScreen(
  initialPack: DifficultyPack?,
  progressMap: Map<String, LevelProgressEntity>,
  onSelectPuzzle: (FillwordPuzzle) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  var selectedPack by remember(initialPack) { mutableStateOf(initialPack) }

  val filteredPuzzles = remember(selectedPack) {
    if (selectedPack == null) {
      CuratedPuzzles.allPuzzles
    } else {
      CuratedPuzzles.allPuzzles.filter { it.pack == selectedPack }
    }
  }

  val totalStars = CuratedPuzzles.allPuzzles.sumOf { progressMap[it.id]?.stars ?: 0 }

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
      // Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("levels_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = TextPrimary,
            )
          }
          Text(
            text = stringResource(R.string.levels_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
          )
        }

        Surface(
          color = SlateSurfaceElevated,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
            Text(
              text = "$totalStars / ${CuratedPuzzles.allPuzzles.size * 3}",
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
            )
          }
        }
      }

      // Pack filter chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          FilterChip(
            selected = selectedPack == null,
            onClick = { selectedPack = null },
            label = { Text("All (${CuratedPuzzles.allPuzzles.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentSky.copy(alpha = 0.25f),
              selectedLabelColor = TextPrimary,
            ),
          )
        }
        items(DifficultyPack.entries) { pack ->
          val count = CuratedPuzzles.allPuzzles.count { it.pack == pack }
          FilterChip(
            selected = selectedPack == pack,
            onClick = { selectedPack = pack },
            label = { Text("${pack.displayName} ($count)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentSky.copy(alpha = 0.25f),
              selectedLabelColor = TextPrimary,
            ),
          )
        }
      }

      // Levels Grid
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(filteredPuzzles, key = { it.id }) { puzzle ->
          val progress = progressMap[puzzle.id]
          LevelCard(
            puzzle = puzzle,
            progress = progress,
            onClick = { onSelectPuzzle(puzzle) },
          )
        }
      }
    }
  }
}

@Composable
private fun LevelCard(
  puzzle: FillwordPuzzle,
  progress: LevelProgressEntity?,
  onClick: () -> Unit,
) {
  val isCompleted = progress?.isCompleted == true
  val inProgress = !isCompleted && (!progress?.foundWordsCsv.isNullOrBlank() || !progress?.bonusWordsCsv.isNullOrBlank())
  val stars = progress?.stars ?: 0
  val accent = WordTilePalette[(puzzle.levelNumber - 1).coerceAtLeast(0) % WordTilePalette.size]

  val typeAccent = when (puzzle.levelType) {
    LevelType.BLITZ -> AccentCoral
    LevelType.MEGA -> WordTilePalette[4]
    LevelType.SIGNATURE -> AccentGold
    LevelType.CLASSIC -> accent
  }

  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(
        width = if (puzzle.levelType != LevelType.CLASSIC) 1.4.dp else 1.dp,
        color = if (puzzle.levelType != LevelType.CLASSIC) typeAccent.copy(alpha = 0.6f) else UnsolvedTileStroke.copy(alpha = 0.4f),
        shape = RoundedCornerShape(18.dp),
      )
      .clickable(onClick = onClick)
      .testTag("level_card_${puzzle.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(accent.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text(
              text = "LVL ${puzzle.levelNumber}",
              fontFamily = OutfitFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = accent,
            )
          }

          if (puzzle.levelType != LevelType.CLASSIC) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(typeAccent.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
              Text(
                text = puzzle.levelType.badgeLabel.uppercase(),
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = typeAccent,
              )
            }
          }
        }

        when {
          isCompleted -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Completed",
            tint = AccentEmerald,
            modifier = Modifier.size(18.dp),
          )
          puzzle.isSignatureBoard -> Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = "Signature board",
            tint = AccentGold,
            modifier = Modifier.size(18.dp),
          )
          puzzle.levelType == LevelType.BLITZ -> Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = "Timed Blitz",
            tint = AccentCoral,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = puzzle.title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${puzzle.cols}×${puzzle.rows} • ${puzzle.placements.size} words",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }

      Spacer(Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          for (i in 1..3) {
            Icon(
              imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
              contentDescription = null,
              tint = if (i <= stars) AccentGold else TextMuted,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        val statusText = when {
          isCompleted && (progress?.bestTimeSeconds ?: 0) > 0 -> formatSeconds(progress!!.bestTimeSeconds)
          inProgress -> "Resume"
          else -> "Play"
        }
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelMedium,
          color = if (inProgress) AccentSky else TextSecondary,
        )
      }
    }
  }
}

internal fun formatSeconds(totalSeconds: Int): String {
  val mins = totalSeconds / 60
  val secs = totalSeconds % 60
  return "%02d:%02d".format(mins, secs)
}
