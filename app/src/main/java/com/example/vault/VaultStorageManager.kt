package com.example.vault

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.system.Os
import com.example.data.local.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class VaultImportOutcome(
  val importedItems: List<VaultMediaEntity>,
  val mediaStoreDeleteItems: List<Pair<String, Uri>>,
  val safDocumentDeleteItems: List<Pair<String, Uri>>,
) {
  val deletableOriginalUris: List<Uri>
    get() = mediaStoreDeleteItems.map { it.second }.distinct()
}

object VaultStorageManager {

  const val TYPE_PHOTO = "PHOTO"
  const val TYPE_VIDEO = "VIDEO"
  const val TYPE_AUDIO = "AUDIO"
  const val TYPE_DOCUMENT = "DOCUMENT"
  const val TYPE_OTHER = "OTHER"

  private const val BLOBS_DIR = "vault_blobs"
  private const val THUMBS_DIR = "vault_thumbs"

  private val AUDIO_EXTS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "amr", "wma", "mid")
  private val DOC_EXTS = setOf(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "md", "csv", "json", "xml", "rtf", "odt", "epub", "log", "html", "htm", "yaml", "yml",
  )

  private fun ensurePrivateDir(context: Context, name: String): File {
    val dir = File(context.filesDir, name)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val noMedia = File(dir, ".nomedia")
    if (!noMedia.exists()) {
      runCatching { noMedia.createNewFile() }
    }
    return dir
  }

  fun classifyVaultType(mime: String, displayName: String): String {
    val cleanMime = mime.lowercase(Locale.US)
    val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
    return when {
      cleanMime.startsWith("image/") -> TYPE_PHOTO
      cleanMime.startsWith("video/") -> TYPE_VIDEO
      cleanMime.startsWith("audio/") || ext in AUDIO_EXTS -> TYPE_AUDIO
      cleanMime == "application/pdf" ||
        cleanMime.startsWith("text/") ||
        cleanMime.contains("word") ||
        cleanMime.contains("officedocument") ||
        cleanMime.contains("spreadsheet") ||
        cleanMime.contains("presentation") ||
        cleanMime == "application/json" ||
        cleanMime == "application/rtf" ||
        ext in DOC_EXTS -> TYPE_DOCUMENT
      else -> TYPE_OTHER
    }
  }

  suspend fun importMediaUris(
    context: Context,
    uris: List<Uri>,
  ): VaultImportOutcome = withContext(Dispatchers.IO) {
    val blobsDir = ensurePrivateDir(context, BLOBS_DIR)
    val thumbsDir = ensurePrivateDir(context, THUMBS_DIR)
    val resolver = context.contentResolver
    val imported = mutableListOf<VaultMediaEntity>()
    val mediaStoreItems = mutableListOf<Pair<String, Uri>>()
    val safDocItems = mutableListOf<Pair<String, Uri>>()

    for (uri in uris) {
      val item = runCatching {
        importSingleUri(context, resolver, uri, blobsDir, thumbsDir)
      }.getOrNull()
      if (item != null) {
        imported += item
        val mediaStoreUri = resolveMediaStoreUriForDelete(context, uri, item.mediaType)
        if (mediaStoreUri != null) {
          mediaStoreItems += item.id to mediaStoreUri
        } else if (DocumentsContract.isDocumentUri(context, uri)) {
          safDocItems += item.id to uri
        }
      }
    }

    VaultImportOutcome(
      importedItems = imported,
      mediaStoreDeleteItems = mediaStoreItems.distinctBy { it.second },
      safDocumentDeleteItems = safDocItems.distinctBy { it.second },
    )
  }

  private fun importSingleUri(
    context: Context,
    resolver: ContentResolver,
    uri: Uri,
    blobsDir: File,
    thumbsDir: File,
  ): VaultMediaEntity? {
    val queriedName = queryDisplayName(resolver, uri)
    val rawMime = resolver.getType(uri) ?: guessMimeFromName(queriedName)
    val displayName = queriedName ?: defaultDisplayName(rawMime)
    val vaultType = classifyVaultType(rawMime, displayName)

    val id = UUID.randomUUID().toString()
    val blobFile = File(blobsDir, "$id.bin")
    val thumbFile = File(thumbsDir, "$id.thm")

    var width = 0
    var height = 0
    var durationMs = 0L
    var sizeBytes = 0L

    when (vaultType) {
      TYPE_PHOTO -> {
        val plainBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        sizeBytes = plainBytes.size.toLong()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size, bounds)
        width = bounds.outWidth
        height = bounds.outHeight

        blobFile.writeBytes(VaultCrypto.encryptBytes(plainBytes))

        val thumbBitmap = decodeSampledBitmap(plainBytes, maxDim = 420)
        plainBytes.fill(0)
        if (thumbBitmap != null) {
          val thumbBytes = encodeThumbnailJpeg(thumbBitmap)
          thumbBitmap.recycle()
          thumbFile.writeBytes(VaultCrypto.encryptBytes(thumbBytes))
          thumbBytes.fill(0)
        }
      }

      TYPE_VIDEO -> {
        resolver.openInputStream(uri)?.use { input ->
          sizeBytes = VaultCrypto.encryptChunkedStream(input, blobFile)
        } ?: return null

        val retriever = MediaMetadataRetriever()
        var frameBitmap: Bitmap? = null
        try {
          retriever.setDataSource(context, uri)
          durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
          width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
          height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
          frameBitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
        } finally {
          runCatching { retriever.release() }
        }

        if (frameBitmap != null) {
          val thumbBytes = encodeThumbnailJpeg(frameBitmap)
          frameBitmap.recycle()
          thumbFile.writeBytes(VaultCrypto.encryptBytes(thumbBytes))
          thumbBytes.fill(0)
        }
      }

      TYPE_AUDIO -> {
        resolver.openInputStream(uri)?.use { input ->
          sizeBytes = VaultCrypto.encryptChunkedStream(input, blobFile)
        } ?: return null

        val retriever = MediaMetadataRetriever()
        try {
          retriever.setDataSource(context, uri)
          durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
        } finally {
          runCatching { retriever.release() }
        }
      }

      TYPE_DOCUMENT, TYPE_OTHER -> {
        resolver.openInputStream(uri)?.use { input ->
          sizeBytes = VaultCrypto.encryptChunkedStream(input, blobFile)
        } ?: return null
      }
    }

    return VaultMediaEntity(
      id = id,
      mediaType = vaultType,
      mimeType = rawMime,
      encryptedDisplayName = VaultCrypto.encryptString(displayName),
      encryptedBlobPath = blobFile.absolutePath,
      encryptedThumbPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
      sizeBytes = sizeBytes,
      durationMs = durationMs,
      width = width,
      height = height,
      originalDeleted = false,
      sourceUriString = uri.toString(),
    )
  }

  /**
   * Resolves a MediaStore URI when available so Android 11+ can display the system
   * `MediaStore.createDeleteRequest` confirmation dialog to remove originals from Gallery/Audio library.
   */
  fun resolveMediaStoreUriForDelete(context: Context, uri: Uri, vaultType: String): Uri? {
    if (uri.authority == MediaStore.AUTHORITY && !uri.toString().contains("/picker")) {
      return uri
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val resolved = runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull()
      if (resolved != null && resolved.authority == MediaStore.AUTHORITY && !resolved.toString().contains("/picker")) {
        return resolved
      }
    }
    val segments = uri.pathSegments
    val mediaId = segments.lastOrNull()?.toLongOrNull()
    if (mediaId != null && uri.toString().contains("picker")) {
      val base = when (vaultType) {
        TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      }
      return ContentUris.withAppendedId(base, mediaId)
    }
    return null
  }

  suspend fun deleteSafDocumentOriginals(
    context: Context,
    uris: List<Uri>,
  ): List<Uri> = withContext(Dispatchers.IO) {
    val deleted = mutableListOf<Uri>()
    for (uri in uris) {
      val ok = runCatching {
        if (DocumentsContract.isDocumentUri(context, uri)) {
          DocumentsContract.deleteDocument(context.contentResolver, uri)
        } else {
          context.contentResolver.delete(uri, null, null) > 0
        }
      }.getOrDefault(false)
      if (ok) {
        deleted += uri
      }
    }
    deleted
  }

  suspend fun decryptThumbnailBitmap(entity: VaultMediaEntity): Bitmap? = withContext(Dispatchers.IO) {
    val path = entity.encryptedThumbPath
    if (path.isBlank()) return@withContext null
    val file = File(path)
    if (!file.exists()) return@withContext null
    runCatching {
      val enc = file.readBytes()
      val plain = VaultCrypto.decryptBytes(enc)
      val bmp = BitmapFactory.decodeByteArray(plain, 0, plain.size)
      plain.fill(0)
      bmp
    }.getOrNull()
  }

  suspend fun decryptPhotoBitmap(entity: VaultMediaEntity): Bitmap? = withContext(Dispatchers.IO) {
    val file = File(entity.encryptedBlobPath)
    if (!file.exists()) return@withContext null
    runCatching {
      val enc = file.readBytes()
      val plain = VaultCrypto.decryptBytes(enc)
      val bmp = decodeSampledBitmap(plain, maxDim = 2200)
      plain.fill(0)
      bmp
    }.getOrNull()
  }

  /**
   * Decrypts a text-based document in memory up to [maxBytes] for safe preview without writing
   * any plaintext temp file to disk.
   */
  suspend fun decryptTextPreview(
    entity: VaultMediaEntity,
    maxBytes: Int = 256 * 1024,
  ): String? = withContext(Dispatchers.IO) {
    val file = File(entity.encryptedBlobPath)
    if (!file.exists()) return@withContext null
    runCatching {
      val out = ByteArrayOutputStream()
      VaultCrypto.decryptChunkedToStream(file, out)
      val bytes = out.toByteArray()
      val sliceLen = minOf(bytes.size, maxBytes)
      val text = String(bytes, 0, sliceLen, Charsets.UTF_8)
      bytes.fill(0)
      if (entity.sizeBytes > maxBytes) {
        "$text\n\n... (preview truncated cleanly at ${maxBytes / 1024} KB)"
      } else {
        text
      }
    }.getOrNull()
  }

  /**
   * Renders up to [maxPages] PDF pages purely in RAM via an anonymous memfd descriptor
   * (zero plaintext cache file written to storage). Returns empty list on API < 27 or non-PDFs.
   */
  suspend fun renderPdfPagesInMemory(
    entity: VaultMediaEntity,
    maxPages: Int = 12,
  ): List<Bitmap> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return@withContext emptyList()
    val blob = File(entity.encryptedBlobPath)
    if (!blob.exists()) return@withContext emptyList()

    var pfd: ParcelFileDescriptor? = null
    return@withContext runCatching {
      val fd = Os.memfd_create("vault_pdf_mem", 0)
      pfd = ParcelFileDescriptor.dup(fd)
      Os.close(fd)

      FileOutputStream(pfd!!.fileDescriptor).use { out ->
        VaultCrypto.decryptChunkedToStream(blob, out)
      }

      val pages = mutableListOf<Bitmap>()
      PdfRenderer(pfd!!).use { renderer ->
        val count = minOf(renderer.pageCount, maxPages)
        for (i in 0 until count) {
          renderer.openPage(i).use { page ->
            val targetWidth = 1080
            val aspect = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.414f
            val targetHeight = (targetWidth * aspect).toInt().coerceIn(200, 2400)
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pages += bmp
          }
        }
      }
      pages
    }.getOrElse {
      emptyList()
    }.also {
      runCatching { pfd?.close() }
    }
  }

  /**
   * Restores a vault photo or video back into the device Gallery (Pictures/Movies -> Restored)
   * when explicitly requested by the user.
   */
  suspend fun exportToGallery(context: Context, entity: VaultMediaEntity): Boolean = withContext(Dispatchers.IO) {
    val blob = File(entity.encryptedBlobPath)
    if (!blob.exists()) return@withContext false

    val resolver = context.contentResolver
    val cleanName = runCatching { VaultCrypto.decryptString(entity.encryptedDisplayName) }
      .getOrDefault(defaultDisplayName(entity.mimeType))

    val collection = if (entity.mediaType == TYPE_VIDEO) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      }
    }

    val values = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
      put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val dir = if (entity.mediaType == TYPE_VIDEO) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/Restored")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
    }

    val targetUri = resolver.insert(collection, values) ?: return@withContext false
    val ok = runCatching {
      resolver.openOutputStream(targetUri)?.use { out ->
        if (entity.mediaType == TYPE_PHOTO) {
          val plain = VaultCrypto.decryptBytes(blob.readBytes())
          out.write(plain)
          plain.fill(0)
        } else {
          VaultCrypto.decryptChunkedToStream(blob, out)
        }
      } ?: error("Cannot open output stream")

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val doneValues = ContentValues().apply {
          put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(targetUri, doneValues, null, null)
      }
      true
    }.getOrElse {
      runCatching { resolver.delete(targetUri, null, null) }
      false
    }
    ok
  }

  /**
   * Decrypts and writes any hidden Document, Audio, or Other file directly to a user-chosen
   * SAF document URI (`ACTION_CREATE_DOCUMENT`) with zero temporary cache files.
   */
  suspend fun exportToDocumentUri(
    context: Context,
    entity: VaultMediaEntity,
    destUri: Uri,
  ): Boolean = withContext(Dispatchers.IO) {
    val blob = File(entity.encryptedBlobPath)
    if (!blob.exists()) return@withContext false
    runCatching {
      context.contentResolver.openOutputStream(destUri)?.use { out ->
        if (entity.mediaType == TYPE_PHOTO) {
          val plain = VaultCrypto.decryptBytes(blob.readBytes())
          out.write(plain)
          plain.fill(0)
        } else {
          VaultCrypto.decryptChunkedToStream(blob, out)
        }
      } ?: return@runCatching false
      true
    }.getOrDefault(false)
  }

  /**
   * Multi-pass overwrites and deletes both the encrypted payload and encrypted thumbnail so
   * zero recoverable bytes remain on disk.
   */
  suspend fun zeroTraceDeleteFiles(entity: VaultMediaEntity): Boolean = withContext(Dispatchers.IO) {
    val blobOk = if (entity.encryptedBlobPath.isNotBlank()) {
      VaultCrypto.secureWipeFile(File(entity.encryptedBlobPath))
    } else {
      true
    }
    val thumbOk = if (entity.encryptedThumbPath.isNotBlank()) {
      VaultCrypto.secureWipeFile(File(entity.encryptedThumbPath))
    } else {
      true
    }
    blobOk && thumbOk
  }

  suspend fun zeroTraceWipeAllDirectories(context: Context) = withContext(Dispatchers.IO) {
    listOf(BLOBS_DIR, THUMBS_DIR).forEach { dirName ->
      val dir = File(context.filesDir, dirName)
      if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
          if (file.name != ".nomedia") {
            VaultCrypto.secureWipeFile(file)
          }
        }
      }
    }
  }

  fun isTextPreviewable(entity: VaultMediaEntity, title: String): Boolean {
    val mime = entity.mimeType.lowercase(Locale.US)
    val ext = title.substringAfterLast('.', "").lowercase(Locale.US)
    return mime.startsWith("text/") ||
      mime == "application/json" ||
      mime == "application/xml" ||
      ext in setOf("txt", "md", "csv", "json", "xml", "log", "yaml", "yml", "ini", "conf")
  }

  fun isPdf(entity: VaultMediaEntity, title: String): Boolean {
    val mime = entity.mimeType.lowercase(Locale.US)
    val ext = title.substringAfterLast('.', "").lowercase(Locale.US)
    return mime == "application/pdf" || ext == "pdf"
  }

  fun formatFileSize(bytes: Long): String {
    return when {
      bytes < 1024L -> "$bytes B"
      bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
      else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
  }

  private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    return runCatching {
      resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
      }
    }.getOrNull()
  }

  private fun guessMimeFromName(name: String?): String {
    val ext = name?.substringAfterLast('.', "")?.lowercase(Locale.US).orEmpty()
    return when {
      ext == "pdf" -> "application/pdf"
      ext in setOf("txt", "md", "log", "csv") -> "text/plain"
      ext in AUDIO_EXTS -> "audio/$ext"
      ext in setOf("png", "jpg", "jpeg", "webp") -> "image/$ext"
      ext in setOf("mp4", "mkv", "mov") -> "video/$ext"
      else -> "application/octet-stream"
    }
  }

  private fun defaultDisplayName(mime: String): String {
    return when {
      mime.startsWith("video/") -> "video.mp4"
      mime.startsWith("audio/") -> "audio.mp3"
      mime == "application/pdf" -> "document.pdf"
      mime.startsWith("text/") -> "note.txt"
      mime.startsWith("image/") -> "photo.jpg"
      else -> "secret_file.bin"
    }
  }

  private fun encodeThumbnailJpeg(bitmap: Bitmap): ByteArray {
    val scaled = if (bitmap.width > 420 || bitmap.height > 420) {
      val ratio = minOf(420f / bitmap.width.toFloat(), 420f / bitmap.height.toFloat())
      Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true,
      )
    } else {
      bitmap
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
    if (scaled !== bitmap) scaled.recycle()
    return out.toByteArray()
  }

  private fun decodeSampledBitmap(bytes: ByteArray, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
  }
}
