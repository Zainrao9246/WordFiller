package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [LevelProgressEntity::class, PlayerStatsEntity::class],
  version = 2,
  exportSchema = false,
)
abstract class FillwordsDatabase : RoomDatabase() {
  abstract fun fillwordsDao(): FillwordsDao

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

    @Volatile
    private var INSTANCE: FillwordsDatabase? = null

    fun getInstance(context: Context): FillwordsDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          FillwordsDatabase::class.java,
          "fillwords_db",
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
