package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.VaultConfigEntity
import com.example.data.local.VaultDao
import com.example.data.local.VaultMediaEntity
import com.example.data.local.VaultMessageEntity
import com.example.vault.VaultCrypto
import com.example.vault.VaultImportOutcome
import com.example.vault.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed interface PinVerifyResult {
  data class Success(val viaRecovery: Boolean = false) : PinVerifyResult
  data object SelfDestruct : PinVerifyResult
  data class Invalid(val remainingBeforeLockout: Int, val recoveryHintAttempt: Int = 0) : PinVerifyResult
  data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
}

data class DecryptedVaultMessage(
  val id: String,
  val contact: String,
  val body: String,
  val accentIndex: Int,
  val createdAt: Long,
  val updatedAt: Long,
)

class VaultRepository(private val vaultDao: VaultDao) {

  val hasPinConfigured: Flow<Boolean> = vaultDao.observeConfig().map { it != null }
  val vaultConfig: Flow<VaultConfigEntity?> = vaultDao.observeConfig()
  val hasCustomDestructPin: Flow<Boolean> = vaultDao.observeConfig().map {
    it?.wipePinHashBase64?.isNotBlank() == true
  }
  val allMedia: Flow<List<VaultMediaEntity>> = vaultDao.observeAllMedia()

  @Volatile
  private var consecutiveZeroRecoveryCount = 0

  @Volatile
  private var lastRecoveryCode: String? = null

  @Volatile
  private var consecutiveDestructCount = 0

  val allMessages: Flow<List<DecryptedVaultMessage>> =
    vaultDao.observeAllMessages().map { entities ->
      entities.map { entity ->
        DecryptedVaultMessage(
          id = entity.id,
          contact = runCatching { VaultCrypto.decryptString(entity.encryptedContact) }
            .getOrDefault("Private Note"),
          body = runCatching { VaultCrypto.decryptString(entity.encryptedBody) }
            .getOrDefault(""),
          accentIndex = entity.accentIndex,
          createdAt = entity.createdAt,
          updatedAt = entity.updatedAt,
        )
      }
    }

  suspend fun isPinSet(): Boolean = vaultDao.getConfig() != null

  fun resetRecoveryAttempts() {
    consecutiveZeroRecoveryCount = 0
    lastRecoveryCode = null
    consecutiveDestructCount = 0
  }

  private fun matchesDestructPin(pin: String, config: VaultConfigEntity?): Boolean {
    if (config != null && config.wipePinHashBase64.isNotBlank() && config.wipePinSaltBase64.isNotBlank()) {
      return VaultCrypto.verifyPin(pin, config.wipePinSaltBase64, config.wipePinHashBase64)
    }
    return MessageDigest.isEqual(
      pin.toByteArray(Charsets.UTF_8),
      DEFAULT_DESTRUCT_PIN.toByteArray(Charsets.UTF_8),
    )
  }

  suspend fun isDestructPin(pin: String): Boolean = withContext(Dispatchers.IO) {
    matchesDestructPin(pin, vaultDao.getConfig())
  }

  suspend fun setupPin(pin: String) = withContext(Dispatchers.IO) {
    require(pin.length in 4..6 && pin.all { it.isDigit() }) { "PIN must be 4–6 digits" }
    val existing = vaultDao.getConfig()
    val salt = VaultCrypto.generateSaltBase64()
    val hash = VaultCrypto.hashPinBase64(pin, salt)
    resetRecoveryAttempts()
    vaultDao.upsertConfig(
      VaultConfigEntity(
        id = 1,
        pinSaltBase64 = salt,
        pinHashBase64 = hash,
        failedAttempts = 0,
        lockoutUntilMs = 0L,
        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        pinLength = pin.length,
        blockScreenshots = existing?.blockScreenshots ?: false,
        wipePinSaltBase64 = existing?.wipePinSaltBase64 ?: "",
        wipePinHashBase64 = existing?.wipePinHashBase64 ?: "",
      )
    )
  }

