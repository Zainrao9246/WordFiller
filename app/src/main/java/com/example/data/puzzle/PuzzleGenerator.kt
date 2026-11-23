package com.example.data.puzzle

import com.example.data.model.DifficultyPack
import com.example.data.model.FillwordPuzzle
import com.example.data.model.GridPos
import com.example.data.model.LevelType
import com.example.data.model.WordPlacement
import kotlin.random.Random

object PuzzleGenerator {

  private val wordsByLength: Map<Int, List<String>> = mapOf(
    3 to listOf(
      "SUN", "SKY", "MAP", "GEM", "OAK", "ICE", "JOY", "ART", "PEN", "CUP",
      "BOX", "KEY", "RUN", "FUN", "TEA", "AIR", "SEA", "RED", "ORB", "ZEN",
      "RAY", "DEW", "FOX", "OWL", "BEE", "JET", "ARC", "INK", "HUE", "WIZ",
      "FIN", "FIR", "FIG", "YAM", "OAT", "RYE", "HOP", "FLY", "ACE", "BUD"
    ),
    4 to listOf(
      "WORD", "PLAY", "GAME", "QUIZ", "BOOK", "MIND", "IDEA", "CODE", "STAR",
      "MOON", "WIND", "RAIN", "FIRE", "LEAF", "TREE", "WAVE", "SAND", "GOLD",
      "RUBY", "JADE", "GLOW", "DAWN", "DUSK", "ECHO", "PEAK", "PATH", "HOME",
      "LAMP", "CAKE", "SOUP", "MINT", "NEON", "HERO", "LUCK", "ZOOM", "CORE",
      "EDGE", "FLOW", "LINK", "GRID", "TILE", "CLUE", "HINT", "NOTE", "TUNE",
      "SONG", "BEAT", "DRUM", "HARP", "BLUE", "CYAN", "ROSE", "JAZZ", "FOLK",
      "WARM", "SNOW", "MIST", "BIRD", "DEER", "BEAR", "WOLF", "FROG", "CORN",
      "PLUM", "PEAR", "LIME", "TEAL", "SILK", "WOOL", "IRON", "BOLT", "GIFT"
    ),
    5 to listOf(
      "SMART", "THINK", "LOGIC", "BRAIN", "SKILL", "SOLVE", "CRAFT", "SPARK",
      "LIGHT", "FLAME", "OCEAN", "RIVER", "CLOUD", "STORM", "FROST", "BLOOM",
      "MAPLE", "CEDAR", "CORAL", "PEARL", "AMBER", "TOPAZ", "PRISM", "LASER",
      "ORBIT", "COMET", "SPACE", "LUNAR", "SOLAR", "APPLE", "HONEY", "BREAD",
      "BERRY", "MANGO", "LEMON", "PEACH", "PIANO", "CHORD", "TEMPO", "MAGIC",
      "DREAM", "QUEST", "TRAIL", "STONE", "STEEL", "POWER", "SWIFT", "FOCUS",
      "HAPPY", "BRAVE", "NOBLE", "VIVID", "GLEAM", "PULSE", "STAGE", "CLOCK",
      "EARTH", "FIELD", "GREEN", "HEART", "HOUSE", "MUSIC", "PEACE", "PLANT",
      "QUIET", "RADIO", "SMILE", "SOUND", "SWEET", "TOWER", "TRAIN", "WORLD"
    ),
    6 to listOf(
      "PUZZLE", "LETTER", "SEARCH", "GENIUS", "CLEVER", "ANSWER", "RIDDLE",
      "CIPHER", "MEMORY", "WISDOM", "PLANET", "GALAXY", "NEBULA", "ROCKET",
      "METEOR", "FOREST", "MEADOW", "CANYON", "VALLEY", "DESERT", "ISLAND",
      "STREAM", "SILVER", "GOLDEN", "ORANGE", "VIOLET", "INDIGO", "BRONZE",
      "COFFEE", "MUFFIN", "COOKIE", "PASTRY", "PEPPER", "GARLIC", "GUITAR",
      "VIOLIN", "RHYTHM", "MELODY", "BRIDGE", "CASTLE", "HARBOR", "MARKET",
      "MUSEUM", "GARDEN", "WINTER", "SUMMER", "SPRING", "AUTUMN", "SPIRIT",
      "WONDER", "ENERGY", "MOTION", "VECTOR", "SIGNAL", "MATRIX", "BRIGHT",
      "DRAGON", "FLIGHT", "FLOWER", "FRIEND", "FUTURE", "JUNGLE", "KNIGHT",
      "LANTERN".take(6), "MIRROR", "NATURE", "PORTAL", "SECRET", "SHADOW", "SUNSET"
    ).distinct().filter { it.length == 6 },
    7 to listOf(
      "FILLING", "JOURNEY", "EXPLORE", "HORIZON", "COMPASS", "CRYSTAL",
      "DIAMOND", "EMERALD", "RAINBOW", "THUNDER", "GLACIER", "VOLCANO",
      "DOLPHIN", "OCTOPUS", "PANTHER", "PHOENIX", "LANTERN", "LIBRARY",
      "CHAPTER", "PENCILS", "BALLOON", "HARMONY", "MYSTERY", "FORTUNE",
      "VICTORY", "COURAGE", "SCIENCE", "NETWORK", "CIRCUIT", "DIGITAL",
      "QUANTUM", "GRAVITY", "ECLIPSE", "MORNING", "EVENING", "BLOSSOM",
      "FEATHER", "ORCHARD", "WHISPER", "SPARKLE", "KINGDOM", "SUNRISE",
      "ELEMENT", "BATTERY", "CAPTAIN", "MONARCH", "ODYSSEY", "RADIANT",
      "CITADEL", "MIRACLE", "PIONEER", "TRIUMPH"
    ).distinct().filter { it.length == 7 },
    8 to listOf(
      "FILLWORD", "QUESTION", "TREASURE", "DISCOVER", "STARDUST", "MOUNTAIN",
      "SUNLIGHT", "MOONBEAM", "KEYBOARD", "NOTEBOOK", "BACKPACK", "BLUEBIRD",
      "CREATIVE", "SPECTRUM", "MAGNETIC", "PARADISE", "CHAMPION", "FIREWORK",
      "SNOWFALL", "RAINDROP", "STARFISH", "LAVENDER", "SAPPHIRE", "AMETHYST",
      "DAYDREAM", "WATERWAY", "FOOTSTEP", "SKYLINE", "EVERGREEN".take(8), "INFINITY"
    ).distinct().filter { it.length == 8 },
  )

