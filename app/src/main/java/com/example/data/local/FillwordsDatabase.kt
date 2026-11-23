package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    LevelProgressEntity::class,
    PlayerStatsEntity::class,
    VaultConfigEntity::class,
    VaultMediaEntity::class,
    VaultMessageEntity::class,
  ],
  version = 5,
  exportSchema = false,
)
abstract class FillwordsDatabase : RoomDatabase() {
  abstract fun fillwordsDao(): FillwordsDao
  abstract fun vaultDao(): VaultDao

  companion object {
    const val BUCKET_TARGET_WORDS = 5

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE level_progress ADD COLUMN bonusWordsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE player_stats ADD COLUMN extraWordsBucketCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_stats ADD COLUMN totalExtraWordsFound INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_stats ADD COLUMN blitzClearedCount INTEGER NOT NULL DEFAULT 0")
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_stats ADD COLUMN lastDailyCompletedDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE player_stats ADD COLUMN dailyStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS vault_config (
            id INTEGER NOT NULL PRIMARY KEY,
            pinSaltBase64 TEXT NOT NULL,
            pinHashBase64 TEXT NOT NULL,
            failedAttempts INTEGER NOT NULL,
            lockoutUntilMs INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS vault_media (
            id TEXT NOT NULL PRIMARY KEY,
            mediaType TEXT NOT NULL,
            mimeType TEXT NOT NULL,
            encryptedDisplayName TEXT NOT NULL,
            encryptedBlobPath TEXT NOT NULL,
            encryptedThumbPath TEXT NOT NULL,
            sizeBytes INTEGER NOT NULL,
            durationMs INTEGER NOT NULL,
            width INTEGER NOT NULL,
            height INTEGER NOT NULL,
            originalDeleted INTEGER NOT NULL,
            sourceUriString TEXT NOT NULL,
            createdAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS vault_messages (
            id TEXT NOT NULL PRIMARY KEY,
            encryptedContact TEXT NOT NULL,
            encryptedBody TEXT NOT NULL,
            accentIndex INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vault_config ADD COLUMN pinLength INTEGER NOT NULL DEFAULT 4")
        db.execSQL("ALTER TABLE vault_config ADD COLUMN blockScreenshots INTEGER NOT NULL DEFAULT 0")
      }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vault_config ADD COLUMN wipePinSaltBase64 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE vault_config ADD COLUMN wipePinHashBase64 TEXT NOT NULL DEFAULT ''")
      }
    }

    @Volatile
    private var INSTANCE: FillwordsDatabase? = null

    fun getInstance(context: Context): FillwordsDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          FillwordsDatabase::class.java,
          "fillwords_db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .addCallback(object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
              super.onOpen(db)
              runCatching { db.query("PRAGMA secure_delete = ON").close() }
            }
          })
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
