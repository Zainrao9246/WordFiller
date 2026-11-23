package com.example.ui.screens

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vault.VaultCrypto
import com.example.data.local.VaultMediaEntity
import com.example.data.repository.DecryptedVaultMessage
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette
import com.example.ui.viewmodel.VaultUiState
import com.example.vault.VaultStorageManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class VaultTab(val label: String, val icon: ImageVector) {
  MEDIA("Files", Icons.Filled.FolderOpen),
  MESSAGES("Messages", Icons.Filled.ChatBubbleOutline),
  SECURITY("Security", Icons.Filled.Security),
}

private enum class MediaFilter(val label: String) {
  ALL("All"),
  PHOTO("Photos"),
  VIDEO("Videos"),
  AUDIO("Audio"),
  DOCUMENT("Docs"),
  OTHER("Other"),
}

private enum class VaultFolder(
  val title: String,
  val subtitle: String,
  val mediaType: String?,
  val icon: ImageVector,
  val accent: Color,
) {
  PHOTOS("Photos", "Encrypted pictures", VaultStorageManager.TYPE_PHOTO, Icons.Filled.PhotoLibrary, AccentEmerald),
  VIDEOS("Videos", "Encrypted video clips", VaultStorageManager.TYPE_VIDEO, Icons.Filled.Videocam, AccentCoral),
  AUDIO("Audio & Voice", "Music & voice notes", VaultStorageManager.TYPE_AUDIO, Icons.Filled.AudioFile, AccentSky),
  DOCUMENTS("Documents", "PDF, text & office docs", VaultStorageManager.TYPE_DOCUMENT, Icons.Filled.Description, AccentGold),
  OTHER("Other Files", "Archives & private files", VaultStorageManager.TYPE_OTHER, Icons.Filled.InsertDriveFile, WordTilePalette[6]),
  ALL("All Files", "Browse every vault item", null, Icons.Filled.FolderOpen, AccentSky),
}

