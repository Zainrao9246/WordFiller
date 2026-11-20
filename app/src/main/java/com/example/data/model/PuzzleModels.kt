package com.example.data.model

data class GridPos(val row: Int, val col: Int) {
  fun isOrthogonallyAdjacentTo(other: GridPos): Boolean {
    val dr = kotlin.math.abs(row - other.row)
    val dc = kotlin.math.abs(col - other.col)
    return dr + dc == 1
  }
}

data class WordPlacement(
  val word: String,
  val path: List<GridPos>,
  val colorIndex: Int,
)

enum class DifficultyPack(
  val displayName: String,
  val subtitle: String,
) {
  WARMUP("Warmup", "Levels 1–25 • 4×4 & 5×5"),
  CLASSIC("Classic", "Levels 26–50 • 5×6, 6×6 & 8×11"),
  NATURE("Nature & Cosmos", "Levels 51–75 • 6×7 & 7×7"),
  MASTERMIND("Mastermind", "Levels 76–100 • 7×8, 8×8 & 9×8"),
}

enum class LevelType(
  val displayName: String,
  val badgeLabel: String,
) {
  CLASSIC("Classic", "Classic"),
  BLITZ("Timed Blitz", "Blitz"),
  MEGA("Mega Board", "Mega"),
  SIGNATURE("Signature 8×11", "Signature"),
}

data class FillwordPuzzle(
  val id: String,
  val levelNumber: Int,
  val title: String,
  val subtitle: String,
  val pack: DifficultyPack,
  val rows: Int,
  val cols: Int,
  val placements: List<WordPlacement>,
  val isSignatureBoard: Boolean = false,
  val levelType: LevelType = if (isSignatureBoard) LevelType.SIGNATURE else LevelType.CLASSIC,
  val targetSeconds: Int = 0,
) {
  fun buildCharGrid(): Array<CharArray> {
    val grid = Array(rows) { CharArray(cols) { ' ' } }
    for (placement in placements) {
      placement.path.forEachIndexed { idx, pos ->
        if (idx < placement.word.length && pos.row in 0 until rows && pos.col in 0 until cols) {
          grid[pos.row][pos.col] = placement.word[idx]
        }
      }
    }
    return grid
  }

  fun validate(): Boolean {
    if (rows <= 0 || cols <= 0 || placements.isEmpty()) return false
    val seen = HashSet<GridPos>()
    for (placement in placements) {
      if (placement.word.length != placement.path.size || placement.word.isEmpty()) return false
      for (i in placement.path.indices) {
        val pos = placement.path[i]
        if (pos.row !in 0 until rows || pos.col !in 0 until cols) return false
        if (!seen.add(pos)) return false
        if (i > 0 && !pos.isOrthogonallyAdjacentTo(placement.path[i - 1])) return false
      }
    }
    return seen.size == rows * cols
  }
}