  suspend fun verifyPin(pin: String): PinVerifyResult = withContext(Dispatchers.IO) {
    val config = vaultDao.getConfig() ?: return@withContext PinVerifyResult.Invalid(MAX_ATTEMPTS)
    val now = System.currentTimeMillis()
    if (config.lockoutUntilMs > now) {
      val rem = (((config.lockoutUntilMs - now) + 999L) / 1000L).toInt().coerceAtLeast(1)
      return@withContext PinVerifyResult.LockedOut(rem)
    }

    // 1. Check user's configured PIN first
    if (VaultCrypto.verifyPin(pin, config.pinSaltBase64, config.pinHashBase64)) {
      resetRecoveryAttempts()
      if (config.failedAttempts != 0 || config.lockoutUntilMs != 0L) {
        vaultDao.upsertConfig(config.copy(failedAttempts = 0, lockoutUntilMs = 0L))
      }
      return@withContext PinVerifyResult.Success(viaRecovery = false)
    }

    // 2. Check destructive self-destruct password (hard 6-digit code entered 2 consecutive times).
    //    First attempt returns "Incorrect PIN" quietly without tripping lockout;
    //    Second consecutive attempt triggers immediate full zero-trace Vault destruction.
    if (matchesDestructPin(pin, config)) {
      consecutiveZeroRecoveryCount = 0
      lastRecoveryCode = null
      consecutiveDestructCount += 1
      if (consecutiveDestructCount >= DESTRUCT_REPEAT_COUNT) {
        resetRecoveryAttempts()
        return@withContext PinVerifyResult.SelfDestruct
      }
      return@withContext PinVerifyResult.Invalid(
        remainingBeforeLockout = (MAX_ATTEMPTS - config.failedAttempts).coerceAtLeast(1),
        recoveryHintAttempt = consecutiveDestructCount,
      )
    }

    // 3. Check master forgot-PIN sequence: 0000 (or 000000 for 6-digit PIN) entered 3 times in a row.
    //    First & second times intentionally report wrong PIN; third consecutive attempt unlocks.
    val configuredZeros = "0".repeat(config.pinLength.coerceIn(4, 6))
    val isRecoveryCode = pin == configuredZeros || pin == RECOVERY_PIN_4 || pin == RECOVERY_PIN_6
    if (isRecoveryCode) {
      consecutiveDestructCount = 0
      if (lastRecoveryCode == pin) {
        consecutiveZeroRecoveryCount += 1
      } else {
        lastRecoveryCode = pin
        consecutiveZeroRecoveryCount = 1
      }
      if (consecutiveZeroRecoveryCount >= RECOVERY_REPEAT_COUNT) {
        resetRecoveryAttempts()
        vaultDao.upsertConfig(config.copy(failedAttempts = 0, lockoutUntilMs = 0L))
        return@withContext PinVerifyResult.Success(viaRecovery = true)
      }
      val attempt = consecutiveZeroRecoveryCount
      return@withContext PinVerifyResult.Invalid(
        remainingBeforeLockout = (MAX_ATTEMPTS - config.failedAttempts).coerceAtLeast(1),
        recoveryHintAttempt = attempt,
      )
    }

    // 4. Regular wrong PIN attempt
    resetRecoveryAttempts()
    val nextFailures = config.failedAttempts + 1
    if (nextFailures >= MAX_ATTEMPTS) {
      val lockoutUntil = now + LOCKOUT_DURATION_MS
      vaultDao.upsertConfig(
        config.copy(
          failedAttempts = 0,
          lockoutUntilMs = lockoutUntil,
        )
      )
      PinVerifyResult.LockedOut((LOCKOUT_DURATION_MS / 1000L).toInt())
    } else {
      vaultDao.upsertConfig(config.copy(failedAttempts = nextFailures))
      PinVerifyResult.Invalid(MAX_ATTEMPTS - nextFailures)
    }
  }

  suspend fun changePin(oldPin: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
    val config = vaultDao.getConfig()
    if (config == null) {
      setupPin(newPin)
      return@withContext true
    }
    if (!VaultCrypto.verifyPin(oldPin, config.pinSaltBase64, config.pinHashBase64)) {
      return@withContext false
    }
    setupPin(newPin)
    true
  }

  suspend fun resetPinFromUnlockedRecovery(newPin: String) = withContext(Dispatchers.IO) {
    setupPin(newPin)
  }

  suspend fun setCustomDestructPin(
    oldUnlockPin: String,
    newDestructPin: String,
    skipOldPinCheck: Boolean,
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val config = vaultDao.getConfig() ?: return@withContext (false to "Set a Vault PIN first")
    if (!skipOldPinCheck && !VaultCrypto.verifyPin(oldUnlockPin, config.pinSaltBase64, config.pinHashBase64)) {
      return@withContext (false to "Current Vault PIN did not match")
    }
    val validationError = validateHardDestructPin(newDestructPin)
    if (validationError != null) {
      return@withContext (false to validationError)
    }
    if (VaultCrypto.verifyPin(newDestructPin, config.pinSaltBase64, config.pinHashBase64)) {
      return@withContext (false to "Self-Destruct PIN must differ from your Vault PIN")
    }
    val wipeSalt = VaultCrypto.generateSaltBase64()
    val wipeHash = VaultCrypto.hashPinBase64(newDestructPin, wipeSalt)
    vaultDao.upsertConfig(
      config.copy(
        wipePinSaltBase64 = wipeSalt,
        wipePinHashBase64 = wipeHash,
      )
    )
    true to "Custom Self-Destruct PIN saved"
  }

