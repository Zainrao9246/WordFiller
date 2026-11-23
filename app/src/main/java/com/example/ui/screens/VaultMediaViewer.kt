package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.VaultMediaEntity
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
import com.example.vault.EncryptedVideoDataSource
import com.example.vault.VaultCrypto
import com.example.vault.VaultStorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private class CreateTypedDocumentContract : ActivityResultContract<Pair<String, String>, Uri?>() {
  override fun createIntent(context: Context, input: Pair<String, String>): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT)
      .addCategory(Intent.CATEGORY_OPENABLE)
      .setType(input.second.ifBlank { "*/*" })
      .putExtra(Intent.EXTRA_TITLE, input.first)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
  }
}

@Composable
fun VaultMediaViewer(
  item: VaultMediaEntity,
  onBeginSystemAction: () -> Unit,
  onEndSystemAction: () -> Unit,
  onClose: () -> Unit,
  onZeroTraceDelete: (VaultMediaEntity) -> Unit,
  onExportMessage: (String) -> Unit,
) {
  BackHandler(onBack = onClose)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var isExporting by remember { mutableStateOf(false) }

  val decryptedTitle = remember(item.id) {
    runCatching { VaultCrypto.decryptString(item.encryptedDisplayName) }.getOrDefault("Encrypted File")
  }

  val saveDocumentLauncher = rememberLauncherForActivityResult(
    contract = CreateTypedDocumentContract(),
  ) { destUri ->
    onEndSystemAction()
    if (destUri != null) {
      isExporting = true
      scope.launch {
        val ok = VaultStorageManager.exportToDocumentUri(context, item, destUri)
        isExporting = false
        onExportMessage(if (ok) "Saved decrypted copy" else "Could not save file")
      }
    }
  }

  val triggerExport: () -> Unit = {
    if (item.mediaType == VaultStorageManager.TYPE_PHOTO || item.mediaType == VaultStorageManager.TYPE_VIDEO) {
      isExporting = true
      scope.launch {
        val ok = VaultStorageManager.exportToGallery(context, item)
        isExporting = false
        onExportMessage(if (ok) "Restored copy to Gallery" else "Could not restore to Gallery")
      }
    } else {
      onBeginSystemAction()
      saveDocumentLauncher.launch(decryptedTitle to item.mimeType)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(if (item.mediaType == VaultStorageManager.TYPE_PHOTO || item.mediaType == VaultStorageManager.TYPE_VIDEO) Color.Black else DeepNavyBackground)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    // Center media/file viewport
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 64.dp),
      contentAlignment = Alignment.Center,
    ) {
      when (item.mediaType) {
        VaultStorageManager.TYPE_VIDEO -> {
          EncryptedVaultVideoPlayer(encryptedBlobPath = item.encryptedBlobPath)
        }

        VaultStorageManager.TYPE_PHOTO -> {
          val photoBitmap by produceState<Bitmap?>(initialValue = null, item.id) {
            value = VaultStorageManager.decryptPhotoBitmap(item)
          }
          DisposableEffect(item.id) {
            onDispose { photoBitmap?.recycle() }
          }
          val bmp = photoBitmap
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = decryptedTitle,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            CircularProgressIndicator(color = AccentSky)
          }
        }

        VaultStorageManager.TYPE_AUDIO -> {
          EncryptedVaultAudioPlayer(
            title = decryptedTitle,
            sizeBytes = item.sizeBytes,
            encryptedBlobPath = item.encryptedBlobPath,
            onExportClick = triggerExport,
          )
        }

        VaultStorageManager.TYPE_DOCUMENT -> {
          EncryptedDocumentViewer(
            item = item,
            title = decryptedTitle,
            onCopyText = { text ->
              val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              cb.setPrimaryClip(ClipData.newPlainText(decryptedTitle, text))
              onExportMessage("Copied document text")
            },
            onSaveCopy = triggerExport,
          )
        }

        else -> {
          EncryptedGenericFileCard(
            item = item,
            title = decryptedTitle,
            onSaveCopy = triggerExport,
          )
        }
      }
    }

    // Top Action Bar
    Surface(
      color = DeepNavyBackground.copy(alpha = 0.88f),
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          IconButton(
            onClick = onClose,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("viewer_close_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Close viewer",
              tint = TextPrimary,
            )
          }
          Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
              text = decryptedTitle,
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (item.originalDeleted) {
                "Vault-only • Zero original copy"
              } else {
                "Encrypted in Vault • ${VaultStorageManager.formatFileSize(item.sizeBytes)}"
              },
              style = MaterialTheme.typography.labelSmall,
              color = AccentEmerald,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            enabled = !isExporting,
            onClick = triggerExport,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("viewer_export_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Download,
              contentDescription = "Export decrypted copy",
              tint = AccentSky,
            )
          }

          IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("viewer_delete_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteForever,
              contentDescription = "Delete permanently with zero trace",
              tint = AccentCoral,
            )
          }
        }
      }
    }

    if (showDeleteConfirm) {
      AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        containerColor = SlateSurface,
        title = { Text("Zero-Trace Delete?", color = TextPrimary) },
        text = {
          Text(
            "This will overwrite the encrypted file with random data, zero it out, and erase its record permanently. It cannot be recovered.",
            color = TextSecondary,
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showDeleteConfirm = false
              onZeroTraceDelete(item)
              onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
          ) {
            Text("Wipe Forever", color = TextPrimary)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirm = false }) {
            Text("Cancel", color = TextSecondary)
          }
        },
      )
    }
  }
}

