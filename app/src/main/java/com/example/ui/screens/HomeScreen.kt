package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.FillwordsDatabase
import com.example.data.local.LevelProgressEntity
import com.example.data.local.PlayerStatsEntity
import com.example.data.model.DifficultyPack
import com.example.data.puzzle.CuratedPuzzles
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.BoardContainerNavy
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileBevel
import com.example.ui.theme.UnsolvedTileBg
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette

import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Today

@Composable
fun HomeScreen(
  progressMap: Map<String, LevelProgressEntity>,
  stats: PlayerStatsEntity,
  todayDateKey: String,
  onHintBulbTap: () -> Unit,
  onResetBulbTapSequence: () -> Unit,
  onPlayClick: () -> Unit,
  onPlaySignatureClick: () -> Unit,
  onDailyChallengeClick: () -> Unit,
  onBrowseLevelsClick: (DifficultyPack?) -> Unit,
  onStartEndlessClick: (rows: Int, cols: Int) -> Unit,
  onMiniGamesClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val totalLevels = CuratedPuzzles.allPuzzles.size
  val completedLevels = CuratedPuzzles.allPuzzles.count { progressMap[it.id]?.isCompleted == true }
  val totalStars = CuratedPuzzles.allPuzzles.sumOf { progressMap[it.id]?.stars ?: 0 }
  val hasInProgress = CuratedPuzzles.allPuzzles.any { puzzle ->
    val p = progressMap[puzzle.id]
    p != null && !p.isCompleted && (p.foundWordsCsv.isNotBlank() || p.bonusWordsCsv.isNotBlank())
  }
  val dailyDoneToday = stats.lastDailyCompletedDate == todayDateKey

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DeepNavyBackground)
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(AccentSky.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.18f),
            radius = size.width * 0.85f,
          ),
        )
      },
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .statusBarsPadding()
        .navigationBarsPadding()
        .testTag("home_lazy_column"),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Top Wordmark + Bucket & Hints Pills
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "FILLWORDS",
              style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
              ),
              color = TextPrimary,
            )
            Text(
              text = "$totalLevels Levels • Extra Words Bucket",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = SlateSurfaceElevated,
              shape = RoundedCornerShape(50),
              modifier = Modifier.border(1.dp, AccentEmerald.copy(alpha = 0.45f), RoundedCornerShape(50)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Inventory2,
                  contentDescription = "Extra words bucket progress",
                  tint = AccentEmerald,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "${stats.extraWordsBucketCount}/${FillwordsDatabase.BUCKET_TARGET_WORDS}",
                  style = MaterialTheme.typography.labelLarge,
                  color = TextPrimary,
                )
              }
            }

            Box(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onHintBulbTap)
                .testTag("home_hint_bulb_pill"),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                color = SlateSurfaceElevated,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, AccentGold.copy(alpha = 0.45f), RoundedCornerShape(50)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = "Hints available",
                    tint = AccentGold,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "${stats.hintsRemaining}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                  )
                }
              }
            }
          }
        }
      }

      // Hero Fillwords Showcase Card
      item {
        Surface(
          color = BoardContainerNavy,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, UnsolvedTileStroke.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            MiniFillwordsPreviewGrid()

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Button(
                onClick = {
                  onResetBulbTapSequence()
                  onPlayClick()
                },
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("home_play_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                  containerColor = PillWhite,
                  contentColor = PillNavyInk,
                ),
              ) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = if (hasInProgress) stringResource(R.string.home_continue) else stringResource(R.string.home_play),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                  ),
                )
              }

              OutlinedButton(
                onClick = {
                  onResetBulbTapSequence()
                  onPlaySignatureClick()
                },
                modifier = Modifier
                  .height(52.dp)
                  .testTag("home_signature_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AccentGold.copy(alpha = 0.7f)),
              ) {
                Icon(
                  imageVector = Icons.Filled.WorkspacePremium,
                  contentDescription = null,
                  tint = AccentGold,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.home_signature_board),
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }

      // Quick Stats Strip
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StatPillCard(
            icon = Icons.Filled.GridView,
            iconTint = AccentSky,
            label = "Levels",
            value = "$completedLevels/$totalLevels",
            modifier = Modifier.weight(1f),
          )
          StatPillCard(
            icon = Icons.Filled.Star,
            iconTint = AccentGold,
            label = "Stars",
            value = "$totalStars/${totalLevels * 3}",
            modifier = Modifier.weight(1f),
          )
          StatPillCard(
            icon = Icons.Filled.Bolt,
            iconTint = AccentEmerald,
            label = "Score",
            value = "${stats.totalScore}",
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Daily Challenge Card (Game Enhancement)
      item {
        Surface(
          color = SlateSurface,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
              width = 1.2.dp,
              color = if (dailyDoneToday) AccentEmerald.copy(alpha = 0.6f) else AccentGold.copy(alpha = 0.55f),
              shape = RoundedCornerShape(20.dp),
            )
            .clickable {
              onResetBulbTapSequence()
              onDailyChallengeClick()
            }
            .testTag("home_daily_challenge_card"),
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
                .background((if (dailyDoneToday) AccentEmerald else AccentGold).copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Today,
                contentDescription = null,
                tint = if (dailyDoneToday) AccentEmerald else AccentGold,
                modifier = Modifier.size(24.dp),
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (dailyDoneToday) "Daily Challenge Solved!" else "Daily 7×7 Challenge",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = if (dailyDoneToday) {
                  "Play again anytime • Streak ${stats.dailyStreak}d"
                } else {
                  "Fresh daily board • Reward +2 Hints"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }

            if (stats.dailyStreak > 0) {
              Surface(
                color = AccentCoral.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = AccentCoral,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "${stats.dailyStreak}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentCoral,
                  )
                }
              }
            }
          }
        }
      }

      // Mini Games Card
      item {
        Surface(
          color = SlateSurface,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.2.dp, AccentCoral.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable {
              onResetBulbTapSequence()
              onMiniGamesClick()
            }
            .testTag("home_mini_games_card"),
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
                .background(AccentCoral.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                tint = AccentCoral,
                modifier = Modifier.size(24.dp),
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Mini Games",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = "Anagram Rush • Word Chain • Word Guess • Memory Match",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = AccentCoral,
            )
          }
        }
      }

      // Puzzle Packs Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.home_packs_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .minimumInteractiveComponentSize()
              .clickable {
                onResetBulbTapSequence()
                onBrowseLevelsClick(null)
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("browse_all_levels_button"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "All $totalLevels Levels",
              style = MaterialTheme.typography.labelLarge,
              color = AccentSky,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = stringResource(R.string.cd_all_levels),
              tint = AccentSky,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          DifficultyPack.entries.forEachIndexed { idx, pack ->
            val packPuzzles = CuratedPuzzles.allPuzzles.filter { it.pack == pack }
            val solvedCount = packPuzzles.count { progressMap[it.id]?.isCompleted == true }
            val packStars = packPuzzles.sumOf { progressMap[it.id]?.stars ?: 0 }
            val accent = WordTilePalette[idx % WordTilePalette.size]

            PackRowCard(
              pack = pack,
              solvedCount = solvedCount,
              totalCount = packPuzzles.size,
              stars = packStars,
              maxStars = packPuzzles.size * 3,
              accentColor = accent,
              onClick = {
                onResetBulbTapSequence()
                onBrowseLevelsClick(pack)
              },
            )
          }
        }
      }

      // Endless Procedural Mode
      item {
        Surface(
          color = SlateSurface,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = AccentCoral,
                modifier = Modifier.size(20.dp),
              )
              Column {
                Text(
                  text = stringResource(R.string.home_endless_title),
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = "Generate a fresh word-snake grid anytime",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(
                5 to 5,
                6 to 6,
                7 to 7,
                10 to 8,
              ).forEach { (r, c) ->
                EndlessSizeChip(
                  label = "${c}×${r}",
                  onClick = {
                    onResetBulbTapSequence()
                    onStartEndlessClick(r, c)
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("endless_${c}x${r}_button"),
                )
              }
            }
          }
        }
      }

      item { Spacer(Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun MiniFillwordsPreviewGrid() {
  val rows = listOf(
    listOf('Q', 'U', 'E', 'S', 'T', 'I', 'O', 'N') to listOf(null, null, null, null, null, null, null, null),
    listOf('F', 'I', 'L', 'L', 'W', 'O', 'R', 'D') to List(8) { AccentGold },
    listOf('R', 'A', 'W', 'O', 'R', 'D', 'S', 'E') to listOf(null, null, AccentCoral, AccentCoral, AccentCoral, AccentCoral, null, null),
    listOf('L', 'E', 'T', 'T', 'E', 'R', 'P', 'U') to listOf(null, null, null, null, null, null, null, null),
    listOf('S', 'E', 'A', 'R', 'C', 'H', 'Z', 'Z') to listOf(AccentEmerald, AccentEmerald, AccentEmerald, AccentEmerald, AccentEmerald, AccentEmerald, null, null),
  )

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    rows.forEach { (letters, colors) ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        letters.forEachIndexed { idx, ch ->
          val highlight = colors[idx]
          Box(
            modifier = Modifier
              .weight(1f)
              .height(34.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(highlight ?: UnsolvedTileBg)
              .border(
                width = 1.dp,
                color = if (highlight != null) Color.White.copy(alpha = 0.25f) else UnsolvedTileBevel,
                shape = RoundedCornerShape(7.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = ch.toString(),
              fontFamily = OutfitFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextPrimary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatPillCard(
  icon: ImageVector,
  iconTint: Color,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.border(1.dp, UnsolvedTileStroke.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = OutfitFontFamily),
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PackRowCard(
  pack: DifficultyPack,
  solvedCount: Int,
  totalCount: Int,
  stars: Int,
  maxStars: Int,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("pack_${pack.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accentColor.copy(alpha = 0.18f))
          .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "$solvedCount/$totalCount",
          fontFamily = OutfitFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = accentColor,
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = pack.displayName,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Text(
          text = pack.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Star,
          contentDescription = null,
          tint = AccentGold,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "$stars/$maxStars",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun EndlessSizeChip(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = SlateSurfaceElevated,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
  ) {
    Box(
      modifier = Modifier.padding(vertical = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
      )
    }
  }
}
