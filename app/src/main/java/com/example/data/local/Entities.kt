package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
  @PrimaryKey val puzzleId: String,
  val isCompleted: Boolean = false,
  val stars: Int = 0,
  val bestTimeSeconds: Int = 0,
  val foundWordsCsv: String = "",
  val bonusWordsCsv: String = "",
  val elapsedSeconds: Int = 0,
  val hintsUsed: Int = 0,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
  @PrimaryKey val id: Int = 1,
  val hintsRemaining: Int = 5,
  val extraWordsBucketCount: Int = 0,
  val totalExtraWordsFound: Int = 0,
  val totalWordsFound: Int = 0,
  val totalScore: Int = 0,
  val currentStreak: Int = 0,
  val blitzClearedCount: Int = 0,
  val hapticsEnabled: Boolean = true,
)