@Composable
private fun EncryptedVaultAudioPlayer(
  title: String,
  sizeBytes: Long,
  encryptedBlobPath: String,
  onExportClick: () -> Unit,
) {
  var isPrepared by remember { mutableStateOf(false) }
  var isPlaying by remember { mutableStateOf(false) }
  var hasError by remember { mutableStateOf(false) }
  var durationMs by remember { mutableIntStateOf(0) }
  var currentPosMs by remember { mutableIntStateOf(0) }

  val mediaPlayer = remember { MediaPlayer() }
  val dataSource = remember(encryptedBlobPath) {
    runCatching { EncryptedVideoDataSource(File(encryptedBlobPath)) }.getOrNull()
  }

  DisposableEffect(encryptedBlobPath) {
    if (dataSource != null) {
      runCatching {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(dataSource)
        mediaPlayer.setOnPreparedListener { mp ->
          isPrepared = true
          durationMs = mp.duration.coerceAtLeast(1)
          mp.start()
          isPlaying = true
        }
        mediaPlayer.setOnCompletionListener {
          isPlaying = false
          currentPosMs = durationMs
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
          hasError = true
          isPlaying = false
          true
        }
        mediaPlayer.prepareAsync()
      }.onFailure {
        hasError = true
      }
    } else {
      hasError = true
    }

    onDispose {
      runCatching {
        if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.stop()
      }
      runCatching { mediaPlayer.reset() }
      runCatching { mediaPlayer.release() }
      runCatching { dataSource?.close() }
    }
  }

  LaunchedEffect(isPlaying, isPrepared) {
    while (isPrepared && isPlaying) {
      currentPosMs = runCatching { mediaPlayer.currentPosition }.getOrDefault(currentPosMs)
      delay(250L)
    }
  }

  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 460.dp)
      .padding(24.dp)
      .border(1.dp, AccentSky.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(84.dp)
          .clip(CircleShape)
          .background(AccentSky.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.AudioFile,
          contentDescription = null,
          tint = AccentSky,
          modifier = Modifier.size(42.dp),
        )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "In-Memory Audio Stream • ${VaultStorageManager.formatFileSize(sizeBytes)}",
        style = MaterialTheme.typography.labelMedium,
        color = AccentEmerald,
      )

      if (hasError) {
        Text(
          text = "Preview unavailable for this codec. Use Export to play externally.",
          style = MaterialTheme.typography.bodyMedium,
          color = AccentCoral,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onExportClick,
          colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(8.dp))
          Text("Save Decrypted Copy", fontWeight = FontWeight.Bold)
        }
      } else {
        Slider(
          value = if (durationMs > 0) currentPosMs.toFloat() / durationMs.toFloat() else 0f,
          onValueChange = { fraction ->
            if (isPrepared && durationMs > 0) {
              val target = (fraction * durationMs).toInt()
              currentPosMs = target
              runCatching { mediaPlayer.seekTo(target) }
            }
          },
          colors = SliderDefaults.colors(
            thumbColor = AccentSky,
            activeTrackColor = AccentSky,
          ),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = formatSeconds(currentPosMs / 1000),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
          Text(
            text = formatSeconds(durationMs / 1000),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
        }

        IconButton(
          enabled = isPrepared,
          onClick = {
            if (isPlaying) {
              runCatching { mediaPlayer.pause() }
              isPlaying = false
            } else {
              runCatching { mediaPlayer.start() }
              isPlaying = true
            }
          },
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(AccentSky),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause audio" else "Play audio",
            tint = PillNavyInk,
            modifier = Modifier.size(34.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun EncryptedDocumentViewer(
  item: VaultMediaEntity,
  title: String,
  onCopyText: (String) -> Unit,
  onSaveCopy: () -> Unit,
) {
  val isPdf = remember(item.id) { VaultStorageManager.isPdf(item, title) }
  val isText = remember(item.id) { VaultStorageManager.isTextPreviewable(item, title) }

  if (isPdf) {
    var loading by remember { mutableStateOf(true) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(item.id) {
      loading = true
      pages = VaultStorageManager.renderPdfPagesInMemory(item)
      loading = false
    }
    DisposableEffect(item.id) {
      onDispose { pages.forEach { it.recycle() } }
    }

    if (loading) {
      CircularProgressIndicator(color = AccentSky)
    } else if (pages.isNotEmpty()) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        itemsIndexed(pages) { index, pageBmp ->
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column {
              Image(
                bitmap = pageBmp.asImageBitmap(),
                contentDescription = "PDF page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
    } else {
      EncryptedGenericFileCard(item = item, title = title, onSaveCopy = onSaveCopy)
    }
  } else if (isText) {
    var loading by remember { mutableStateOf(true) }
    var textContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
      loading = true
      textContent = VaultStorageManager.decryptTextPreview(item)
      loading = false
    }

    if (loading) {
      CircularProgressIndicator(color = AccentSky)
    } else if (textContent != null) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 720.dp)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          Button(
            onClick = { onCopyText(textContent.orEmpty()) },
            colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceElevated, contentColor = TextPrimary),
            shape = RoundedCornerShape(50),
          ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentSky)
            Spacer(Modifier.size(6.dp))
            Text("Copy Text")
          }
        }

        Surface(
          color = SlateSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        ) {
          Text(
            text = textContent.orEmpty(),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextPrimary,
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          )
        }
      }
    } else {
      EncryptedGenericFileCard(item = item, title = title, onSaveCopy = onSaveCopy)
    }
  } else {
    EncryptedGenericFileCard(item = item, title = title, onSaveCopy = onSaveCopy)
  }
}