  private val dirs = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

  fun generate(
    rows: Int,
    cols: Int,
    seed: Long = System.nanoTime(),
    id: String = "endless_${rows}x${cols}_$seed",
    levelNumber: Int = 0,
    title: String = "Endless ${cols}×${rows}",
    subtitle: String? = null,
    pack: DifficultyPack = DifficultyPack.CLASSIC,
    levelType: LevelType = LevelType.CLASSIC,
    targetSeconds: Int = 0,
  ): FillwordPuzzle {
    val rng = Random(seed)
    val totalCells = rows * cols

    for (attempt in 0 until 80) {
      val segments = tryPartitionGrid(rows, cols, rng) ?: continue
      val usedWords = HashSet<String>()
      val placements = ArrayList<WordPlacement>(segments.size)
      var allMatched = true

      segments.forEachIndexed { idx, path ->
        val candidates = wordsByLength[path.size]?.filter { it !in usedWords }
        if (candidates.isNullOrEmpty()) {
          allMatched = false
          return@forEachIndexed
        }
        val chosen = candidates[rng.nextInt(candidates.size)]
        usedWords.add(chosen)
        placements.add(
          WordPlacement(
            word = chosen,
            path = path,
            colorIndex = idx % 8,
          )
        )
      }

      if (allMatched && placements.sumOf { it.path.size } == totalCells) {
        val puzzle = FillwordPuzzle(
          id = id,
          levelNumber = levelNumber,
          title = title,
          subtitle = subtitle ?: "${placements.size} Words",
          pack = pack,
          rows = rows,
          cols = cols,
          placements = placements,
          levelType = levelType,
          targetSeconds = targetSeconds,
        )
        if (puzzle.validate()) return puzzle
      }
    }

    return deterministicRowFallback(
      rows = rows,
      cols = cols,
      rng = rng,
      id = id,
      levelNumber = levelNumber,
      title = title,
      subtitle = subtitle,
      pack = pack,
      levelType = levelType,
      targetSeconds = targetSeconds,
    )
  }

  private fun tryPartitionGrid(rows: Int, cols: Int, rng: Random): List<List<GridPos>>? {
    val visited = Array(rows) { BooleanArray(cols) }
    val segments = ArrayList<List<GridPos>>()
    var remaining = rows * cols

    while (remaining > 0) {
      val start = pickBestStartCell(rows, cols, visited, rng) ?: return null
      val targetLen = when {
        remaining <= 8 -> remaining
        remaining in 9..10 -> if (rng.nextBoolean()) 5 else 4
        else -> rng.nextInt(4, 8)
      }

      val path = growSnake(rows, cols, visited, start, targetLen, rng)
      if (path.size < 3) {
        return null
      }
      if (!areRemainingComponentsValid(rows, cols, visited)) {
        return null
      }
      segments.add(path)
      remaining -= path.size
    }
    return segments
  }

