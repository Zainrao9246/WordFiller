package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FillwordsDatabase
import com.example.data.local.VaultMediaEntity
import com.example.data.repository.DecryptedVaultMessage
import com.example.data.repository.PinVerifyResult
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingGalleryDelete(
  val mediaIds: List<String>,
  val uris: List<Uri>,
)

data class PendingSafDelete(
  val items: List<Pair<String, Uri>>,
)

data class VaultUiState(
  val pinModalOpen: Boolean = false,
  val isSetupMode: Boolean = true,
  val isUnlocked: Boolean = false,
  val unlockedViaRecovery: Boolean = false,
  val blockScreenshots: Boolean = false,
  val pinError: String? = null,
  val pinErrorNonce: Long = 0L,
  val lockoutSeconds: Int = 0,
  val isImporting: Boolean = false,
  val statusMessage: String? = null,
  val pendingGalleryDelete: PendingGalleryDelete? = null,
  val pendingSafDelete: PendingSafDelete? = null,
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: VaultRepository by lazy {
    val vaultDao = FillwordsDatabase.getInstance(application).vaultDao()
    VaultRepository(vaultDao)
  }

  val hasPinConfigured: StateFlow<Boolean> = repository.hasPinConfigured.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = false,
  )

  val hasCustomDestructPin: StateFlow<Boolean> = repository.hasCustomDestructPin.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = false,
  )

  val blockScreenshots: StateFlow<Boolean> = repository.vaultConfig
    .let { flow ->
      kotlinx.coroutines.flow.MutableStateFlow(false).also { target ->
        viewModelScope.launch {
          flow.collect { cfg ->
            val enabled = cfg?.blockScreenshots ?: false
            target.value = enabled
            _uiState.update { it.copy(blockScreenshots = enabled) }
          }
        }
      }
    }

  val mediaItems: StateFlow<List<VaultMediaEntity>> = repository.allMedia.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = emptyList(),
  )

  val messages: StateFlow<List<DecryptedVaultMessage>> = repository.allMessages.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = emptyList(),
  )

  private val _uiState = MutableStateFlow(VaultUiState())
  val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

  private var bulbTapCount = 0
  private var lastBulbTapAtMs = 0L
  private var suppressAutoLock = false
  private var lockoutJob: Job? = null

  fun onTopHintBulbTapped() {
    val now = System.currentTimeMillis()
    if (now - lastBulbTapAtMs > 3_500L) {
      bulbTapCount = 0
    }
    lastBulbTapAtMs = now
    bulbTapCount += 1
    if (bulbTapCount >= 3) {
      bulbTapCount = 0
      lastBulbTapAtMs = 0L
      openPinPrompt()
    }
  }

  fun resetBulbTapSequence() {
    bulbTapCount = 0
    lastBulbTapAtMs = 0L
  }

  fun openPinPrompt() {
    repository.resetRecoveryAttempts()
    viewModelScope.launch {
      val pinSet = repository.isPinSet()
      _uiState.update {
        it.copy(
          pinModalOpen = true,
          isSetupMode = !pinSet,
          pinError = null,
        )
      }
    }
  }

  fun dismissPinPrompt() {
    repository.resetRecoveryAttempts()
    _uiState.update {
      it.copy(
        pinModalOpen = false,
        pinError = null,
      )
    }
  }

  fun setupNewPin(pin: String, onUnlocked: () -> Unit) {
    if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
      _uiState.update {
        it.copy(pinError = "Choose a 4 to 6 digit PIN", pinErrorNonce = System.nanoTime())
      }
      return
    }
    viewModelScope.launch {
      if (repository.isDestructPin(pin)) {
        _uiState.update {
          it.copy(
            pinError = "Reserved for Self-Destruct — choose another PIN",
            pinErrorNonce = System.nanoTime(),
          )
        }
        return@launch
      }
      repository.setupPin(pin)
      _uiState.update {
        it.copy(
          pinModalOpen = false,
          isSetupMode = false,
          isUnlocked = true,
          unlockedViaRecovery = false,
          pinError = null,
          statusMessage = "Vault PIN set & unlocked",
        )
      }
      onUnlocked()
    }
  }

  fun clearPinError() {
    if (_uiState.value.pinError != null) {
      _uiState.update { it.copy(pinError = null) }
    }
  }

  fun submitUnlockPin(pin: String, onUnlocked: () -> Unit) {
    _uiState.update { it.copy(pinError = null) }
    viewModelScope.launch {
      when (val result = repository.verifyPin(pin)) {
        is PinVerifyResult.Success -> {
          lockoutJob?.cancel()
          _uiState.update {
            it.copy(
              pinModalOpen = false,
              isUnlocked = true,
              unlockedViaRecovery = result.viaRecovery,
              pinError = null,
              lockoutSeconds = 0,
              statusMessage = if (result.viaRecovery) {
                "Recovered access — open Security tab to set a new PIN"
              } else {
                null
              },
            )
          }
          onUnlocked()
        }
        is PinVerifyResult.SelfDestruct -> {
          lockoutJob?.cancel()
          repository.zeroTraceWipeAllVaultData(getApplication())
          _uiState.update {
            it.copy(
              pinModalOpen = false,
              isSetupMode = true,
              isUnlocked = true,
              unlockedViaRecovery = false,
              pinError = null,
              lockoutSeconds = 0,
              statusMessage = null,
              pendingGalleryDelete = null,
              pendingSafDelete = null,
            )
          }
          onUnlocked()
        }
        is PinVerifyResult.Invalid -> {
          val msg = if (result.recoveryHintAttempt > 0) {
            "Incorrect PIN"
          } else {
            "Incorrect PIN (${result.remainingBeforeLockout} left)"
          }
          _uiState.update {
            it.copy(pinError = msg, pinErrorNonce = System.nanoTime())
          }
        }
        is PinVerifyResult.LockedOut -> {
          startLockoutCountdown(result.remainingSeconds)
        }
      }
    }
  }

  private fun startLockoutCountdown(seconds: Int) {
    lockoutJob?.cancel()
    lockoutJob = viewModelScope.launch {
      var remaining = seconds
      while (remaining > 0) {
        _uiState.update {
          it.copy(
            lockoutSeconds = remaining,
            pinError = "Too many attempts. Wait ${remaining}s",
            pinErrorNonce = System.nanoTime(),
          )
        }
        delay(1_000L)
        remaining -= 1
      }
      _uiState.update { it.copy(lockoutSeconds = 0, pinError = null) }
    }
  }

  fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
    if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
      onResult(false, "New PIN must be 4–6 digits")
      return
    }
    viewModelScope.launch {
      if (repository.isDestructPin(newPin)) {
        onResult(false, "Reserved for Self-Destruct — choose another PIN")
        return@launch
      }
      if (_uiState.value.unlockedViaRecovery || !repository.isPinSet()) {
        repository.resetPinFromUnlockedRecovery(newPin)
        _uiState.update {
          it.copy(
            isSetupMode = false,
            unlockedViaRecovery = false,
            statusMessage = "New Vault PIN saved",
          )
        }
        onResult(true, "New Vault PIN saved")
      } else {
        val changed = repository.changePin(oldPin, newPin)
        if (changed) {
          _uiState.update { it.copy(statusMessage = "Vault PIN updated") }
          onResult(true, "Vault PIN updated")
        } else {
          onResult(false, "Current PIN did not match")
        }
      }
    }
  }

  fun changeDestructPin(oldPin: String, newDestructPin: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val (ok, msg) = repository.setCustomDestructPin(
        oldUnlockPin = oldPin,
        newDestructPin = newDestructPin,
        skipOldPinCheck = _uiState.value.unlockedViaRecovery,
      )
      if (ok) {
        _uiState.update { it.copy(statusMessage = msg) }
      }
      onResult(ok, msg)
    }
  }

  fun resetDestructPinToDefault(oldPin: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val (ok, msg) = repository.resetDestructPinToDefault(
        oldUnlockPin = oldPin,
        skipOldPinCheck = _uiState.value.unlockedViaRecovery,
      )
      if (ok) {
        _uiState.update { it.copy(statusMessage = msg) }
      }
      onResult(ok, msg)
    }
  }

  fun toggleBlockScreenshots(enabled: Boolean) {
    viewModelScope.launch {
      repository.setBlockScreenshots(enabled)
      _uiState.update {
        it.copy(
          blockScreenshots = enabled,
          statusMessage = if (enabled) {
            "Screen capture block enabled"
          } else {
            "Screen capture block turned off"
          },
        )
      }
    }
  }

  fun beginExternalSystemAction() {
    suppressAutoLock = true
  }

  fun endExternalSystemAction() {
    suppressAutoLock = false
  }

  fun shouldAutoLockOnStop(): Boolean = !suppressAutoLock

  fun lockVault() {
    suppressAutoLock = false
    repository.resetRecoveryAttempts()
    _uiState.update {
      it.copy(
        isUnlocked = false,
        unlockedViaRecovery = false,
        pinModalOpen = false,
        pinError = null,
        pendingGalleryDelete = null,
        pendingSafDelete = null,
      )
    }
  }

  fun importMediaFromPicker(uris: List<Uri>) {
    if (uris.isEmpty()) {
      endExternalSystemAction()
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isImporting = true, statusMessage = null) }
      val outcome = repository.importAndSaveMedia(getApplication(), uris)
      if (outcome.importedItems.isEmpty()) {
        endExternalSystemAction()
        _uiState.update {
          it.copy(
            isImporting = false,
            statusMessage = "Could not import selected items",
          )
        }
        return@launch
      }

      val mediaStoreItems = outcome.mediaStoreDeleteItems
      val safDocItems = outcome.safDocumentDeleteItems
      if (mediaStoreItems.isNotEmpty() || safDocItems.isNotEmpty()) {
        if (mediaStoreItems.isEmpty()) {
          endExternalSystemAction()
        }
        _uiState.update {
          it.copy(
            isImporting = false,
            pendingGalleryDelete = if (mediaStoreItems.isNotEmpty()) {
              PendingGalleryDelete(
                mediaIds = mediaStoreItems.map { p -> p.first }.distinct(),
                uris = mediaStoreItems.map { p -> p.second }.distinct(),
              )
            } else {
              null
            },
            pendingSafDelete = if (safDocItems.isNotEmpty()) {
              PendingSafDelete(safDocItems)
            } else {
              null
            },
          )
        }
      } else {
        endExternalSystemAction()
        _uiState.update {
          it.copy(
            isImporting = false,
            statusMessage = "Encrypted ${outcome.importedItems.size} item(s) inside Vault",
          )
        }
      }
    }
  }

  fun onOriginalDeleteConfirmationResult(deleted: Boolean) {
    val pending = _uiState.value.pendingGalleryDelete
    endExternalSystemAction()
    if (pending == null) return

    viewModelScope.launch {
      if (deleted) {
        repository.markOriginalsDeleted(pending.mediaIds)
        _uiState.update {
          it.copy(
            pendingGalleryDelete = null,
            statusMessage = "Moved ${pending.mediaIds.size} item(s) to Vault — original removed",
          )
        }
      } else {
        _uiState.update {
          it.copy(
            pendingGalleryDelete = null,
            statusMessage = "Saved in Vault (original copy kept)",
          )
        }
      }
    }
  }

  fun confirmSafDocumentOriginalsDelete() {
    val pending = _uiState.value.pendingSafDelete ?: return
    viewModelScope.launch {
      val uris = pending.items.map { it.second }
      val deletedUris = com.example.vault.VaultStorageManager.deleteSafDocumentOriginals(getApplication(), uris)
      val deletedSet = deletedUris.toSet()
      val deletedIds = pending.items.filter { it.second in deletedSet }.map { it.first }.distinct()
      if (deletedIds.isNotEmpty()) {
        repository.markOriginalsDeleted(deletedIds)
      }
      _uiState.update {
        it.copy(
          pendingSafDelete = null,
          statusMessage = if (deletedIds.isNotEmpty()) {
            "Moved ${deletedIds.size} file(s) to Vault — original removed"
          } else {
            "Saved in Vault (source folder did not allow auto-delete)"
          },
        )
      }
    }
  }

  fun dismissSafDocumentOriginalsDelete() {
    _uiState.update {
      it.copy(
        pendingSafDelete = null,
        statusMessage = "Saved in Vault (original file kept)",
      )
    }
  }

  fun clearPendingGalleryDelete() {
    endExternalSystemAction()
    _uiState.update { it.copy(pendingGalleryDelete = null) }
  }

  fun zeroTraceDeleteMedia(entity: VaultMediaEntity) {
    viewModelScope.launch {
      val wiped = repository.zeroTraceDeleteMedia(entity)
      _uiState.update {
        it.copy(
          statusMessage = if (wiped) {
            "Permanently wiped with zero trace"
          } else {
            "Removed from Vault"
          },
        )
      }
    }
  }

  fun saveMessage(existingId: String?, contact: String, body: String, accentIndex: Int) {
    viewModelScope.launch {
      repository.saveMessage(existingId, contact, body, accentIndex)
      _uiState.update { it.copy(statusMessage = "Encrypted message saved") }
    }
  }

  fun zeroTraceDeleteMessage(id: String) {
    viewModelScope.launch {
      repository.zeroTraceDeleteMessage(id)
      _uiState.update { it.copy(statusMessage = "Message wiped with zero trace") }
    }
  }

  fun zeroTraceWipeEverything(onLockedOut: () -> Unit) {
    viewModelScope.launch {
      repository.zeroTraceWipeAllVaultData(getApplication())
      lockVault()
      onLockedOut()
    }
  }

  fun consumeStatusMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    lockoutJob?.cancel()
  }
}
