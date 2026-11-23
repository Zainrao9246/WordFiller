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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.repository.MiniGameScoresStore
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val CELL_COUNT = 9

private val WIN_LINES = listOf(
  intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
  intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
  intArrayOf(0, 4, 8), intArrayOf(2, 4, 6),
)

private enum class Cell { EMPTY, X, O }
private enum class RoundResult { X_WINS, O_WINS, DRAW, IN_PROGRESS }

@Composable
fun TicTacToeScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  BackHandler(onBack = onBack)

  val board = remember { mutableStateListOf(*Array(CELL_COUNT) { Cell.EMPTY }) }
  var xTurn by remember { mutableStateOf(true) }
  var roundOver by remember { mutableStateOf(false) }
  var result by remember { mutableStateOf(RoundResult.IN_PROGRESS) }
  var winningLine by remember { mutableStateOf<IntArray?>(null) }
  var scoreX by remember { mutableIntStateOf(0) }
  var scoreO by remember { mutableIntStateOf(0) }
  var draws by remember { mutableIntStateOf(0) }
  var bestStreak by remember { mutableIntStateOf(MiniGameScoresStore.best(context, MiniGameScoresStore.KEY_TICTACTOE_BEST)) }
  var streak by remember { mutableIntStateOf(0) }

  fun finishRound(res: RoundResult, line: IntArray?) {
    result = res
    winningLine = line
    roundOver = true
    when (res) {
      RoundResult.X_WINS -> {
        scoreX++
        streak++
        if (MiniGameScoresStore.saveBest(context, MiniGameScoresStore.KEY_TICTACTOE_BEST, streak)) {
          bestStreak = streak
        }
      }
      RoundResult.O_WINS -> {
        scoreO++
        streak = 0
      }
      RoundResult.DRAW -> draws++
      RoundResult.IN_PROGRESS -> Unit
    }
  }

  fun checkResult(b: List<Cell>, lastPlayer: Cell): RoundResult {
    for (line in WIN_LINES) {
      if (b[line[0]] == lastPlayer && b[line[1]] == lastPlayer && b[line[2]] == lastPlayer) {
        return if (lastPlayer == Cell.X) RoundResult.X_WINS else RoundResult.O_WINS
      }
    }
    return if (b.all { it != Cell.EMPTY }) RoundResult.DRAW else RoundResult.IN_PROGRESS
  }

  fun lineFor(b: List<Cell>, player: Cell): IntArray? =
    WIN_LINES.firstOrNull { line -> line.all { b[it] == player } }

  fun aiMove(b: List<Cell>): Int {
    // 1. Win if possible, 2. block the player's winning move
    for (player in listOf(Cell.O, Cell.X)) {
      for (i in 0 until CELL_COUNT) {
        if (b[i] != Cell.EMPTY) continue
        val candidate = b.toMutableList().also { it[i] = player }
        if (lineFor(candidate, player) != null) return i
      }
    }
    // 3. Center, 4. corners, 5. any open side
    if (b[4] == Cell.EMPTY) return 4
    val corners = listOf(0, 2, 6, 8).filter { b[it] == Cell.EMPTY }
    if (corners.isNotEmpty()) return corners.random()
    val sides = listOf(1, 3, 5, 7).filter { b[it] == Cell.EMPTY }
    if (sides.isNotEmpty()) return sides.random()
    return -1
  }

  fun resetRound() {
    for (i in 0 until CELL_COUNT) board[i] = Cell.EMPTY
    xTurn = true
    roundOver = false
    result = RoundResult.IN_PROGRESS
    winningLine = null
  }

  fun onCellTap(index: Int) {
    if (roundOver || board[index] != Cell.EMPTY) return
    board[index] = Cell.X
    var res = checkResult(board.toList(), Cell.X)
    if (res != RoundResult.IN_PROGRESS) {
      finishRound(res, lineFor(board.toList(), Cell.X))
      return
    }
    xTurn = false
    val ai = aiMove(board.toList())
    if (ai >= 0) {
      board[ai] = Cell.O
      res = checkResult(board.toList(), Cell.O)
      if (res != RoundResult.IN_PROGRESS) {
        finishRound(res, lineFor(board.toList(), Cell.O))
        return
      }
    }
    xTurn = true
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
    ) {
      // Top bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 0.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("tictactoe_back_button")) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
          )
        }
        Column {
          Text(
            text = "Tic Tac Toe",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = OutfitFontFamily),
            color = TextPrimary,
          )
          Text(
            text = "You are X • Beat the board AI and grow your streak",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      Spacer(Modifier.height(6.dp))

      // Score strip
      Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.2.dp, AccentGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
          .testTag("tictactoe_score_card"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          ScoreCell(label = "You", value = scoreX.toString(), tint = AccentSky)
          ScoreCell(label = "AI", value = scoreO.toString(), tint = AccentCoral)
          ScoreCell(label = "Draws", value = draws.toString(), tint = TextSecondary)
          ScoreCell(label = "Streak", value = streak.toString(), tint = AccentGold)
          ScoreCell(label = "Best", value = bestStreak.toString(), tint = AccentEmerald)
        }
      }

      Spacer(Modifier.height(14.dp))

      // Status line
      Text(
        text = when {
          roundOver && result == RoundResult.X_WINS -> "You win this round!"
          roundOver && result == RoundResult.O_WINS -> "AI takes the round"
          roundOver && result == RoundResult.DRAW -> "Draw — nobody blinked"
          xTurn -> "Your move — tap a square"
          else -> "AI is thinking…"
        },
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold),
        color = if (roundOver) AccentGold else TextPrimary,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .testTag("tictactoe_status"),
      )

      Spacer(Modifier.height(10.dp))

      // Board
      Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, AccentSky.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
          .padding(14.dp)
          .testTag("tictactoe_board"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              for (col in 0 until 3) {
                val index = row * 3 + col
                val inWinningLine = winningLine?.contains(index) == true
                val tint = when {
                  board[index] == Cell.X -> AccentSky
                  board[index] == Cell.O -> AccentCoral
                  else -> Color.Transparent
                }
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .height(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (inWinningLine) SlateSurfaceElevated else Color.Transparent)
                    .border(
                      1.5.dp,
                      when {
                        inWinningLine -> AccentGold
                        board[index] == Cell.EMPTY -> TextSecondary.copy(alpha = 0.35f)
                        else -> tint.copy(alpha = 0.6f)
                      },
                      RoundedCornerShape(16.dp),
                    )
                    .clickable(enabled = !roundOver && board[index] == Cell.EMPTY) {
                      onCellTap(index)
                    }
                    .testTag("tictactoe_cell_$index"),
                ) {
                  Text(
                    text = when (board[index]) {
                      Cell.X -> "X"
                      Cell.O -> "O"
                      Cell.EMPTY -> ""
                    },
                    style = MaterialTheme.typography.displaySmall.copy(
                      fontFamily = OutfitFontFamily,
                      fontWeight = FontWeight.Black,
                    ),
                    color = tint,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      Button(
        onClick = { resetRound() },
        enabled = roundOver,
        colors = ButtonDefaults.buttonColors(
          containerColor = AccentGold,
          contentColor = Color(0xFF123052),
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("tictactoe_reset_button"),
      ) {
        Text(
          text = "Play Again",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }
}

@Composable
private fun ScoreCell(label: String, value: String, tint: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge.copy(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Black,
      ),
      color = tint,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
    )
  }
}
