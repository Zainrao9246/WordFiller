package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FillwordPuzzle
import com.example.data.model.GridPos
import com.example.data.model.WordPlacement
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BoardContainerNavy
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.UnsolvedTileBevel
import com.example.ui.theme.UnsolvedTileBg
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTileBevelPalette
import com.example.ui.theme.WordTilePalette
import com.example.ui.viewmodel.TraceStepOutcome
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FillwordBoard(
  puzzle: FillwordPuzzle,
  charGrid: Array<CharArray>,
  solvedCellMap: Map<GridPos, WordPlacement>,
  foundPlacements: List<WordPlacement>,
  activeTrace: List<GridPos>,
  activeTraceColorIndex: Int,
  hintedCell: GridPos?,
  shakeTrigger: Int,
  hapticsEnabled: Boolean,
  onTraceStart: (GridPos) -> Boolean,
  onTraceStep: (GridPos) -> TraceStepOutcome,
  onTraceEnd: () -> Boolean,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val shakeOffset = remember { Animatable(0f) }
  val solvePop = remember { Animatable(1f) }
  val solvedWordCount = foundPlacements.size

  LaunchedEffect(solvedWordCount) {
    if (solvedWordCount > 0) {
      solvePop.snapTo(1.06f)
      solvePop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
    }
  }

  LaunchedEffect(shakeTrigger) {
    if (shakeTrigger > 0) {
      if (hapticsEnabled) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      }
      val offsets = listOf(-12f, 12f, -8f, 8f, -4f, 0f)
      for (target in offsets) {
        shakeOffset.animateTo(target, tween(durationMillis = 36))
      }
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
  val hintPulse by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "hintAlpha",
  )

  BoxWithConstraints(
    modifier = modifier
      .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
      .testTag("fillword_board"),
    contentAlignment = Alignment.Center,
  ) {
    val density = LocalDensity.current
    val outerPaddingDp = 10.dp
    val gapDp: Dp = if (puzzle.cols >= 8 || puzzle.rows >= 9) 4.dp else 6.dp

    val availWidthDp = (maxWidth - outerPaddingDp * 2).coerceAtLeast(120.dp)
    val availHeightDp = (maxHeight - outerPaddingDp * 2).coerceAtLeast(120.dp)

    val maxTileW = (availWidthDp - gapDp * (puzzle.cols - 1)) / puzzle.cols
    val maxTileH = (availHeightDp - gapDp * (puzzle.rows - 1)) / puzzle.rows
    val tileSizeDp = min(maxTileW.value, maxTileH.value).dp.coerceIn(22.dp, 68.dp)

    val boardWidthDp = tileSizeDp * puzzle.cols + gapDp * (puzzle.cols - 1)
    val boardHeightDp = tileSizeDp * puzzle.rows + gapDp * (puzzle.rows - 1)

    val tileSizePx = with(density) { tileSizeDp.toPx() }
    val gapPx = with(density) { gapDp.toPx() }
    val stepPx = tileSizePx + gapPx
    val cornerRadiusDp = (tileSizeDp * 0.22f).coerceIn(5.dp, 14.dp)
    val letterFontSize = (tileSizeDp.value * 0.50f).coerceIn(13f, 30f).sp

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(18.dp))
        .background(BoardContainerNavy)
        .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        .padding(outerPaddingDp),
    ) {
      Box(
        modifier = Modifier
          .size(width = boardWidthDp, height = boardHeightDp)
          .pointerInput(puzzle.id, tileSizePx, gapPx, hapticsEnabled) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val startPos = pointToGridPos(
                offset = down.position,
                rows = puzzle.rows,
                cols = puzzle.cols,
                tileSizePx = tileSizePx,
                stepPx = stepPx,
                hitTolerance = 1.0f,
              )
              if (startPos != null) {
                if (onTraceStart(startPos) && hapticsEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
              }
              down.consume()

              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                  val matched = onTraceEnd()
                  if (matched && hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                  change.consume()
                  break
                }
                val stepPos = pointToGridPos(
                  offset = change.position,
                  rows = puzzle.rows,
                  cols = puzzle.cols,
                  tileSizePx = tileSizePx,
                  stepPx = stepPx,
                  hitTolerance = 0.86f,
                )
                if (stepPos != null) {
                  val outcome = onTraceStep(stepPos)
                  if (outcome != TraceStepOutcome.IGNORED && hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  }
                }
                change.consume()
              }
            }
          },
      ) {
        // 1. Connector bridges & unified word-snake ribbons drawn cleanly behind letters
        Canvas(modifier = Modifier.fillMaxSize()) {
          val bridgeWidth = tileSizePx * 0.44f
          val cornerPx = cornerRadiusDp.toPx()

          // Draw solved word bridges
          for (placement in foundPlacements) {
            val fillColor = WordTilePalette[placement.colorIndex % WordTilePalette.size]
            val bevelColor = WordTileBevelPalette[placement.colorIndex % WordTileBevelPalette.size]
            drawPathConnectors(
              path = placement.path,
              tileSizePx = tileSizePx,
              stepPx = stepPx,
              bridgeWidth = bridgeWidth,
              fillColor = fillColor,
              bevelColor = bevelColor,
              cornerPx = cornerPx,
            )
          }

          // Draw active drag trace bridges
          if (activeTrace.size >= 2) {
            val traceColor = WordTilePalette[activeTraceColorIndex % WordTilePalette.size]
            val traceBevel = WordTileBevelPalette[activeTraceColorIndex % WordTileBevelPalette.size]
            drawPathConnectors(
              path = activeTrace,
              tileSizePx = tileSizePx,
              stepPx = stepPx,
              bridgeWidth = bridgeWidth,
              fillColor = traceColor,
              bevelColor = traceBevel,
              cornerPx = cornerPx,
            )
          }

          // Draw tiles
          val bevelOffset = (tileSizePx * 0.08f).coerceAtLeast(2f)
          val lastSolvedWord = foundPlacements.lastOrNull()?.word
          for (r in 0 until puzzle.rows) {
            for (c in 0 until puzzle.cols) {
              val pos = GridPos(r, c)
              val left = c * stepPx
              val top = r * stepPx

              val solvedPlacement = solvedCellMap[pos]
              val isTraced = pos in activeTrace
              val isTraceTip = activeTrace.lastOrNull() == pos
              val isHinted = hintedCell == pos
              val isLatestSolved = solvedPlacement != null && solvedPlacement.word == lastSolvedWord

              val (faceColor, bevelColor) = when {
                solvedPlacement != null -> {
                  val idx = solvedPlacement.colorIndex % WordTilePalette.size
                  WordTilePalette[idx] to WordTileBevelPalette[idx]
                }
                isTraced -> {
                  val idx = activeTraceColorIndex % WordTilePalette.size
                  WordTilePalette[idx] to WordTileBevelPalette[idx]
                }
                else -> UnsolvedTileBg to UnsolvedTileBevel
              }

              val popScale = when {
                isTraceTip -> 0.045f
                isLatestSolved -> (solvePop.value - 1f)
                else -> 0f
              }
              val scaleBoost = tileSizePx * popScale
              val tileLeft = left - scaleBoost
              val tileTop = top - scaleBoost
              val tileW = tileSizePx + scaleBoost * 2
              val tileH = tileSizePx + scaleBoost * 2

              // Bottom tactile bevel
              drawRoundRect(
                color = bevelColor,
                topLeft = Offset(tileLeft, tileTop + bevelOffset),
                size = Size(tileW, tileH),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
              )

              // Main tile face
              drawRoundRect(
                color = faceColor,
                topLeft = Offset(tileLeft, tileTop),
                size = Size(tileW, tileH),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
              )

              // Crisp top edge gloss on solved or actively traced tiles
              if (solvedPlacement != null || isTraced) {
                drawRoundRect(
                  color = Color.White.copy(alpha = 0.16f),
                  topLeft = Offset(tileLeft + 2f, tileTop + 1.5f),
                  size = Size(tileW - 4f, (tileH * 0.36f).coerceAtLeast(4f)),
                  cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
              }

              // Subtle hint glow ring & soft gold aura
              if (isHinted && solvedPlacement == null && !isTraced) {
                drawRoundRect(
                  color = AccentGold.copy(alpha = hintPulse * 0.22f),
                  topLeft = Offset(tileLeft, tileTop),
                  size = Size(tileW, tileH),
                  cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
                drawRoundRect(
                  color = AccentGold.copy(alpha = hintPulse),
                  topLeft = Offset(tileLeft + 1.5f, tileTop + 1.5f),
                  size = Size(tileW - 3f, tileH - 3f),
                  cornerRadius = CornerRadius(cornerPx, cornerPx),
                  style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.5f),
                )
              }
            }
          }
        }

        // 2. Crisp tile letters layered over tiles
        for (r in 0 until puzzle.rows) {
          for (c in 0 until puzzle.cols) {
            val pos = GridPos(r, c)
            val letter = charGrid[r][c]
            val solved = solvedCellMap[pos] != null
            val traced = pos in activeTrace
            val leftDp = (tileSizeDp + gapDp) * c
            val topDp = (tileSizeDp + gapDp) * r

            Box(
              modifier = Modifier
                .offset(x = leftDp, y = topDp)
                .size(tileSizeDp)
                .semantics {
                  contentDescription = "$letter at row ${r + 1} column ${c + 1}"
                },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = letter.toString(),
                fontFamily = OutfitFontFamily,
                fontWeight = if (solved || traced) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = letterFontSize,
                color = TextPrimary,
              )
            }
          }
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathConnectors(
  path: List<GridPos>,
  tileSizePx: Float,
  stepPx: Float,
  bridgeWidth: Float,
  fillColor: Color,
  bevelColor: Color,
  cornerPx: Float,
) {
  if (path.size < 2) return
  val bevelShift = (tileSizePx * 0.08f).coerceAtLeast(2f)
  for (i in 0 until path.lastIndex) {
    val a = path[i]
    val b = path[i + 1]
    val centerA = Offset(a.col * stepPx + tileSizePx / 2f, a.row * stepPx + tileSizePx / 2f)
    val centerB = Offset(b.col * stepPx + tileSizePx / 2f, b.row * stepPx + tileSizePx / 2f)

    drawLine(
      color = bevelColor,
      start = centerA.copy(y = centerA.y + bevelShift),
      end = centerB.copy(y = centerB.y + bevelShift),
      strokeWidth = bridgeWidth,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = fillColor,
      start = centerA,
      end = centerB,
      strokeWidth = bridgeWidth,
      cap = StrokeCap.Round,
    )
  }
}

private fun pointToGridPos(
  offset: Offset,
  rows: Int,
  cols: Int,
  tileSizePx: Float,
  stepPx: Float,
  hitTolerance: Float,
): GridPos? {
  if (offset.x < 0f || offset.y < 0f) return null
  val col = floor(offset.x / stepPx).toInt()
  val row = floor(offset.y / stepPx).toInt()
  if (row !in 0 until rows || col !in 0 until cols) return null

  val cellCenterX = col * stepPx + tileSizePx / 2f
  val cellCenterY = row * stepPx + tileSizePx / 2f
  val halfTile = tileSizePx / 2f

  val nx = abs(offset.x - cellCenterX) / halfTile
  val ny = abs(offset.y - cellCenterY) / halfTile
  return if (nx <= hitTolerance && ny <= hitTolerance) GridPos(row, col) else null
}
