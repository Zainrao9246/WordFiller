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
  val lastDailyCompletedDate: String = "",
  val dailyStreak: Int = 0,
)

@Entity(tableName = "vault_config")
data class VaultConfigEntity(
  @PrimaryKey val id: Int = 1,
  val pinSaltBase64: String,
  val pinHashBase64: String,
  val failedAttempts: Int = 0,
  val lockoutUntilMs: Long = 0L,
  val createdAt: Long = System.currentTimeMillis(),
  val pinLength: Int = 4,
  val blockScreenshots: Boolean = false,
  val wipePinSaltBase64: String = "",
  val wipePinHashBase64: String = "",
)

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
  @PrimaryKey val id: String,
  val mediaType: String, // "PHOTO" or "VIDEO"
  val mimeType: String,
  val encryptedDisplayName: String,
  val encryptedBlobPath: String,
  val encryptedThumbPath: String,
  val sizeBytes: Long = 0L,
  val durationMs: Long = 0L,
  val width: Int = 0,
  val height: Int = 0,
  val originalDeleted: Boolean = false,
  val sourceUriString: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "vault_messages")
data class VaultMessageEntity(
  @PrimaryKey val id: String,
  val encryptedContact: String,
  val encryptedBody: String,
  val accentIndex: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
