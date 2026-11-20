package com.example.data.puzzle

import com.example.data.model.DifficultyPack
import com.example.data.model.FillwordPuzzle
import com.example.data.model.GridPos
import com.example.data.model.LevelType
import com.example.data.model.WordPlacement

object CuratedPuzzles {

  private fun p(vararg coords: Pair<Int, Int>): List<GridPos> =
    coords.map { (r, c) -> GridPos(r, c) }

  val signatureClassicPuzzle: FillwordPuzzle =
    FillwordPuzzle(
      id = "classic_8x11",
      levelNumber = 35,
      title = "Fillwords Classic",
      subtitle = "Signature 8×11 Board",
      pack = DifficultyPack.CLASSIC,
      rows = 11,
      cols = 8,
      isSignatureBoard = true,
      levelType = LevelType.SIGNATURE,
      placements = listOf(
        WordPlacement("QUESTION", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7), 3),
        WordPlacement("FILLWORD", p(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 1 to 7), 0),
        WordPlacement("SMART", p(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4), 4),
        WordPlacement("IDEA", p(2 to 5, 2 to 6, 2 to 7, 3 to 7), 5),
        WordPlacement("THINK", p(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4), 6),
        WordPlacement("LOGIC", p(3 to 6, 3 to 5, 4 to 5, 4 to 6, 4 to 7), 7),
        WordPlacement("BRAIN", p(4 to 0, 5 to 0, 5 to 1, 6 to 1, 6 to 0), 3),
        WordPlacement("BOOK", p(4 to 1, 4 to 2, 4 to 3, 4 to 4), 5),
        WordPlacement("WORD", p(5 to 2, 5 to 3, 5 to 4, 5 to 5), 1),
        WordPlacement("SEEN", p(5 to 6, 5 to 7, 6 to 7, 6 to 6), 4),
        WordPlacement("CLEVER", p(7 to 0, 7 to 1, 7 to 2, 6 to 2, 6 to 3, 7 to 3), 0),
        WordPlacement("SKILLS", p(7 to 4, 6 to 4, 6 to 5, 7 to 5, 7 to 6, 7 to 7), 6),
        WordPlacement("LETTER", p(8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5), 7),
        WordPlacement("PUZZLE", p(8 to 6, 8 to 7, 9 to 7, 9 to 6, 10 to 6, 10 to 7), 3),
        WordPlacement("SEARCH", p(9 to 0, 9 to 1, 9 to 2, 9 to 3, 9 to 4, 9 to 5), 2),
        WordPlacement("GENIUS", p(10 to 0, 10 to 1, 10 to 2, 10 to 3, 10 to 4, 10 to 5), 4),
      ),
    )

  private val handcraftedIntro: Map<Int, FillwordPuzzle> = mapOf(
    1 to FillwordPuzzle(
      id = "warmup_1",
      levelNumber = 1,
      title = "First Steps",
      subtitle = "Warmup 4×4",
      pack = DifficultyPack.WARMUP,
      rows = 4,
      cols = 4,
      placements = listOf(
        WordPlacement("WORD", p(0 to 0, 0 to 1, 0 to 2, 0 to 3), 0),
        WordPlacement("PLAY", p(1 to 0, 1 to 1, 2 to 1, 2 to 0), 1),
        WordPlacement("GAME", p(1 to 2, 1 to 3, 2 to 3, 2 to 2), 2),
        WordPlacement("QUIZ", p(3 to 0, 3 to 1, 3 to 2, 3 to 3), 3),
      ),
    ),
    2 to FillwordPuzzle(
      id = "warmup_2",
      levelNumber = 2,
      title = "Morning Brew",
      subtitle = "Warmup 4×4",
      pack = DifficultyPack.WARMUP,
      rows = 4,
      cols = 4,
      placements = listOf(
        WordPlacement("COFFEE", p(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 1, 1 to 0), 0),
        WordPlacement("TOAST", p(0 to 3, 1 to 3, 2 to 3, 3 to 3, 3 to 2), 1),
        WordPlacement("CREAM", p(2 to 0, 3 to 0, 3 to 1, 2 to 1, 2 to 2), 2),
      ),
    ),
    3 to FillwordPuzzle(
      id = "warmup_3",
      levelNumber = 3,
      title = "Sunny Day",
      subtitle = "Warmup 5×5",
      pack = DifficultyPack.WARMUP,
      rows = 5,
      cols = 5,
      placements = listOf(
        WordPlacement("SUNNY", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4), 0),
        WordPlacement("CLOUD", p(1 to 0, 1 to 1, 1 to 2, 2 to 2, 2 to 1), 3),
        WordPlacement("BREEZE", p(1 to 3, 1 to 4, 2 to 4, 2 to 3, 3 to 3, 3 to 4), 6),
        WordPlacement("WARM", p(2 to 0, 3 to 0, 4 to 0, 4 to 1), 1),
        WordPlacement("SHINE", p(3 to 1, 3 to 2, 4 to 2, 4 to 3, 4 to 4), 2),
      ),
    ),
    4 to FillwordPuzzle(
      id = "warmup_4",
      levelNumber = 4,
      title = "Soundtrack",
      subtitle = "Warmup 5×5",
      pack = DifficultyPack.WARMUP,
      rows = 5,
      cols = 5,
      placements = listOf(
        WordPlacement("MUSIC", p(0 to 0, 1 to 0, 1 to 1, 0 to 1, 0 to 2), 4),
        WordPlacement("PIANO", p(0 to 3, 0 to 4, 1 to 4, 1 to 3, 1 to 2), 0),
        WordPlacement("DRUMS", p(2 to 0, 3 to 0, 4 to 0, 4 to 1, 4 to 2), 1),
        WordPlacement("CHORD", p(2 to 1, 3 to 1, 3 to 2, 2 to 2, 2 to 3), 2),
        WordPlacement("NOTES", p(2 to 4, 3 to 4, 3 to 3, 4 to 3, 4 to 4), 3),
      ),
    ),
    5 to FillwordPuzzle(
      id = "warmup_5",
      levelNumber = 5,
      title = "Sweet Blitz",
      subtitle = "Timed Blitz 5×5",
      pack = DifficultyPack.WARMUP,
      rows = 5,
      cols = 5,
      levelType = LevelType.BLITZ,
      targetSeconds = 75,
      placements = listOf(
        WordPlacement("CAKE", p(0 to 0, 0 to 1, 1 to 1, 1 to 0), 1),
        WordPlacement("APPLE", p(0 to 2, 0 to 3, 0 to 4, 1 to 4, 1 to 3), 2),
        WordPlacement("HONEY", p(1 to 2, 2 to 2, 2 to 3, 2 to 4, 3 to 4), 0),
        WordPlacement("BREAD", p(2 to 0, 2 to 1, 3 to 1, 3 to 0, 4 to 0), 5),
        WordPlacement("PASTRY", p(4 to 1, 4 to 2, 3 to 2, 3 to 3, 4 to 3, 4 to 4), 4),
      ),
    ),
    26 to FillwordPuzzle(
      id = "classic_26",
      levelNumber = 26,
      title = "Wordsmith",
      subtitle = "Classic 6×6",
      pack = DifficultyPack.CLASSIC,
      rows = 6,
      cols = 6,
      placements = listOf(
        WordPlacement("LETTER", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), 0),
        WordPlacement("PUZZLE", p(1 to 0, 1 to 1, 1 to 2, 2 to 2, 2 to 1, 2 to 0), 1),
        WordPlacement("SOLVER", p(1 to 3, 1 to 4, 1 to 5, 2 to 5, 2 to 4, 2 to 3), 2),
        WordPlacement("BRAIN", p(3 to 0, 4 to 0, 5 to 0, 5 to 1, 4 to 1), 3),
        WordPlacement("WORD", p(3 to 1, 3 to 2, 4 to 2, 5 to 2), 4),
        WordPlacement("LOGIC", p(3 to 3, 3 to 4, 3 to 5, 4 to 5, 4 to 4), 5),
        WordPlacement("QUIZ", p(4 to 3, 5 to 3, 5 to 4, 5 to 5), 6),
      ),
    ),
    27 to FillwordPuzzle(
      id = "classic_27",
      levelNumber = 27,
      title = "Paper & Ink",
      subtitle = "Classic 6×6",
      pack = DifficultyPack.CLASSIC,
      rows = 6,
      cols = 6,
      placements = listOf(
        WordPlacement("PENCIL", p(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 1, 1 to 0), 0),
        WordPlacement("JOURNAL", p(0 to 3, 0 to 4, 0 to 5, 1 to 5, 1 to 4, 1 to 3, 2 to 3), 3),
        WordPlacement("STORY", p(2 to 0, 2 to 1, 2 to 2, 3 to 2, 3 to 1), 2),
        WordPlacement("BOOK", p(3 to 0, 4 to 0, 5 to 0, 5 to 1), 1),
        WordPlacement("NOVEL", p(4 to 1, 4 to 2, 5 to 2, 5 to 3, 4 to 3), 4),
        WordPlacement("WRITE", p(2 to 4, 2 to 5, 3 to 5, 3 to 4, 3 to 3), 5),
        WordPlacement("PAGE", p(4 to 4, 4 to 5, 5 to 5, 5 to 4), 6),
      ),
    ),
    35 to signatureClassicPuzzle,
    51 to FillwordPuzzle(
      id = "nature_51",
      levelNumber = 51,
      title = "Wild Forest",
      subtitle = "Nature 6×7",
      pack = DifficultyPack.NATURE,
      rows = 7,
      cols = 6,
      placements = listOf(
        WordPlacement("FOREST", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), 2),
        WordPlacement("RIVER", p(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4), 3),
        WordPlacement("TRAIL", p(1 to 5, 2 to 5, 3 to 5, 3 to 4, 2 to 4), 0),
        WordPlacement("MEADOW", p(2 to 0, 2 to 1, 2 to 2, 2 to 3, 3 to 3, 3 to 2), 6),
        WordPlacement("PINE", p(3 to 0, 3 to 1, 4 to 1, 4 to 0), 1),
        WordPlacement("FERNS", p(5 to 0, 6 to 0, 6 to 1, 5 to 1, 5 to 2), 4),
        WordPlacement("WOODS", p(6 to 2, 6 to 3, 5 to 3, 4 to 3, 4 to 2), 5),
        WordPlacement("STREAM", p(4 to 4, 4 to 5, 5 to 5, 5 to 4, 6 to 4, 6 to 5), 7),
      ),
    ),
    52 to FillwordPuzzle(
      id = "nature_52",
      levelNumber = 52,
      title = "Deep Ocean",
      subtitle = "Nature 7×7",
      pack = DifficultyPack.NATURE,
      rows = 7,
      cols = 7,
      placements = listOf(
        WordPlacement("DOLPHIN", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6), 3),
        WordPlacement("CORAL", p(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4), 1),
        WordPlacement("WAVE", p(1 to 5, 1 to 6, 2 to 6, 2 to 5), 6),
        WordPlacement("WHALE", p(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4), 4),
        WordPlacement("OCTOPUS", p(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5, 3 to 6), 5),
        WordPlacement("SHARK", p(4 to 0, 4 to 1, 4 to 2, 5 to 2, 5 to 1), 0),
        WordPlacement("REEF", p(5 to 0, 6 to 0, 6 to 1, 6 to 2), 2),
        WordPlacement("TURTLE", p(4 to 3, 4 to 4, 5 to 4, 5 to 3, 6 to 3, 6 to 4), 7),
        WordPlacement("ISLAND", p(4 to 5, 4 to 6, 5 to 6, 5 to 5, 6 to 5, 6 to 6), 3),
      ),
    ),
    76 to FillwordPuzzle(
      id = "mastermind_76",
      levelNumber = 76,
      title = "Tech Lab",
      subtitle = "Challenge 7×7",
      pack = DifficultyPack.MASTERMIND,
      rows = 7,
      cols = 7,
      placements = listOf(
        WordPlacement("BINARY", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), 3),
        WordPlacement("CLOUD", p(0 to 6, 1 to 6, 1 to 5, 1 to 4, 1 to 3), 6),
        WordPlacement("SERVER", p(1 to 0, 1 to 1, 1 to 2, 2 to 2, 2 to 1, 2 to 0), 0),
        WordPlacement("PIXEL", p(2 to 3, 2 to 4, 2 to 5, 2 to 6, 3 to 6), 1),
        WordPlacement("CODING", p(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5), 2),
        WordPlacement("MEMORY", p(4 to 0, 4 to 1, 4 to 2, 5 to 2, 5 to 1, 5 to 0), 4),
        WordPlacement("LOGIC", p(6 to 0, 6 to 1, 6 to 2, 6 to 3, 6 to 4), 5),
        WordPlacement("ROBOT", p(4 to 3, 5 to 3, 5 to 4, 4 to 4, 4 to 5), 7),
        WordPlacement("CYBER", p(4 to 6, 5 to 6, 5 to 5, 6 to 5, 6 to 6), 3),
      ),
    ),
    100 to FillwordPuzzle(
      id = "mastermind_100",
      levelNumber = 100,
      title = "Grand Finale",
      subtitle = "Mega 8×8 Crown",
      pack = DifficultyPack.MASTERMIND,
      rows = 8,
      cols = 8,
      levelType = LevelType.MEGA,
      placements = listOf(
        WordPlacement("VICTORY", p(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6), 0),
        WordPlacement("WISDOM", p(0 to 7, 1 to 7, 1 to 6, 1 to 5, 1 to 4, 1 to 3), 4),
        WordPlacement("CIPHER", p(1 to 0, 1 to 1, 1 to 2, 2 to 2, 2 to 1, 2 to 0), 1),
        WordPlacement("RIDDLE", p(2 to 3, 2 to 4, 2 to 5, 2 to 6, 2 to 7, 3 to 7), 2),
        WordPlacement("MEMORY", p(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5), 3),
        WordPlacement("FOCUS", p(3 to 6, 4 to 6, 4 to 7, 5 to 7, 5 to 6), 5),
        WordPlacement("CLEVER", p(4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 5), 6),
        WordPlacement("GENIUS", p(5 to 0, 5 to 1, 5 to 2, 5 to 3, 5 to 4, 5 to 5), 7),
        WordPlacement("ANSWER", p(6 to 0, 7 to 0, 7 to 1, 6 to 1, 6 to 2, 7 to 2), 0),
        WordPlacement("PUZZLE", p(6 to 3, 7 to 3, 7 to 4, 6 to 4, 6 to 5, 7 to 5), 2),
        WordPlacement("MIND", p(6 to 6, 6 to 7, 7 to 7, 7 to 6), 1),
      ),
    ),
  )

  private val creativeTitles = listOf(
    "Morning Spark", "Color Splash", "Cozy Corner", "Swift Trail", "Golden Hour",
    "Meadow Breeze", "Starlight", "Paper Lantern", "Crystal Tide", "Amber Glow",
    "Hidden Path", "City Rhythm", "Emerald Grove", "Skyward", "Silent Peak",
    "Velvet Dusk", "Silver Lining", "Neon Pulse", "Harbor Mist", "Compass Rose",
    "Prism Twist", "Aurora Drift", "Solar Flare", "Maple Leaf", "Summit View",
    "Echo Chamber", "Alchemy Lab", "Moonbeam", "Whispering Pines", "Treasure Hunt",
  )

  val allPuzzles: List<FillwordPuzzle> by lazy {
    (1..100).map { levelNum ->
      handcraftedIntro[levelNum] ?: buildDeterministicLevel(levelNum)
    }
  }

  private fun buildDeterministicLevel(levelNum: Int): FillwordPuzzle {
    val pack = when (levelNum) {
      in 1..25 -> DifficultyPack.WARMUP
      in 26..50 -> DifficultyPack.CLASSIC
      in 51..75 -> DifficultyPack.NATURE
      else -> DifficultyPack.MASTERMIND
    }

    val (rows, cols) = when (levelNum) {
      in 1..8 -> 4 to 4
      in 9..25 -> 5 to 5
      in 26..37 -> 6 to 5
      in 38..50 -> 6 to 6
      in 51..62 -> 7 to 6
      in 63..75 -> 7 to 7
      in 76..88 -> 8 to 7
      else -> 8 to 8
    }

    val isBlitz = levelNum % 5 == 0
    val isMega = levelNum == 25 || levelNum == 50 || levelNum == 75
    val levelType = when {
      isMega -> LevelType.MEGA
      isBlitz -> LevelType.BLITZ
      else -> LevelType.CLASSIC
    }

    val targetSeconds = if (levelType == LevelType.BLITZ) {
      when {
        rows * cols <= 25 -> 80
        rows * cols <= 36 -> 115
        rows * cols <= 49 -> 150
        else -> 185
      }
    } else {
      0
    }

    val themeTitle = creativeTitles[(levelNum * 7) % creativeTitles.size]
    val title = if (levelType == LevelType.BLITZ) "$themeTitle Blitz" else themeTitle
    val subtitle = when (levelType) {
      LevelType.BLITZ -> "Timed Blitz ${cols}×${rows}"
      LevelType.MEGA -> "Mega Board ${cols}×${rows}"
      else -> "${pack.displayName} ${cols}×${rows}"
    }

    return PuzzleGenerator.generate(
      rows = rows,
      cols = cols,
      seed = 42_000L + levelNum * 7919L,
      id = "${pack.name.lowercase()}_$levelNum",
      levelNumber = levelNum,
      title = title,
      subtitle = subtitle,
      pack = pack,
      levelType = levelType,
      targetSeconds = targetSeconds,
    )
  }

  fun findById(id: String): FillwordPuzzle? = allPuzzles.firstOrNull { it.id == id }

  fun nextPuzzleAfter(currentId: String): FillwordPuzzle? {
    val index = allPuzzles.indexOfFirst { it.id == currentId }
    return if (index in 0 until allPuzzles.lastIndex) allPuzzles[index + 1] else null
  }
}