@Composable
fun VaultScreen(
  uiState: VaultUiState,
  mediaItems: List<VaultMediaEntity>,
  messages: List<DecryptedVaultMessage>,
  hasPinConfigured: Boolean,
  hasCustomDestructPin: Boolean,
  onBeginSystemAction: () -> Unit,
  onEndSystemAction: () -> Unit,
  onImportMediaUris: (List<Uri>) -> Unit,
  onDeleteConfirmationResult: (Boolean) -> Unit,
  onClearPendingDelete: () -> Unit,
  onConfirmSafDelete: () -> Unit,
  onDismissSafDelete: () -> Unit,
  onZeroTraceDeleteMedia: (VaultMediaEntity) -> Unit,
  onSaveMessage: (id: String?, contact: String, body: String, accentIndex: Int) -> Unit,
  onZeroTraceDeleteMessage: (String) -> Unit,
  onChangePin: (oldPin: String, newPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
  onChangeDestructPin: (oldPin: String, newDestructPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
  onResetDestructPin: (oldPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
  onToggleBlockScreenshots: (Boolean) -> Unit,
  onWipeAllVault: () -> Unit,
  onConsumeStatus: () -> Unit,
  onLockAndExit: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var activeTab by remember { mutableStateOf(VaultTab.MEDIA) }
  var activeFolder by remember { mutableStateOf<VaultFolder?>(null) }
  var mediaFilter by remember { mutableStateOf(MediaFilter.ALL) }
  var activeViewerItem by remember { mutableStateOf<VaultMediaEntity?>(null) }
  var pendingWipeItem by remember { mutableStateOf<VaultMediaEntity?>(null) }
  var editingMessage by remember { mutableStateOf<DecryptedVaultMessage?>(null) }
  var showAddChooserDialog by remember { mutableStateOf(false) }
  var showMessageComposer by remember { mutableStateOf(false) }
  var showChangePinDialog by remember { mutableStateOf(false) }
  var showChangeDestructPinDialog by remember { mutableStateOf(false) }
  var showWipeAllDialog by remember { mutableStateOf(false) }

  BackHandler {
    when {
      activeViewerItem != null -> activeViewerItem = null
      pendingWipeItem != null -> pendingWipeItem = null
      showAddChooserDialog -> showAddChooserDialog = false
      showMessageComposer -> showMessageComposer = false
      showChangePinDialog -> showChangePinDialog = false
      showChangeDestructPinDialog -> showChangeDestructPinDialog = false
      showWipeAllDialog -> showWipeAllDialog = false
      activeTab == VaultTab.MEDIA && activeFolder != null -> activeFolder = null
      else -> onLockAndExit()
    }
  }

  val mediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
  ) { uris ->
    if (uris.isNotEmpty()) {
      onImportMediaUris(uris)
    } else {
      onEndSystemAction()
    }
  }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    if (uris.isNotEmpty()) {
      onImportMediaUris(uris)
    } else {
      onEndSystemAction()
    }
  }

  val documentFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    if (uris.isNotEmpty()) {
      onImportMediaUris(uris)
    } else {
      onEndSystemAction()
    }
  }

  val deleteConsentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult(),
  ) { result ->
    onDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
  }

  // Trigger OS confirmation prompt to permanently delete original Gallery copies after vault encryption
  LaunchedEffect(uiState.pendingGalleryDelete) {
    val pending = uiState.pendingGalleryDelete ?: return@LaunchedEffect
    if (pending.uris.isEmpty()) {
      onClearPendingDelete()
      return@LaunchedEffect
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val mediaUris = pending.uris.filter { it.authority == MediaStore.AUTHORITY }
      if (mediaUris.isNotEmpty()) {
        val launched = runCatching {
          val intent = MediaStore.createDeleteRequest(context.contentResolver, mediaUris)
          deleteConsentLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
          true
        }.getOrDefault(false)
        if (!launched) {
          onDeleteConfirmationResult(false)
        }
      } else {
        onDeleteConfirmationResult(false)
      }
    } else {
      var anyDeleted = false
      for (uri in pending.uris) {
        try {
          if (DocumentsContract.isDocumentUri(context, uri)) {
            if (DocumentsContract.deleteDocument(context.contentResolver, uri)) {
              anyDeleted = true
            }
          } else if (context.contentResolver.delete(uri, null, null) > 0) {
            anyDeleted = true
          }
        } catch (sec: SecurityException) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
            deleteConsentLauncher.launch(
              IntentSenderRequest.Builder(sec.userAction.actionIntent.intentSender).build()
            )
            return@LaunchedEffect
          }
        } catch (_: Throwable) {
        }
      }
      onDeleteConfirmationResult(anyDeleted)
    }
  }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    onConsumeStatus()
  }

  if (activeViewerItem != null) {
    VaultMediaViewer(
      item = activeViewerItem!!,
      onBeginSystemAction = onBeginSystemAction,
      onEndSystemAction = onEndSystemAction,
      onClose = { activeViewerItem = null },
      onZeroTraceDelete = { entity -> onZeroTraceDeleteMedia(entity) },
      onExportMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
    )
    return
  }

  Scaffold(
    containerColor = DeepNavyBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Surface(
        color = SlateSurface,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            IconButton(
              onClick = onLockAndExit,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("vault_back_to_game_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Fillwords game",
                tint = TextPrimary,
              )
            }
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentEmerald.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.EnhancedEncryption,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(20.dp),
              )
            }
            Column {
              Text(
                text = "Private Vault",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (uiState.blockScreenshots) {
                  "AES-256 • Screen Block On"
                } else {
                  "AES-256 • Instant Auto-Lock"
                },
                style = MaterialTheme.typography.labelSmall,
                color = AccentEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          OutlinedButton(
            onClick = onLockAndExit,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("vault_lock_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              tint = AccentGold,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text("Logout", color = TextPrimary)
          }
        }
      }
    },
    bottomBar = {
      NavigationBar(
        containerColor = SlateSurface,
        modifier = Modifier.navigationBarsPadding(),
      ) {
        VaultTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = activeTab == tab,
            onClick = {
              if (tab == VaultTab.MEDIA && activeTab == VaultTab.MEDIA) {
                activeFolder = null
              } else {
                activeTab = tab
              }
            },
            icon = { Icon(tab.icon, contentDescription = tab.label) },
            label = { Text(tab.label) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = AccentSky,
              selectedTextColor = AccentSky,
              indicatorColor = AccentSky.copy(alpha = 0.18f),
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary,
            ),
            modifier = Modifier.testTag("vault_tab_${tab.name.lowercase()}"),
          )
        }
      }
    },
    floatingActionButton = {
      when (activeTab) {
        VaultTab.MEDIA -> FloatingActionButton(
          onClick = {
            when (activeFolder) {
              VaultFolder.PHOTOS -> {
                onBeginSystemAction()
                mediaPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              VaultFolder.VIDEOS -> {
                onBeginSystemAction()
                mediaPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
              }
              VaultFolder.AUDIO -> {
                onBeginSystemAction()
                audioPickerLauncher.launch(arrayOf("audio/*"))
              }
              VaultFolder.DOCUMENTS, VaultFolder.OTHER -> {
                onBeginSystemAction()
                documentFilePickerLauncher.launch(arrayOf("*/*"))
              }
              null, VaultFolder.ALL -> {
                showAddChooserDialog = true
              }
            }
          },
          containerColor = PillWhite,
          contentColor = PillNavyInk,
          modifier = Modifier.testTag("vault_add_media_fab"),
        ) {
          if (uiState.isImporting) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PillNavyInk)
          } else {
            Icon(Icons.Filled.Add, contentDescription = "Hide files in vault")
          }
        }

        VaultTab.MESSAGES -> FloatingActionButton(
          onClick = {
            editingMessage = null
            showMessageComposer = true
          },
          containerColor = PillWhite,
          contentColor = PillNavyInk,
          modifier = Modifier.testTag("vault_add_message_fab"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Add private message")
        }

        VaultTab.SECURITY -> Unit
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (activeTab) {
        VaultTab.MEDIA -> VaultMediaTabContent(
          items = mediaItems,
          activeFolder = activeFolder,
          onSelectFolder = { folder ->
            activeFolder = folder
            if (folder == VaultFolder.ALL) {
              mediaFilter = MediaFilter.ALL
            }
          },
          onBackToFolders = { activeFolder = null },
          filter = mediaFilter,
          onFilterChange = { mediaFilter = it },
          onItemClick = { activeViewerItem = it },
          onDeleteClick = { pendingWipeItem = it },
        )

        VaultTab.MESSAGES -> VaultMessagesTabContent(
          messages = messages,
          onEdit = {
            editingMessage = it
            showMessageComposer = true
          },
          onCopy = { text ->
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("Vault Message", text))
            scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
          },
          onDelete = onZeroTraceDeleteMessage,
        )

        VaultTab.SECURITY -> VaultSecurityTabContent(
          mediaCount = mediaItems.size,
          messageCount = messages.size,
          unlockedViaRecovery = uiState.unlockedViaRecovery,
          hasPinConfigured = hasPinConfigured,
          hasCustomDestructPin = hasCustomDestructPin,
          blockScreenshots = uiState.blockScreenshots,
          onToggleBlockScreenshots = onToggleBlockScreenshots,
          onChangePinClick = { showChangePinDialog = true },
          onChangeDestructPinClick = { showChangeDestructPinDialog = true },
          onLogoutClick = onLockAndExit,
          onWipeAllClick = { showWipeAllDialog = true },
        )
      }
    }
  }

  if (showAddChooserDialog) {
    AddVaultItemChooserDialog(
      onDismiss = { showAddChooserDialog = false },
      onPickPhotosAndVideos = {
        showAddChooserDialog = false
        onBeginSystemAction()
        mediaPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
      },
      onPickAudio = {
        showAddChooserDialog = false
        onBeginSystemAction()
        audioPickerLauncher.launch(arrayOf("audio/*"))
      },
      onPickDocumentsAndFiles = {
        showAddChooserDialog = false
        onBeginSystemAction()
        documentFilePickerLauncher.launch(arrayOf("*/*"))
      },
    )
  }

  val pendingSaf = uiState.pendingSafDelete
  if (pendingSaf != null && uiState.pendingGalleryDelete == null) {
    AlertDialog(
      onDismissRequest = onDismissSafDelete,
      containerColor = SlateSurface,
      title = { Text("Remove Original File(s)?", color = TextPrimary) },
      text = {
        Text(
          text = "${pendingSaf.items.size} file(s) are now AES-256 encrypted inside your Private Vault. Permanently remove the original copy from your device folder?",
          color = TextSecondary,
        )
      },
      confirmButton = {
        Button(
          onClick = onConfirmSafDelete,
          colors = ButtonDefaults.buttonColors(containerColor = AccentCoral, contentColor = TextPrimary),
          modifier = Modifier.testTag("confirm_saf_delete_original"),
        ) {
          Text("Remove Original", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissSafDelete) {
          Text("Keep Original", color = TextSecondary)
        }
      },
    )
  }

  pendingWipeItem?.let { entity ->
    ZeroTraceDeleteMediaDialog(
      entity = entity,
      onDismiss = { pendingWipeItem = null },
      onConfirmWipe = {
        pendingWipeItem = null
        onZeroTraceDeleteMedia(it)
      },
    )
  }

  if (showMessageComposer) {
    VaultMessageEditorDialog(
      existing = editingMessage,
      onDismiss = { showMessageComposer = false },
      onSave = { id, contact, body, accent ->
        showMessageComposer = false
        onSaveMessage(id, contact, body, accent)
      },
    )
  }

  if (showChangePinDialog) {
    ChangePinDialog(
      requireCurrentPin = !uiState.unlockedViaRecovery && hasPinConfigured,
      onDismiss = { showChangePinDialog = false },
      onChangePin = onChangePin,
    )
  }

  if (showChangeDestructPinDialog) {
    ChangeDestructPinDialog(
      requireCurrentPin = !uiState.unlockedViaRecovery && hasPinConfigured,
      hasCustomDestructPin = hasCustomDestructPin,
      onDismiss = { showChangeDestructPinDialog = false },
      onSaveDestructPin = onChangeDestructPin,
      onResetDefault = onResetDestructPin,
    )
  }

  if (showWipeAllDialog) {
    WipeAllVaultDialog(
      onDismiss = { showWipeAllDialog = false },
      onConfirmWipeAll = {
        showWipeAllDialog = false
        onWipeAllVault()
      },
    )
  }
}