  private fun pickBestStartCell(
    rows: Int,
    cols: Int,
    visited: Array<BooleanArray>,
    rng: Random,
  ): GridPos? {
    var bestDegree = Int.MAX_VALUE
    val candidates = ArrayList<GridPos>()
    for (r in 0 until rows) {
      for (c in 0 until cols) {
        if (!visited[r][c]) {
          val deg = countUnvisitedNeighbors(r, c, rows, cols, visited)
          if (deg < bestDegree) {
            bestDegree = deg
            candidates.clear()
            candidates.add(GridPos(r, c))
          } else if (deg == bestDegree) {
            candidates.add(GridPos(r, c))
          }
        }
      }
    }
    return if (candidates.isEmpty()) null else candidates[rng.nextInt(candidates.size)]
  }

  private fun growSnake(
    rows: Int,
    cols: Int,
    visited: Array<BooleanArray>,
    start: GridPos,
    targetLen: Int,
    rng: Random,
  ): List<GridPos> {
    val path = ArrayList<GridPos>(targetLen)
    var current = start
    visited[current.row][current.col] = true
    path.add(current)

    while (path.size < targetLen) {
      val neighbors = dirs.mapNotNull { (dr, dc) ->
        val nr = current.row + dr
        val nc = current.col + dc
        if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) {
          GridPos(nr, nc)
        } else {
          null
        }
      }
      if (neighbors.isEmpty()) break

      val scored = neighbors.map { pos ->
        pos to countUnvisitedNeighbors(pos.row, pos.col, rows, cols, visited)
      }.sortedBy { it.second }

      val minDeg = scored.first().second
      val topChoices = scored.filter { it.second <= minDeg + 1 }.map { it.first }
      current = topChoices[rng.nextInt(topChoices.size)]
      visited[current.row][current.col] = true
      path.add(current)
    }
    return path
  }

  private fun countUnvisitedNeighbors(
    r: Int,
    c: Int,
    rows: Int,
    cols: Int,
    visited: Array<BooleanArray>,
  ): Int {
    var count = 0
    for ((dr, dc) in dirs) {
      val nr = r + dr
      val nc = c + dc
      if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) {
        count++
      }
    }
    return count
  }

  private fun areRemainingComponentsValid(
    rows: Int,
    cols: Int,
    visited: Array<BooleanArray>,
  ): Boolean {
    val seen = Array(rows) { BooleanArray(cols) }
    val queue = ArrayDeque<GridPos>()
    for (r in 0 until rows) {
      for (c in 0 until cols) {
        if (!visited[r][c] && !seen[r][c]) {
          var size = 0
          queue.add(GridPos(r, c))
          seen[r][c] = true
          while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            size++
            for ((dr, dc) in dirs) {
              val nr = cur.row + dr
              val nc = cur.col + dc
              if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc] && !seen[nr][nc]) {
                seen[nr][nc] = true
                queue.add(GridPos(nr, nc))
              }
            }
          }
          if (size in 1..2) return false
        }
      }
    }
    return true
  }

  private fun deterministicRowFallback(
    rows: Int,
    cols: Int,
    rng: Random,
    id: String,
    levelNumber: Int,
    title: String,
    subtitle: String?,
    pack: DifficultyPack,
    levelType: LevelType,
    targetSeconds: Int,
  ): FillwordPuzzle {
    val snake = ArrayList<GridPos>(rows * cols)
    for (r in 0 until rows) {
      if (r % 2 == 0) {
        for (c in 0 until cols) snake.add(GridPos(r, c))
      } else {
        for (c in cols - 1 downTo 0) snake.add(GridPos(r, c))
      }
    }
    val usedWords = HashSet<String>()
    val placements = ArrayList<WordPlacement>()
    var index = 0
    var colorIdx = 0
    while (index < snake.size) {
      val rem = snake.size - index
      val len = when {
        rem <= 7 -> rem
        rem == 8 -> 4
        rem == 9 -> 5
        else -> 5
      }
      val slice = snake.subList(index, index + len).toList()
      val pool = wordsByLength[len].orEmpty().filter { it !in usedWords }
      val word = if (pool.isNotEmpty()) pool[rng.nextInt(pool.size)] else "WORDS".take(len)
      usedWords.add(word)
      placements.add(WordPlacement(word, slice, colorIdx % 8))
      index += len
      colorIdx++
    }
    return FillwordPuzzle(
      id = id,
      levelNumber = levelNumber,
      title = title,
      subtitle = subtitle ?: "${placements.size} Words",
      pack = pack,
      rows = rows,
      cols = cols,
      placements = placements,
      levelType = levelType,
      targetSeconds = targetSeconds,
    )
  }
}
