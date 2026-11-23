package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
  @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
  fun observeConfig(): Flow<VaultConfigEntity?>

  @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
  suspend fun getConfig(): VaultConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConfig(config: VaultConfigEntity)

  @Query("DELETE FROM vault_config")
  suspend fun deleteConfig()

  @Query("SELECT * FROM vault_media ORDER BY createdAt DESC")
  fun observeAllMedia(): Flow<List<VaultMediaEntity>>

  @Query("SELECT * FROM vault_media")
  suspend fun getAllMediaSnapshot(): List<VaultMediaEntity>

  @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
  suspend fun getMediaById(id: String): VaultMediaEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMedia(items: List<VaultMediaEntity>)

  @Query("UPDATE vault_media SET originalDeleted = 1, sourceUriString = '' WHERE id IN (:ids)")
  suspend fun markOriginalsDeleted(ids: List<String>)

  @Query("DELETE FROM vault_media WHERE id = :id")
  suspend fun deleteMediaById(id: String)

  @Query("DELETE FROM vault_media")
  suspend fun deleteAllMedia()

  @Query("SELECT * FROM vault_messages ORDER BY updatedAt DESC")
  fun observeAllMessages(): Flow<List<VaultMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessage(message: VaultMessageEntity)

  @Query("DELETE FROM vault_messages WHERE id = :id")
  suspend fun deleteMessageById(id: String)

  @Query("DELETE FROM vault_messages")
  suspend fun deleteAllMessages()
}