@Composable
private fun AddVaultItemChooserDialog(
  onDismiss: () -> Unit,
  onPickPhotosAndVideos: () -> Unit,
  onPickAudio: () -> Unit,
  onPickDocumentsAndFiles: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = {
      Text(
        text = "Hide in Private Vault",
        color = TextPrimary,
        modifier = Modifier.testTag("vault_add_chooser_dialog"),
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChooserOptionRow(
          icon = Icons.Filled.PermMedia,
          accent = AccentEmerald,
          title = "Photos & Videos",
          subtitle = "Encrypt from Gallery & confirm removing originals",
          testTag = "add_vault_photos_videos",
          onClick = onPickPhotosAndVideos,
        )
        ChooserOptionRow(
          icon = Icons.Filled.AudioFile,
          accent = AccentSky,
          title = "Audio & Voice Notes",
          subtitle = "MP3, M4A, WAV, OGG recordings & music",
          testTag = "add_vault_audio",
          onClick = onPickAudio,
        )
        ChooserOptionRow(
          icon = Icons.Filled.Description,
          accent = AccentGold,
          title = "Documents & Any File",
          subtitle = "PDF, TXT, Word, ZIP, or any confidential file",
          testTag = "add_vault_documents",
          onClick = onPickDocumentsAndFiles,
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
private fun ChooserOptionRow(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    color = SlateSurfaceElevated,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
      }
    }
  }
}

@Composable
private fun VaultMediaTabContent(
  items: List<VaultMediaEntity>,
  activeFolder: VaultFolder?,
  onSelectFolder: (VaultFolder) -> Unit,
  onBackToFolders: () -> Unit,
  filter: MediaFilter,
  onFilterChange: (MediaFilter) -> Unit,
  onItemClick: (VaultMediaEntity) -> Unit,
  onDeleteClick: (VaultMediaEntity) -> Unit,
) {
  if (activeFolder == null) {
    VaultFolderDirectory(
      items = items,
      onSelectFolder = onSelectFolder,
    )
    return
  }

  val filtered = remember(items, activeFolder, filter) {
    when (activeFolder) {
      VaultFolder.ALL -> when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTO -> items.filter { it.mediaType == VaultStorageManager.TYPE_PHOTO }
        MediaFilter.VIDEO -> items.filter { it.mediaType == VaultStorageManager.TYPE_VIDEO }
        MediaFilter.AUDIO -> items.filter { it.mediaType == VaultStorageManager.TYPE_AUDIO }
        MediaFilter.DOCUMENT -> items.filter { it.mediaType == VaultStorageManager.TYPE_DOCUMENT }
        MediaFilter.OTHER -> items.filter { it.mediaType == VaultStorageManager.TYPE_OTHER }
      }
      else -> items.filter { it.mediaType == activeFolder.mediaType }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 680.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 0.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(
        onClick = onBackToFolders,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("vault_folder_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to folders",
          tint = TextPrimary,
        )
      }
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(activeFolder.accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = activeFolder.icon,
          contentDescription = null,
          tint = activeFolder.accent,
          modifier = Modifier.size(18.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = activeFolder.title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val countLabel = when (filtered.size) {
          0 -> "Empty folder"
          1 -> "1 encrypted item"
          else -> "${filtered.size} encrypted items"
        }
        Text(
          text = countLabel,
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }

    if (activeFolder == VaultFolder.ALL) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vault_filter_row"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(MediaFilter.entries, key = { it.name }) { entry ->
          val count = when (entry) {
            MediaFilter.ALL -> items.size
            MediaFilter.PHOTO -> items.count { it.mediaType == VaultStorageManager.TYPE_PHOTO }
            MediaFilter.VIDEO -> items.count { it.mediaType == VaultStorageManager.TYPE_VIDEO }
            MediaFilter.AUDIO -> items.count { it.mediaType == VaultStorageManager.TYPE_AUDIO }
            MediaFilter.DOCUMENT -> items.count { it.mediaType == VaultStorageManager.TYPE_DOCUMENT }
            MediaFilter.OTHER -> items.count { it.mediaType == VaultStorageManager.TYPE_OTHER }
          }
          FilterChip(
            selected = filter == entry,
            onClick = { onFilterChange(entry) },
            label = { Text("${entry.label} ($count)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentSky.copy(alpha = 0.24f),
              selectedLabelColor = TextPrimary,
            ),
            modifier = Modifier.testTag("vault_filter_${entry.name.lowercase()}"),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = activeFolder.icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(36.dp),
          )
          val emptyTitle = when {
            activeFolder == VaultFolder.ALL && filter == MediaFilter.ALL -> "No Hidden Files Yet"
            activeFolder == VaultFolder.ALL -> "No Hidden ${filter.label} Yet"
            else -> "No Hidden ${activeFolder.title} Yet"
          }
          Text(
            text = emptyTitle,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
          Text(
            text = "Tap + to encrypt ${activeFolder.title.lowercase(Locale.US)} into this protected folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(filtered, key = { it.id }) { entity ->
          VaultMediaTile(
            entity = entity,
            onClick = { onItemClick(entity) },
            onDeleteClick = { onDeleteClick(entity) },
          )
        }
      }
    }
  }
}

@Composable
private fun VaultFolderDirectory(
  items: List<VaultMediaEntity>,
  onSelectFolder: (VaultFolder) -> Unit,
) {
  val totalBytes = remember(items) { items.sumOf { it.sizeBytes } }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 680.dp),
  ) {
    Surface(
      color = SlateSurface,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
        .border(1.dp, AccentEmerald.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(AccentEmerald.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = AccentEmerald,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Encrypted Folders",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
          Text(
            text = "Thumbnails and file names stay hidden until you open a folder.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        Surface(
          color = SlateSurfaceElevated,
          shape = RoundedCornerShape(50),
          modifier = Modifier.border(1.dp, UnsolvedTileStroke.copy(alpha = 0.45f), RoundedCornerShape(50)),
        ) {
          Text(
            text = if (items.isEmpty()) "Empty" else VaultStorageManager.formatFileSize(totalBytes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
    }

    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 132.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("vault_folder_grid"),
    ) {
      items(
        items = VaultFolder.entries,
        key = { it.name },
        span = { folder ->
          if (folder == VaultFolder.ALL) GridItemSpan(maxLineSpan) else GridItemSpan(1)
        },
      ) { folder ->
        val folderItems = remember(items, folder) {
          if (folder.mediaType == null) items else items.filter { it.mediaType == folder.mediaType }
        }
        val count = folderItems.size
        val sizeBytes = remember(folderItems) { folderItems.sumOf { it.sizeBytes } }
        VaultFolderCard(
          folder = folder,
          itemCount = count,
          totalSizeBytes = sizeBytes,
          onClick = { onSelectFolder(folder) },
        )
      }
    }
  }
}

@Composable
private fun VaultFolderCard(
  folder: VaultFolder,
  itemCount: Int,
  totalSizeBytes: Long,
  onClick: () -> Unit,
) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(1.dp, folder.accent.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("vault_folder_${folder.name.lowercase()}"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(folder.accent.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = folder.icon,
            contentDescription = "${folder.title} folder",
            tint = folder.accent,
            modifier = Modifier.size(24.dp),
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(20.dp),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = folder.title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = folder.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = when (itemCount) {
            0 -> "0 items"
            1 -> "1 item"
            else -> "$itemCount items"
          },
          fontFamily = OutfitFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = if (itemCount > 0) folder.accent else TextMuted,
        )
        if (itemCount > 0 && totalSizeBytes > 0L) {
          Text(
            text = VaultStorageManager.formatFileSize(totalSizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun VaultMediaTile(
  entity: VaultMediaEntity,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val isVisualMedia =
    entity.mediaType == VaultStorageManager.TYPE_PHOTO || entity.mediaType == VaultStorageManager.TYPE_VIDEO

  val thumb by produceState<Bitmap?>(initialValue = null, entity.id, isVisualMedia) {
    value = if (isVisualMedia) VaultStorageManager.decryptThumbnailBitmap(entity) else null
  }
  DisposableEffect(entity.id) {
    onDispose { thumb?.recycle() }
  }

  val decodedTitle by produceState(initialValue = "", entity.id, entity.encryptedDisplayName) {
    value = if (!isVisualMedia) {
      runCatching { VaultCrypto.decryptString(entity.encryptedDisplayName) }.getOrDefault("Encrypted File")
    } else {
      ""
    }
  }

  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(14.dp))
      .background(SlateSurface)
      .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag("vault_media_tile_${entity.id}"),
  ) {
    if (isVisualMedia) {
      val bmp = thumb
      if (bmp != null) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Encrypted ${entity.mediaType.lowercase()}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (entity.mediaType == VaultStorageManager.TYPE_VIDEO) {
              Icons.Filled.Videocam
            } else {
              Icons.Filled.PhotoLibrary
            },
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(28.dp),
          )
        }
      }

      if (entity.mediaType == VaultStorageManager.TYPE_VIDEO) {
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play encrypted video",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    } else {
      val (typeIcon, typeAccent) = when (entity.mediaType) {
        VaultStorageManager.TYPE_AUDIO -> Icons.Filled.AudioFile to AccentSky
        VaultStorageManager.TYPE_DOCUMENT -> Icons.Filled.Description to AccentGold
        else -> Icons.Filled.InsertDriveFile to AccentEmerald
      }
      val extLabel = decodedTitle.substringAfterLast('.', "").uppercase(Locale.US)
        .take(5)
        .ifBlank { entity.mediaType.take(4) }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(typeAccent.copy(alpha = 0.08f))
          .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(typeAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = typeIcon,
              contentDescription = "Encrypted ${entity.mediaType.lowercase()}",
              tint = typeAccent,
              modifier = Modifier.size(16.dp),
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(typeAccent.copy(alpha = 0.22f))
              .padding(horizontal = 5.dp, vertical = 2.dp),
          ) {
            Text(
              text = extLabel,
              fontFamily = OutfitFontFamily,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = typeAccent,
            )
          }
        }

        Text(
          text = decodedTitle.ifBlank { "Encrypted File" },
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    IconButton(
      onClick = onDeleteClick,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(48.dp),
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteForever,
          contentDescription = "Zero-trace delete",
          tint = AccentCoral,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(6.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color.Black.copy(alpha = 0.65f))
        .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
      val badge = when {
        (entity.mediaType == VaultStorageManager.TYPE_VIDEO || entity.mediaType == VaultStorageManager.TYPE_AUDIO) &&
          entity.durationMs > 0L -> formatSeconds((entity.durationMs / 1000L).toInt())
        !isVisualMedia && entity.sizeBytes > 0L -> VaultStorageManager.formatFileSize(entity.sizeBytes)
        entity.originalDeleted -> "Moved"
        else -> "Locked"
      }
      Text(
        text = badge,
        fontFamily = OutfitFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (entity.originalDeleted) AccentEmerald else TextPrimary,
      )
    }
  }
}

@Composable
private fun VaultMessagesTabContent(
  messages: List<DecryptedVaultMessage>,
  onEdit: (DecryptedVaultMessage) -> Unit,
  onCopy: (String) -> Unit,
  onDelete: (String) -> Unit,
) {
  if (messages.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.ChatBubbleOutline,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(56.dp),
        )
        Text(
          text = "No Private Messages Yet",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
        )
        Text(
          text = "Tap + to store encrypted messages, secrets, or private notes visible only inside this vault.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          textAlign = TextAlign.Center,
        )
      }
    }
    return
  }

  val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 640.dp),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items(messages, key = { it.id }) { msg ->
      val accent = WordTilePalette[msg.accentIndex % WordTilePalette.size]
      Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
          .clickable { onEdit(msg) },
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(accent),
              )
              Text(
                text = msg.contact,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Row {
              IconButton(onClick = { onCopy(msg.body) }) {
                Icon(
                  imageVector = Icons.Filled.ContentCopy,
                  contentDescription = "Copy message",
                  tint = AccentSky,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(onClick = { onDelete(msg.id) }) {
                Icon(
                  imageVector = Icons.Filled.DeleteForever,
                  contentDescription = "Zero-trace delete message",
                  tint = AccentCoral,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          Text(
            text = msg.body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
          )

          Text(
            text = dateFormatter.format(Date(msg.updatedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
          )
        }
      }
    }
  }
}

@Composable
private fun VaultSecurityTabContent(
  mediaCount: Int,
  messageCount: Int,
  unlockedViaRecovery: Boolean,
  hasPinConfigured: Boolean,
  hasCustomDestructPin: Boolean,
  blockScreenshots: Boolean,
  onToggleBlockScreenshots: (Boolean) -> Unit,
  onChangePinClick: () -> Unit,
  onChangeDestructPinClick: () -> Unit,
  onLogoutClick: () -> Unit,
  onWipeAllClick: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp)
      .testTag("vault_security_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (unlockedViaRecovery) {
      item {
        SecurityInfoRow(
          icon = Icons.Filled.Key,
          accent = AccentGold,
          title = "Recovery Mode Active",
          subtitle = "You unlocked with the 3-attempt recovery code. Set a new PIN right below.",
        )
      }
    }

    item {
      Button(
        onClick = onChangePinClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceElevated, contentColor = TextPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("vault_change_pin_button"),
      ) {
        Icon(Icons.Filled.Key, contentDescription = null, tint = AccentSky)
        Spacer(Modifier.size(8.dp))
        Text(if (unlockedViaRecovery || !hasPinConfigured) "Set New Vault PIN" else "Change Vault PIN")
      }
    }

    item {
      SecurityInfoRow(
        icon = Icons.Filled.VerifiedUser,
        accent = AccentEmerald,
        title = "Hardware Keystore AES-256-GCM",
        subtitle = "Photos, videos, audio, documents & other files ($mediaCount) plus private messages ($messageCount) are encrypted on-device with zero plaintext cache.",
      )
    }

    item {
      Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, AccentCoral.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
          .testTag("vault_destruct_pin_card"),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentCoral.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = AccentCoral)
            }
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Text(
                text = "Emergency Self-Destruct PIN",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = if (hasCustomDestructPin) {
                  "Custom 6-digit hard Wipe PIN active. Enter it 2 consecutive times on the lock screen to silently zero-wipe everything and open an empty Vault."
                } else {
                  "Default hard code: 937184 (6 unique digits, never collides with 4-digit PINs). Enter 2 consecutive times on the lock screen to silently zero-wipe everything and open an empty Vault."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
          if (hasPinConfigured) {
            OutlinedButton(
              onClick = onChangeDestructPinClick,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, AccentCoral.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("vault_change_destruct_pin_button"),
            ) {
              Text(
                text = if (hasCustomDestructPin) "Update Custom Self-Destruct PIN" else "Customize Self-Destruct PIN",
                color = AccentCoral,
              )
            }
          }
        }
      }
    }

    item {
      Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, AccentSky.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(AccentSky.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = AccentSky)
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = "Block Screenshots & Screen Mirroring",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = "Hides recent-apps previews automatically. Turn on FLAG_SECURE to also block screenshots (may black out screen-casting).",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
          Switch(
            checked = blockScreenshots,
            onCheckedChange = onToggleBlockScreenshots,
            colors = SwitchDefaults.colors(
              checkedThumbColor = PillNavyInk,
              checkedTrackColor = AccentEmerald,
            ),
            modifier = Modifier.testTag("vault_block_screenshots_switch"),
          )
        }
      }
    }

    item {
      SecurityInfoRow(
        icon = Icons.Filled.Lock,
        accent = AccentGold,
        title = "Instant Auto-Lock + Recovery",
        subtitle = "Minimizing or pressing Back logs out straight into Fillwords. Forgot PIN? Enter 0000 (or 000000 for 6-digit PIN) 3 times in a row.",
      )
    }

    item { Spacer(Modifier.height(4.dp)) }

    item {
      Button(
        onClick = onLogoutClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = AccentSky.copy(alpha = 0.18f),
          contentColor = TextPrimary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("vault_security_logout_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentSky)
        Spacer(Modifier.size(8.dp))
        Text("Logout & Back to Game")
      }
    }

    item {
      OutlinedButton(
        onClick = onWipeAllClick,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, AccentCoral.copy(alpha = 0.7f)),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("vault_wipe_all_button"),
      ) {
        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = AccentCoral)
        Spacer(Modifier.size(8.dp))
        Text("Zero-Trace Wipe All Vault Data", color = AccentCoral)
      }
    }
  }
}

@Composable
private fun SecurityInfoRow(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
) {
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = accent)
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
    }
  }
}