  suspend fun resetDestructPinToDefault(
    oldUnlockPin: String,
    skipOldPinCheck: Boolean,
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val config = vaultDao.getConfig() ?: return@withContext (true to "Default Self-Destruct PIN active")
    if (!skipOldPinCheck && !VaultCrypto.verifyPin(oldUnlockPin, config.pinSaltBase64, config.pinHashBase64)) {
      return@withContext (false to "Current Vault PIN did not match")
    }
    vaultDao.upsertConfig(config.copy(wipePinSaltBase64 = "", wipePinHashBase64 = ""))
    true to "Self-Destruct PIN reset to default"
  }

  suspend fun setBlockScreenshots(enabled: Boolean) = withContext(Dispatchers.IO) {
    val config = vaultDao.getConfig() ?: return@withContext
    vaultDao.upsertConfig(config.copy(blockScreenshots = enabled))
  }

  suspend fun importAndSaveMedia(context: Context, uris: List<Uri>): VaultImportOutcome {
    val outcome = VaultStorageManager.importMediaUris(context, uris)
    if (outcome.importedItems.isNotEmpty()) {
      vaultDao.upsertMedia(outcome.importedItems)
    }
    return outcome
  }

  suspend fun markOriginalsDeleted(mediaIds: List<String>) {
    if (mediaIds.isNotEmpty()) {
      vaultDao.markOriginalsDeleted(mediaIds)
    }
  }

  suspend fun zeroTraceDeleteMedia(entity: VaultMediaEntity): Boolean {
    val wiped = VaultStorageManager.zeroTraceDeleteFiles(entity)
    vaultDao.deleteMediaById(entity.id)
    return wiped
  }

  suspend fun saveMessage(
    existingId: String?,
    contact: String,
    body: String,
    accentIndex: Int,
  ) = withContext(Dispatchers.IO) {
    val cleanContact = contact.trim().ifEmpty { "Private Note" }
    val cleanBody = body.trim()
    if (cleanBody.isEmpty()) return@withContext
    val now = System.currentTimeMillis()
    val entity = VaultMessageEntity(
      id = existingId ?: UUID.randomUUID().toString(),
      encryptedContact = VaultCrypto.encryptString(cleanContact),
      encryptedBody = VaultCrypto.encryptString(cleanBody),
      accentIndex = accentIndex,
      createdAt = now,
      updatedAt = now,
    )
    vaultDao.upsertMessage(entity)
  }

  suspend fun zeroTraceDeleteMessage(id: String) {
    vaultDao.deleteMessageById(id)
  }

  suspend fun zeroTraceWipeAllVaultData(context: Context) {
    resetRecoveryAttempts()
    val allItems = vaultDao.getAllMediaSnapshot()
    allItems.forEach { VaultStorageManager.zeroTraceDeleteFiles(it) }
    VaultStorageManager.zeroTraceWipeAllDirectories(context)
    vaultDao.deleteAllMedia()
    vaultDao.deleteAllMessages()
    vaultDao.deleteConfig()
  }

  companion object {
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L
    const val RECOVERY_PIN_4 = "0000"
    const val RECOVERY_PIN_6 = "000000"
    const val RECOVERY_REPEAT_COUNT = 3
    const val DEFAULT_DESTRUCT_PIN = "937184"
    const val DESTRUCT_REPEAT_COUNT = 2

    private val BLOCKED_SEQUENCES = setOf(
      "012345", "123456", "234567", "345678", "456789",
      "987654", "876543", "765432", "654321", "543210",
      "123123", "112233", "121212", "010101", "258025", "147258",
    )

    fun validateHardDestructPin(pin: String): String? {
      if (pin.length != 6 || !pin.all { it.isDigit() }) {
        return "Self-Destruct PIN must be 6 hard digits (distinct from 4-digit PINs)"
      }
      if (pin == RECOVERY_PIN_6) {
        return "Cannot reuse the recovery code"
      }
      if (pin in BLOCKED_SEQUENCES) {
        return "Too predictable — avoid sequences or repeating patterns"
      }
      if (pin.toSet().size < 4) {
        return "Use at least 4 different digits for a hard code"
      }
      val adjacentSteps = pin.zipWithNext().count { (a, b) -> kotlin.math.abs(a - b) == 1 }
      if (adjacentSteps >= 4) {
        return "Too sequential — choose non-sequential digits"
      }
      return null
    }
  }
}
