package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FillwordsDatabase
import com.example.data.local.LevelProgressEntity
import com.example.data.local.PlayerStatsEntity
import com.example.data.model.DifficultyPack
import com.example.data.model.FillwordPuzzle
import com.example.data.model.GridPos
import com.example.data.model.LevelType
import com.example.data.model.WordPlacement
import com.example.data.puzzle.BonusDictionary
import com.example.data.puzzle.CuratedPuzzles
import com.example.data.puzzle.PuzzleGenerator
import com.example.data.repository.FillwordsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface AppScreen {
  data object Home : AppScreen
  data class LevelSelect(val selectedPack: DifficultyPack? = null) : AppScreen
  data object Playing : AppScreen
  data object Vault : AppScreen
  data object MiniGames : AppScreen
  data class MiniGamePlaying(val game: MiniGameType) : AppScreen
}

enum class MiniGameType {
  ANAGRAM_RUSH,
  WORD_CHAIN,
  HANGMAN,
  MEMORY_MATCH,
  TIC_TAC_TOE,
}

enum class FeedbackTone {
  POSITIVE,
  BONUS,
  REWARD,
  WARNING,
}

data class TraceFeedback(
  val message: String,
  val isPositive: Boolean,
  val tone: FeedbackTone = if (isPositive) FeedbackTone.POSITIVE else FeedbackTone.WARNING,
  val id: Long = System.nanoTime(),
)

enum class TraceStepOutcome {
  IGNORED,
  ADDED,
  BACKTRACKED,
}

data class GameSessionState(
  val puzzle: FillwordPuzzle,
  val charGrid: Array<CharArray>,
  val isCurated: Boolean,
  val isDailyChallenge: Boolean = false,
  val foundWordKeys: List<String> = emptyList(),
  val bonusWordsFound: List<String> = emptyList(),
  val activeTrace: List<GridPos> = emptyList(),
  val activeTraceColorIndex: Int = 0,
  val hintedCell: GridPos? = null,
  val hintedWord: String? = null,
  val elapsedSeconds: Int = 0,
  val hintsUsed: Int = 0,
  val comboCount: Int = 0,
  val lastFoundAtMs: Long = 0L,
  val isPaused: Boolean = false,
  val isCompleted: Boolean = false,
  val earnedStars: Int = 0,
  val scoreEarned: Int = 0,
  val blitzBeatenInTime: Boolean = false,
  val victoryHeadline: String = "Board Complete!",
  val victorySubmessage: String = "Every tile connected cleanly!",
  val shakeTrigger: Int = 0,
  val feedback: TraceFeedback? = null,
) {
  val foundPlacements: List<WordPlacement>
    get() = foundWordKeys.mapNotNull { key ->
      puzzle.placements.firstOrNull { it.word == key }
    }

  val solvedCellMap: Map<GridPos, WordPlacement>
    get() {
      val map = HashMap<GridPos, WordPlacement>()
      for (placement in foundPlacements) {
        for (pos in placement.path) {
          map[pos] = placement
        }
      }
      return map
    }

  val solvedCellCount: Int
    get() = foundPlacements.sumOf { it.path.size }

  val totalCellCount: Int
    get() = puzzle.rows * puzzle.cols

  val tracedWordPreview: String
    get() = activeTrace.joinToString("") { pos ->
      charGrid[pos.row][pos.col].toString()
    }

  val blitzRemainingSeconds: Int
    get() = if (puzzle.levelType == LevelType.BLITZ && puzzle.targetSeconds > 0) {
      (puzzle.targetSeconds - elapsedSeconds).coerceAtLeast(0)
    } else {
      0
    }
}

class FillwordsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: FillwordsRepository by lazy {
    val dao = FillwordsDatabase.getInstance(application).fillwordsDao()
    FillwordsRepository(dao)
  }

  val allProgress: StateFlow<Map<String, LevelProgressEntity>> =
    repository.allProgress.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyMap(),
    )

  val playerStats: StateFlow<PlayerStatsEntity> =
    repository.playerStats.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = PlayerStatsEntity(),
    )

  private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _gameState = MutableStateFlow<GameSessionState?>(null)
  val gameState: StateFlow<GameSessionState?> = _gameState.asStateFlow()

  private var timerJob: Job? = null

  private val happyWordPraises = listOf(
    "Awesome!",
    "Brilliant!",
    "Spot On!",
    "Word Wizard!",
    "Superb!",
    "Clever Find!",
    "Nailed It!",
  )

  private val happyVictoryHeadlines = listOf(
    "Pure Genius!",
    "Fantastic Victory!",
    "Board Mastered!",
    "Unstoppable!",
    "Word Champion!",
  )

  init {
    viewModelScope.launch {
      repository.ensureInitialStats()
    }
  }

  fun navigateHome() {
    saveCurrentProgressIfActive()
    stopTimer()
    _currentScreen.value = AppScreen.Home
  }

  fun navigateToLevelSelect(pack: DifficultyPack? = null) {
    saveCurrentProgressIfActive()
    stopTimer()
    _currentScreen.value = AppScreen.LevelSelect(pack)
  }

  fun navigateToVault() {
    saveCurrentProgressIfActive()
    stopTimer()
    _currentScreen.value = AppScreen.Vault
  }

  fun navigateToMiniGames() {
    saveCurrentProgressIfActive()
    stopTimer()
    _currentScreen.value = AppScreen.MiniGames
  }

  fun openMiniGame(game: MiniGameType) {
    saveCurrentProgressIfActive()
    stopTimer()
    _currentScreen.value = AppScreen.MiniGamePlaying(game)
  }

  fun playNextOrContinue() {
    val progressMap = allProgress.value
    val inProgress = CuratedPuzzles.allPuzzles.firstOrNull { puzzle ->
      val p = progressMap[puzzle.id]
      p != null && !p.isCompleted && (p.foundWordsCsv.isNotBlank() || p.bonusWordsCsv.isNotBlank())
    }
    val nextUnsolved = CuratedPuzzles.allPuzzles.firstOrNull { puzzle ->
      progressMap[puzzle.id]?.isCompleted != true
    }
    val target = inProgress ?: nextUnsolved ?: CuratedPuzzles.allPuzzles.first()
    startCuratedPuzzle(target)
  }

  fun playSignatureClassicBoard() {
    startCuratedPuzzle(CuratedPuzzles.signatureClassicPuzzle)
  }

  fun startCuratedPuzzle(puzzle: FillwordPuzzle, forceFresh: Boolean = false) {
    val saved = if (forceFresh) null else allProgress.value[puzzle.id]
    val restoredWords = if (saved != null && !saved.isCompleted && saved.foundWordsCsv.isNotBlank()) {
      saved.foundWordsCsv.split(",").filter { word ->
        puzzle.placements.any { it.word == word }
      }
    } else {
      emptyList()
    }
    val restoredBonus = if (saved != null && !saved.isCompleted && saved.bonusWordsCsv.isNotBlank()) {
      saved.bonusWordsCsv.split(",").filter { it.isNotBlank() }
    } else {
      emptyList()
    }
    val restoredElapsed = if (saved != null && !saved.isCompleted) saved.elapsedSeconds else 0
    val restoredHints = if (saved != null && !saved.isCompleted) saved.hintsUsed else 0

    val nextColor = nextAvailableColorIndex(puzzle, restoredWords)
    val welcomeMessage = when (puzzle.levelType) {
      LevelType.BLITZ -> "Beat ${puzzle.targetSeconds}s for a Blitz Speed Bonus!"
      LevelType.MEGA -> "Mega Board! Extra words fill your Hint Bucket!"
      LevelType.SIGNATURE -> "The iconic 8×11 Fillwords challenge!"
      LevelType.CLASSIC -> if (restoredWords.isNotEmpty()) "Welcome back — progress restored!" else null
    }

    _gameState.value = GameSessionState(
      puzzle = puzzle,
      charGrid = puzzle.buildCharGrid(),
      isCurated = true,
      foundWordKeys = restoredWords,
      bonusWordsFound = restoredBonus,
      activeTraceColorIndex = nextColor,
      elapsedSeconds = restoredElapsed,
      hintsUsed = restoredHints,
      feedback = welcomeMessage?.let { TraceFeedback(it, isPositive = true, tone = FeedbackTone.POSITIVE) },
    )
    _currentScreen.value = AppScreen.Playing
    startTimer()
  }

  fun startEndlessPuzzle(rows: Int, cols: Int) {
    val generated = PuzzleGenerator.generate(rows = rows, cols = cols)
    _gameState.value = GameSessionState(
      puzzle = generated,
      charGrid = generated.buildCharGrid(),
      isCurated = false,
      activeTraceColorIndex = generated.placements.firstOrNull()?.colorIndex ?: 0,
      feedback = TraceFeedback("Fresh ${cols}×${rows} board ready — have fun!", isPositive = true),
    )
    _currentScreen.value = AppScreen.Playing
    startTimer()
  }

  fun startDailyChallenge() {
    val dateKey = todayDateKey()
    val seed = dateKey.hashCode().toLong() * 31_337L + 7_919L
    val generated = PuzzleGenerator.generate(rows = 7, cols = 7, seed = seed).copy(
      id = "daily_$dateKey",
      title = "Daily Challenge",
      subtitle = "Daily 7×7 • $dateKey",
    )
    _gameState.value = GameSessionState(
      puzzle = generated,
      charGrid = generated.buildCharGrid(),
      isCurated = false,
      isDailyChallenge = true,
      activeTraceColorIndex = generated.placements.firstOrNull()?.colorIndex ?: 0,
      feedback = TraceFeedback("Daily 7×7 Challenge — win +2 Bonus Hints!", isPositive = true, tone = FeedbackTone.BONUS),
    )
    _currentScreen.value = AppScreen.Playing
    startTimer()
  }

  fun playNextLevel() {
    val current = _gameState.value ?: return
    if (current.isCurated) {
      val next = CuratedPuzzles.nextPuzzleAfter(current.puzzle.id)
      if (next != null) {
        startCuratedPuzzle(next)
      } else {
        navigateToLevelSelect()
      }
    } else if (current.isDailyChallenge) {
      navigateHome()
    } else {
      startEndlessPuzzle(current.puzzle.rows, current.puzzle.cols)
    }
  }

  fun onTraceStart(pos: GridPos): Boolean {
    val state = _gameState.value ?: return false
    if (state.isPaused || state.isCompleted) return false
    if (pos.row !in 0 until state.puzzle.rows || pos.col !in 0 until state.puzzle.cols) return false
    if (state.solvedCellMap.containsKey(pos)) return false

    val nextColor = nextAvailableColorIndex(state.puzzle, state.foundWordKeys)
    _gameState.update {
      it?.copy(
        activeTrace = listOf(pos),
        activeTraceColorIndex = nextColor,
      )
    }
    return true
  }

  fun onTraceStep(pos: GridPos): TraceStepOutcome {
    val state = _gameState.value ?: return TraceStepOutcome.IGNORED
    if (state.isPaused || state.isCompleted) return TraceStepOutcome.IGNORED
    val trace = state.activeTrace
    if (trace.isEmpty()) return TraceStepOutcome.IGNORED
    if (pos == trace.last()) return TraceStepOutcome.IGNORED

    if (trace.size >= 2 && pos == trace[trace.size - 2]) {
      _gameState.update { it?.copy(activeTrace = trace.dropLast(1)) }
      return TraceStepOutcome.BACKTRACKED
    }

    if (pos.row !in 0 until state.puzzle.rows || pos.col !in 0 until state.puzzle.cols) {
      return TraceStepOutcome.IGNORED
    }
    if (pos in trace) return TraceStepOutcome.IGNORED
    if (state.solvedCellMap.containsKey(pos)) return TraceStepOutcome.IGNORED
    if (!pos.isOrthogonallyAdjacentTo(trace.last())) return TraceStepOutcome.IGNORED

    _gameState.update { it?.copy(activeTrace = trace + pos) }
    return TraceStepOutcome.ADDED
  }

  fun onTraceEnd(): Boolean {
    val state = _gameState.value ?: return false
    val trace = state.activeTrace
    if (trace.isEmpty()) return false

    if (trace.size == 1) {
      _gameState.update { it?.copy(activeTrace = emptyList()) }
      return false
    }

    val tracedWord = trace.joinToString("") { pos ->
      state.charGrid[pos.row][pos.col].toString()
    }
    val remaining = state.puzzle.placements.filter { it.word !in state.foundWordKeys }

    // 1. Exact target path match
    val exactMatch = remaining.firstOrNull { it.path == trace }
    if (exactMatch != null) {
      commitSolvedWord(exactMatch)
      return true
    }

    // 2. Target word spelled on another path
    val textMatch = remaining.firstOrNull { it.word == tracedWord }
    if (textMatch != null) {
      _gameState.update {
        it?.copy(
          activeTrace = emptyList(),
          shakeTrigger = it.shakeTrigger + 1,
          feedback = TraceFeedback(
            "Right word '$tracedWord' — try another path!",
            isPositive = false,
            tone = FeedbackTone.WARNING,
          ),
        )
      }
      return false
    }

    // 3. Extra Words Bucket check!
    val isExtraDictionaryWord = tracedWord.length >= 3 &&
      (tracedWord in BonusDictionary.words || state.puzzle.placements.none { it.word == tracedWord } && tracedWord in allCuratedWords)
    if (isExtraDictionaryWord) {
      if (tracedWord in state.bonusWordsFound) {
        _gameState.update {
          it?.copy(
            activeTrace = emptyList(),
            feedback = TraceFeedback(
              "Already in Bucket: $tracedWord",
              isPositive = true,
              tone = FeedbackTone.BONUS,
            ),
          )
        }
        return true
      }

      val updatedBonus = state.bonusWordsFound + tracedWord
      _gameState.update {
        it?.copy(
          activeTrace = emptyList(),
          bonusWordsFound = updatedBonus,
        )
      }

      viewModelScope.launch {
        val reward = repository.recordExtraWord(
          puzzleId = state.puzzle.id,
          isCuratedLevel = state.isCurated,
          foundWords = state.foundWordKeys,
          bonusWords = updatedBonus,
          elapsedSeconds = state.elapsedSeconds,
          hintsUsed = state.hintsUsed,
        )
        val message = if (reward.earnedHint) {
          "Bucket Full! '$tracedWord' earned +1 Free Hint!"
        } else {
          "Extra Word '$tracedWord'! Bucket ${reward.newBucketCount}/${FillwordsDatabase.BUCKET_TARGET_WORDS}"
        }
        _gameState.update {
          it?.copy(
            feedback = TraceFeedback(
              message = message,
              isPositive = true,
              tone = if (reward.earnedHint) FeedbackTone.REWARD else FeedbackTone.BONUS,
            ),
          )
        }
      }
      return true
    }

    // 4. Not a valid target or bonus word
    _gameState.update {
      it?.copy(
        activeTrace = emptyList(),
        shakeTrigger = it.shakeTrigger + 1,
        feedback = TraceFeedback("Keep looking — not a match", isPositive = false, tone = FeedbackTone.WARNING),
      )
    }
    return false
  }

  private fun commitSolvedWord(placement: WordPlacement) {
    val current = _gameState.value ?: return
    val newFound = current.foundWordKeys + placement.word
    val allDone = newFound.size == current.puzzle.placements.size

    val now = System.currentTimeMillis()
    val newCombo = if (current.lastFoundAtMs > 0L && (now - current.lastFoundAtMs) <= 9_000L) {
      current.comboCount + 1
    } else {
      1
    }

    if (allDone) {
      stopTimer()
      val isBlitz = current.puzzle.levelType == LevelType.BLITZ && current.puzzle.targetSeconds > 0
      val beatBlitzClock = isBlitz && current.elapsedSeconds <= current.puzzle.targetSeconds

      val stars = when {
        isBlitz && !beatBlitzClock -> 1
        current.hintsUsed == 0 -> 3
        current.hintsUsed <= 2 -> 2
        else -> 1
      }
      val baseScore = current.puzzle.rows * current.puzzle.cols * 10
      val starBonus = stars * 50
      val blitzBonus = if (beatBlitzClock) 100 else 0
      val hintPenalty = current.hintsUsed * 20
      val scoreEarned = maxOf(50, baseScore + starBonus + blitzBonus - hintPenalty)

      val headline = when {
        beatBlitzClock -> "Lightning Fast!"
        current.puzzle.isSignatureBoard -> "Signature Mastered!"
        stars == 3 -> happyVictoryHeadlines[current.puzzle.levelNumber % happyVictoryHeadlines.size]
        else -> "Well Done!"
      }

      val submessage = when {
        beatBlitzClock -> "You crushed the Blitz clock (+100 speed bonus)!"
        isBlitz -> "Board solved! Try Blitz again faster for 3 stars."
        current.bonusWordsFound.isNotEmpty() ->
          "Plus ${current.bonusWordsFound.size} extra word${if (current.bonusWordsFound.size == 1) "" else "s"} added to your Bucket!"
        else -> "Every single tile connected with zero gaps!"
      }

      _gameState.update {
        it?.copy(
          foundWordKeys = newFound,
          activeTrace = emptyList(),
          hintedCell = null,
          hintedWord = null,
          comboCount = newCombo,
          lastFoundAtMs = now,
          isCompleted = true,
          earnedStars = stars,
          scoreEarned = scoreEarned,
          blitzBeatenInTime = beatBlitzClock,
          victoryHeadline = headline,
          victorySubmessage = submessage,
          feedback = TraceFeedback("$headline Board Complete!", isPositive = true, tone = FeedbackTone.REWARD),
        )
      }

      viewModelScope.launch {
        if (current.isDailyChallenge) {
          repository.completeDailyChallenge(
            dateKey = todayDateKey(),
            yesterdayKey = yesterdayDateKey(),
            wordsCount = current.puzzle.placements.size,
            scoreEarned = scoreEarned + 75,
          )
        } else {
          repository.completeLevel(
            puzzleId = current.puzzle.id,
            stars = stars,
            timeSeconds = current.elapsedSeconds,
            wordsCount = current.puzzle.placements.size,
            scoreEarned = scoreEarned,
            isCuratedLevel = current.isCurated,
            isBlitzClearedInTime = beatBlitzClock,
          )
        }
      }
    } else {
      val clearHint = current.hintedWord == placement.word
      val praise = if (newCombo >= 2) {
        "${newCombo}x Combo! ${placement.word}!"
      } else {
        "${happyWordPraises[newFound.size % happyWordPraises.size]} ${placement.word}"
      }

      _gameState.update {
        it?.copy(
          foundWordKeys = newFound,
          activeTrace = emptyList(),
          activeTraceColorIndex = nextAvailableColorIndex(current.puzzle, newFound),
          hintedCell = if (clearHint) null else it.hintedCell,
          hintedWord = if (clearHint) null else it.hintedWord,
          comboCount = newCombo,
          lastFoundAtMs = now,
          feedback = TraceFeedback(praise, isPositive = true, tone = FeedbackTone.POSITIVE),
        )
      }
      if (current.isCurated) {
        viewModelScope.launch {
          repository.saveInProgress(
            puzzleId = current.puzzle.id,
            foundWords = newFound,
            bonusWords = current.bonusWordsFound,
            elapsedSeconds = current.elapsedSeconds,
            hintsUsed = current.hintsUsed,
          )
        }
      }
    }
  }

  fun undoLastWord() {
    val state = _gameState.value ?: return
    if (state.isCompleted || state.foundWordKeys.isEmpty()) return
    val removedWord = state.foundWordKeys.last()
    val updated = state.foundWordKeys.dropLast(1)
    _gameState.update {
      it?.copy(
        foundWordKeys = updated,
        activeTrace = emptyList(),
        activeTraceColorIndex = nextAvailableColorIndex(state.puzzle, updated),
        comboCount = 0,
        feedback = TraceFeedback("Undid $removedWord", isPositive = false, tone = FeedbackTone.WARNING),
      )
    }
    if (state.isCurated) {
      viewModelScope.launch {
        repository.saveInProgress(
          puzzleId = state.puzzle.id,
          foundWords = updated,
          bonusWords = state.bonusWordsFound,
          elapsedSeconds = state.elapsedSeconds,
          hintsUsed = state.hintsUsed,
        )
      }
    }
  }

  fun resetBoard() {
    val state = _gameState.value ?: return
    _gameState.update {
      it?.copy(
        foundWordKeys = emptyList(),
        activeTrace = emptyList(),
        activeTraceColorIndex = state.puzzle.placements.firstOrNull()?.colorIndex ?: 0,
        hintedCell = null,
        hintedWord = null,
        elapsedSeconds = 0,
        hintsUsed = 0,
        comboCount = 0,
        lastFoundAtMs = 0L,
        isPaused = false,
        isCompleted = false,
        feedback = TraceFeedback("Fresh start — you got this!", isPositive = true, tone = FeedbackTone.POSITIVE),
      )
    }
    startTimer()
    if (state.isCurated) {
      viewModelScope.launch {
        repository.resetInProgress(state.puzzle.id)
      }
    }
  }

  fun useLetterHint() {
    hintWordTarget(null)
  }

  fun hintSpecificWord(word: String) {
    hintWordTarget(word)
  }

  private fun hintWordTarget(preferredWord: String?) {
    val state = _gameState.value ?: return
    if (state.isCompleted || state.isPaused) return
    val target = if (preferredWord != null) {
      state.puzzle.placements.firstOrNull { it.word == preferredWord && it.word !in state.foundWordKeys }
    } else {
      null
    } ?: state.puzzle.placements.firstOrNull { it.word !in state.foundWordKeys } ?: return

    viewModelScope.launch {
      val consumed = repository.consumeHints(1)
      if (!consumed) {
        _gameState.update {
          it?.copy(
            feedback = TraceFeedback(
              "No hints left — find 5 extra words or clear a level!",
              isPositive = false,
              tone = FeedbackTone.WARNING,
            )
          )
        }
        return@launch
      }
      _gameState.update {
        it?.copy(
          hintedCell = target.path.first(),
          hintedWord = target.word,
          hintsUsed = it.hintsUsed + 1,
          feedback = TraceFeedback(
            "Hint: '${target.word.first()}' starts this ${target.word.length}-letter word!",
            isPositive = true,
            tone = FeedbackTone.BONUS,
          ),
        )
      }
    }
  }

  fun useSolveWordHint() {
    val state = _gameState.value ?: return
    if (state.isCompleted || state.isPaused) return
    val target = state.hintedWord?.let { hinted ->
      state.puzzle.placements.firstOrNull { it.word == hinted && it.word !in state.foundWordKeys }
    } ?: state.puzzle.placements.firstOrNull { it.word !in state.foundWordKeys } ?: return

    viewModelScope.launch {
      val consumed = repository.consumeHints(2)
      if (!consumed) {
        _gameState.update {
          it?.copy(
            feedback = TraceFeedback(
              "Need 2 hints to solve a full word — fill your Bucket!",
              isPositive = false,
              tone = FeedbackTone.WARNING,
            )
          )
        }
        return@launch
      }
      _gameState.update { it?.copy(hintsUsed = it.hintsUsed + 2) }
      commitSolvedWord(target)
    }
  }

  fun setPaused(paused: Boolean) {
    _gameState.update { it?.copy(isPaused = paused) }
  }

  fun toggleHaptics() {
    val current = playerStats.value.hapticsEnabled
    viewModelScope.launch {
      repository.setHapticsEnabled(!current)
    }
  }

  private fun nextAvailableColorIndex(puzzle: FillwordPuzzle, foundWords: List<String>): Int {
    val nextPlacement = puzzle.placements.firstOrNull { it.word !in foundWords }
    return nextPlacement?.colorIndex ?: 0
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (true) {
        delay(1_000L)
        val state = _gameState.value ?: break
        if (!state.isPaused && !state.isCompleted) {
          _gameState.update { it?.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        }
      }
    }
  }

  private fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }

  private fun saveCurrentProgressIfActive() {
    val state = _gameState.value ?: return
    if (state.isCurated && !state.isCompleted && (state.foundWordKeys.isNotEmpty() || state.bonusWordsFound.isNotEmpty())) {
      viewModelScope.launch {
        repository.saveInProgress(
          puzzleId = state.puzzle.id,
          foundWords = state.foundWordKeys,
          bonusWords = state.bonusWordsFound,
          elapsedSeconds = state.elapsedSeconds,
          hintsUsed = state.hintsUsed,
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    stopTimer()
  }

  companion object {
    fun todayDateKey(): String {
      val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      return fmt.format(Calendar.getInstance().time)
    }

    fun yesterdayDateKey(): String {
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      return fmt.format(cal.time)
    }

    private val allCuratedWords: Set<String> by lazy {
      CuratedPuzzles.allPuzzles.flatMap { p -> p.placements.map { it.word } }.toSet()
    }
  }
}