@Composable
private fun EncryptedGenericFileCard(
  item: VaultMediaEntity,
  title: String,
  onSaveCopy: () -> Unit,
) {
  val ext = title.substringAfterLast('.', "").uppercase().ifBlank { item.mediaType }
  Surface(
    color = SlateSurface,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 460.dp)
      .padding(24.dp)
      .border(1.dp, AccentGold.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(AccentGold.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (item.mediaType == VaultStorageManager.TYPE_DOCUMENT) {
            Icons.Filled.Description
          } else {
            Icons.Filled.InsertDriveFile
          },
          contentDescription = null,
          tint = AccentGold,
          modifier = Modifier.size(38.dp),
        )
      }

      Surface(
        color = AccentEmerald.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.EnhancedEncryption,
            contentDescription = null,
            tint = AccentEmerald,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "$ext • ${VaultStorageManager.formatFileSize(item.sizeBytes)}",
            style = MaterialTheme.typography.labelMedium,
            color = AccentEmerald,
          )
        }
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Encrypted with AES-256-GCM inside your Private Vault. Zero plaintext copy is left on storage.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )

      Button(
        onClick = onSaveCopy,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
      ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Save Decrypted Copy", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun EncryptedVaultVideoPlayer(encryptedBlobPath: String) {
  var isPrepared by remember { mutableStateOf(false) }
  var isPlaying by remember { mutableStateOf(false) }
  var durationMs by remember { mutableIntStateOf(0) }
  var currentPosMs by remember { mutableIntStateOf(0) }
  var videoAspect by remember { mutableStateOf(16f / 9f) }

  val mediaPlayer = remember { MediaPlayer() }
  val dataSource = remember(encryptedBlobPath) {
    runCatching { EncryptedVideoDataSource(File(encryptedBlobPath)) }.getOrNull()
  }

  DisposableEffect(encryptedBlobPath) {
    onDispose {
      runCatching {
        if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.stop()
      }
      runCatching { mediaPlayer.reset() }
      runCatching { mediaPlayer.release() }
      runCatching { dataSource?.close() }
    }
  }

  LaunchedEffect(isPlaying, isPrepared) {
    while (isPrepared && isPlaying) {
      currentPosMs = runCatching { mediaPlayer.currentPosition }.getOrDefault(currentPosMs)
      delay(250L)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      AndroidView(
        factory = { ctx ->
          SurfaceView(ctx).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
              override fun surfaceCreated(holder: SurfaceHolder) {
                if (dataSource == null) return
                runCatching {
                  mediaPlayer.reset()
                  mediaPlayer.setDataSource(dataSource)
                  mediaPlayer.setDisplay(holder)
                  mediaPlayer.setOnPreparedListener { mp ->
                    isPrepared = true
                    durationMs = mp.duration.coerceAtLeast(1)
                    if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                      videoAspect = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                    }
                    mp.start()
                    isPlaying = true
                  }
                  mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentPosMs = durationMs
                  }
                  mediaPlayer.prepareAsync()
                }
              }

              override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
              override fun surfaceDestroyed(holder: SurfaceHolder) {
                runCatching {
                  if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                  }
                  mediaPlayer.setDisplay(null)
                }
              }
            })
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(videoAspect.coerceIn(0.4f, 2.4f)),
      )

      if (!isPrepared) {
        CircularProgressIndicator(color = AccentSky)
      }
    }

    // Bottom Playback Controls
    Surface(
      color = DeepNavyBackground.copy(alpha = 0.85f),
      shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        IconButton(
          enabled = isPrepared,
          onClick = {
            if (isPlaying) {
              runCatching { mediaPlayer.pause() }
              isPlaying = false
            } else {
              runCatching { mediaPlayer.start() }
              isPlaying = true
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(AccentSky.copy(alpha = 0.22f)),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = TextPrimary,
          )
        }

        Slider(
          value = if (durationMs > 0) currentPosMs.toFloat() / durationMs.toFloat() else 0f,
          onValueChange = { fraction ->
            if (isPrepared && durationMs > 0) {
              val target = (fraction * durationMs).toInt()
              currentPosMs = target
              runCatching { mediaPlayer.seekTo(target) }
            }
          },
          colors = SliderDefaults.colors(
            thumbColor = AccentSky,
            activeTrackColor = AccentSky,
          ),
          modifier = Modifier.weight(1f),
        )

        Text(
          text = "${formatSeconds(currentPosMs / 1000)} / ${formatSeconds(durationMs / 1000)}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
      }
    }
  }
}
