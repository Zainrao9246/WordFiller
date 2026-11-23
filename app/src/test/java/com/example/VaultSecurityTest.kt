package com.example

import com.example.vault.EncryptedVideoDataSource
import com.example.vault.VaultCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultSecurityTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `pbkdf2 pin hashing verifies matching pin and rejects wrong pin`() {
    val salt = VaultCrypto.generateSaltBase64()
    val hash = VaultCrypto.hashPinBase64("4829", salt)

    assertTrue(VaultCrypto.verifyPin("4829", salt, hash))
    assertFalse(VaultCrypto.verifyPin("4828", salt, hash))
    assertFalse(VaultCrypto.verifyPin("", salt, hash))
  }

  @Test
  fun `aes gcm string and bytes encryption roundtrip hides plaintext`() {
    val secret = "Meet at 9pm — door code 7391"
    val encrypted = VaultCrypto.encryptString(secret)
    assertNotEquals(secret, encrypted)
    assertFalse(encrypted.contains("7391"))
    assertEquals(secret, VaultCrypto.decryptString(encrypted))
  }

  @Test
  fun `chunked encrypted video supports random-access in-memory playback across chunk boundaries`() {
    // 150 KB spans 3 chunks (64KB + 64KB + 22KB)
    val plainVideo = ByteArray(150 * 1024) { idx -> ((idx * 31 + 7) and 0xFF).toByte() }
    val encryptedFile = tempFolder.newFile("sample_video.bin")

    val writtenSize = VaultCrypto.encryptVideoStream(ByteArrayInputStream(plainVideo), encryptedFile)
    assertEquals(plainVideo.size.toLong(), writtenSize)

    EncryptedVideoDataSource(encryptedFile).use { source ->
      assertEquals(plainVideo.size.toLong(), source.size)

      // Read across boundary of chunk 0 and chunk 1 (around 64 KB)
      val crossOffset = (64 * 1024) - 512L
      val outBuf = ByteArray(2048)
      val read = source.readAt(crossOffset, outBuf, 0, outBuf.size)
      assertEquals(outBuf.size, read)
      val expectedSlice = plainVideo.copyOfRange(crossOffset.toInt(), crossOffset.toInt() + outBuf.size)
      assertArrayEquals(expectedSlice, outBuf)
    }

    // Export full decrypted stream verification
    val exported = ByteArrayOutputStream()
    VaultCrypto.decryptVideoToStream(encryptedFile, exported)
    assertArrayEquals(plainVideo, exported.toByteArray())
  }

  @Test
  fun `secureWipeFile overwrites and removes file with no leftover file on disk`() {
    val file: File = tempFolder.newFile("secret_blob.bin")
    file.writeBytes(ByteArray(8192) { 0x5A })
    assertTrue(file.exists())

    val wiped = VaultCrypto.secureWipeFile(file)
    assertTrue(wiped)
    assertFalse(file.exists())
  }

  @Test
  fun `six digit pin unlocks via 000000 entered three consecutive times`() = kotlinx.coroutines.runBlocking {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.FillwordsDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = com.example.data.repository.VaultRepository(db.vaultDao())
      repo.setupPin("918273")

      val first = repo.verifyPin("000000")
      assertTrue(first is com.example.data.repository.PinVerifyResult.Invalid)
      val second = repo.verifyPin("000000")
      assertTrue(second is com.example.data.repository.PinVerifyResult.Invalid)
      val third = repo.verifyPin("000000")
      assertTrue(third is com.example.data.repository.PinVerifyResult.Success)
      assertTrue((third as com.example.data.repository.PinVerifyResult.Success).viaRecovery)

      repo.resetPinFromUnlockedRecovery("654321")
      val withNew = repo.verifyPin("654321")
      assertEquals(com.example.data.repository.PinVerifyResult.Success(viaRecovery = false), withNew)
    } finally {
      db.close()
    }
  }

  @Test
  fun `classifyVaultType accurately separates photos videos audio documents and other files`() {
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_PHOTO,
      com.example.vault.VaultStorageManager.classifyVaultType("image/jpeg", "pic.jpg"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_VIDEO,
      com.example.vault.VaultStorageManager.classifyVaultType("video/mp4", "clip.mp4"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_AUDIO,
      com.example.vault.VaultStorageManager.classifyVaultType("audio/mpeg", "voice.mp3"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_AUDIO,
      com.example.vault.VaultStorageManager.classifyVaultType("application/octet-stream", "memo.m4a"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_DOCUMENT,
      com.example.vault.VaultStorageManager.classifyVaultType("application/pdf", "bank_statement.pdf"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_DOCUMENT,
      com.example.vault.VaultStorageManager.classifyVaultType("application/octet-stream", "notes.docx"),
    )
    assertEquals(
      com.example.vault.VaultStorageManager.TYPE_OTHER,
      com.example.vault.VaultStorageManager.classifyVaultType("application/zip", "archive.zip"),
    )
  }

  @Test
  fun `chunked document encrypts cleanly, previews in memory, and wipes with zero trace`() = kotlinx.coroutines.runBlocking {
    val docText = "CONFIDENTIAL ACCOUNT RECOVERY BACKUP: alpha-bravo-9921"
    val blobFile = tempFolder.newFile("secret_doc.bin")
    val bytes = docText.toByteArray(Charsets.UTF_8)
    val written = VaultCrypto.encryptChunkedStream(ByteArrayInputStream(bytes), blobFile)
    assertEquals(bytes.size.toLong(), written)

    val entity = com.example.data.local.VaultMediaEntity(
      id = "doc-1",
      mediaType = com.example.vault.VaultStorageManager.TYPE_DOCUMENT,
      mimeType = "text/plain",
      encryptedDisplayName = VaultCrypto.encryptString("backup.txt"),
      encryptedBlobPath = blobFile.absolutePath,
      encryptedThumbPath = "",
      sizeBytes = written,
      durationMs = 0L,
      width = 0,
      height = 0,
      originalDeleted = true,
      sourceUriString = "",
    )

    val preview = com.example.vault.VaultStorageManager.decryptTextPreview(entity)
    assertEquals(docText, preview)

    val wiped = com.example.vault.VaultStorageManager.zeroTraceDeleteFiles(entity)
    assertTrue(wiped)
    assertFalse(blobFile.exists())
  }

  @Test
  fun `hard destructive password triggers SelfDestruct only on 2 consecutive attempts and rejects weak custom codes`() = kotlinx.coroutines.runBlocking {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.FillwordsDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = com.example.data.repository.VaultRepository(db.vaultDao())
      repo.setupPin("4829")

      // Hard code validation: rejects 4-digit, sequences, low-entropy repeats; accepts 6 unique hard digits
      assertTrue(com.example.data.repository.VaultRepository.validateHardDestructPin("4829") != null)
      assertTrue(com.example.data.repository.VaultRepository.validateHardDestructPin("123456") != null)
      assertTrue(com.example.data.repository.VaultRepository.validateHardDestructPin("654321") != null)
      assertTrue(com.example.data.repository.VaultRepository.validateHardDestructPin("112233") != null)
      assertTrue(com.example.data.repository.VaultRepository.validateHardDestructPin("000000") != null)
      assertEquals(null, com.example.data.repository.VaultRepository.validateHardDestructPin("846291"))

      // 1st attempt of default destructive password returns Invalid (streak = 1), interrupted streak resets
      val firstTry = repo.verifyPin(com.example.data.repository.VaultRepository.DEFAULT_DESTRUCT_PIN)
      assertTrue(firstTry is com.example.data.repository.PinVerifyResult.Invalid)

      val interrupted = repo.verifyPin("1111")
      assertTrue(interrupted is com.example.data.repository.PinVerifyResult.Invalid)

      val afterInterrupt = repo.verifyPin(com.example.data.repository.VaultRepository.DEFAULT_DESTRUCT_PIN)
      assertTrue(afterInterrupt is com.example.data.repository.PinVerifyResult.Invalid)

      // 2nd consecutive attempt triggers SelfDestruct!
      val destructResult = repo.verifyPin(com.example.data.repository.VaultRepository.DEFAULT_DESTRUCT_PIN)
      assertEquals(com.example.data.repository.PinVerifyResult.SelfDestruct, destructResult)

      // Custom hard 6-digit destructive PIN replaces default and also requires 2 consecutive entries
      val (saved, _) = repo.setCustomDestructPin("4829", "846291", skipOldPinCheck = false)
      assertTrue(saved)

      // Old default no longer triggers SelfDestruct once customized
      assertTrue(repo.verifyPin("937184") is com.example.data.repository.PinVerifyResult.Invalid)
      assertTrue(repo.verifyPin("937184") is com.example.data.repository.PinVerifyResult.Invalid)

      assertTrue(repo.verifyPin("846291") is com.example.data.repository.PinVerifyResult.Invalid)
      assertEquals(
        com.example.data.repository.PinVerifyResult.SelfDestruct,
        repo.verifyPin("846291"),
      )
    } finally {
      db.close()
    }
  }
}
