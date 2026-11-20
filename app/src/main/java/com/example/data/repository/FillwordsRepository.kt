package com.example.data.repository

import com.example.data.local.FillwordsDao
import com.example.data.local.FillwordsDatabase
import com.example.data.local.LevelProgressEntity
import com.example.data.local.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BucketRewardResult(
  val newBucketCount: Int,
  val earnedHint: Boolean,
  val totalExtraWords: Int,
)

class FillwordsRepository(private val dao: FillwordsDao) {

  val allProgress: Flow<Map<String, LevelProgressEntity>> =
    dao.observeAllProgress().map { list -> list.associateBy { it.puzzleId } }

  val playerStats: Flow<PlayerStatsEntity> =
    dao.observePlayerStats().map { it ?: PlayerStatsEntity() }

  suspend fun ensureInitialStats() {
    if (dao.getPlayerStats() == null) {
      dao.upsertPlayerStats(PlayerStatsEntity())
    }
  }

  suspend fun saveInProgress(
    puzzleId: String,
    foundWords: List<String>,
    bonusWords: List<String>,
    elapsedSeconds: Int,
    hintsUsed: Int,
  ) {
    val existing = dao.getProgress(puzzleId)
    dao.upsertProgress(
      LevelProgressEntity(
        puzzleId = puzzleId,
        isCompleted = existing?.isCompleted ?: false,
        stars = existing?.stars ?: 0,
        bestTimeSeconds = existing?.bestTimeSeconds ?: 0,
        foundWordsCsv = foundWords.joinToString(","),
        bonusWordsCsv = bonusWords.joinToString(","),
        elapsedSeconds = elapsedSeconds,
        hintsUsed = hintsUsed,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun recordExtraWord(
    puzzleId: String,
    isCuratedLevel: Boolean,
    foundWords: List<String>,
    bonusWords: List<String>,
    elapsedSeconds: Int,
    hintsUsed: Int,
  ): BucketRewardResult {
    if (isCuratedLevel) {
      saveInProgress(
        puzzleId = puzzleId,
        foundWords = foundWords,
        bonusWords = bonusWords,
        elapsedSeconds = elapsedSeconds,
        hintsUsed = hintsUsed,
      )
    }

    val stats = dao.getPlayerStats() ?: PlayerStatsEntity()
    val nextBucket = stats.extraWordsBucketCount + 1
    val bucketFilled = nextBucket >= FillwordsDatabase.BUCKET_TARGET_WORDS
    val updatedBucket = if (bucketFilled) 0 else nextBucket
    val updatedHints = if (bucketFilled) stats.hintsRemaining + 1 else stats.hintsRemaining
    val updatedTotalExtra = stats.totalExtraWordsFound + 1

    dao.upsertPlayerStats(
      stats.copy(
        hintsRemaining = updatedHints,
        extraWordsBucketCount = updatedBucket,
        totalExtraWordsFound = updatedTotalExtra,
        totalScore = stats.totalScore + 15,
      )
    )

    return BucketRewardResult(
      newBucketCount = updatedBucket,
      earnedHint = bucketFilled,
      totalExtraWords = updatedTotalExtra,
    )
  }

  suspend fun completeLevel(
    puzzleId: String,
    stars: Int,
    timeSeconds: Int,
    wordsCount: Int,
    scoreEarned: Int,
    isCuratedLevel: Boolean,
    isBlitzClearedInTime: Boolean,
  ) {
    if (isCuratedLevel) {
      val existing = dao.getProgress(puzzleId)
      val bestStars = maxOf(existing?.stars ?: 0, stars)
      val bestTime = if (existing != null && existing.bestTimeSeconds > 0) {
        minOf(existing.bestTimeSeconds, timeSeconds)
      } else {
        timeSeconds
      }
      dao.upsertProgress(
        LevelProgressEntity(
          puzzleId = puzzleId,
          isCompleted = true,
          stars = bestStars,
          bestTimeSeconds = bestTime,
          foundWordsCsv = "",
          bonusWordsCsv = "",
          elapsedSeconds = 0,
          hintsUsed = 0,
          updatedAt = System.currentTimeMillis(),
        )
      )
    }

    val stats = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(
      stats.copy(
        hintsRemaining = stats.hintsRemaining + 1,
        totalWordsFound = stats.totalWordsFound + wordsCount,
        totalScore = stats.totalScore + scoreEarned,
        currentStreak = stats.currentStreak + 1,
        blitzClearedCount = stats.blitzClearedCount + if (isBlitzClearedInTime) 1 else 0,
      )
    )
  }

  suspend fun resetInProgress(puzzleId: String) {
    val existing = dao.getProgress(puzzleId)
    if (existing != null) {
      dao.upsertProgress(
        existing.copy(
          foundWordsCsv = "",
          bonusWordsCsv = "",
          elapsedSeconds = 0,
          hintsUsed = 0,
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun consumeHints(count: Int): Boolean {
    val stats = dao.getPlayerStats() ?: PlayerStatsEntity()
    if (stats.hintsRemaining < count) return false
    dao.upsertPlayerStats(stats.copy(hintsRemaining = stats.hintsRemaining - count))
    return true
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    val stats = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(stats.copy(hapticsEnabled = enabled))
  }
}
