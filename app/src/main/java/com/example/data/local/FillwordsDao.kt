package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FillwordsDao {
  @Query("SELECT * FROM level_progress ORDER BY updatedAt DESC")
  fun observeAllProgress(): Flow<List<LevelProgressEntity>>

  @Query("SELECT * FROM level_progress WHERE puzzleId = :puzzleId LIMIT 1")
  suspend fun getProgress(puzzleId: String): LevelProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProgress(entity: LevelProgressEntity)

  @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
  fun observePlayerStats(): Flow<PlayerStatsEntity?>

  @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
  suspend fun getPlayerStats(): PlayerStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlayerStats(stats: PlayerStatsEntity)
}
