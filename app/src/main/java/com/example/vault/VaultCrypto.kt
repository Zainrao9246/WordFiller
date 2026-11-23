package com.example.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "fillwords_vault_master_aes_v1"
  private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_IV_BYTES = 12
  private const val GCM_TAG_BITS = 128
  private const val GCM_TAG_BYTES = 16
  private const val PBKDF2_ITERATIONS = 100_000
  private const val PBKDF2_KEY_BITS = 256

  const val VIDEO_MAGIC = 0x46564C54 // "FVLT"
  const val VIDEO_VERSION = 1
  const val VIDEO_HEADER_BYTES = 24
  const val VIDEO_CHUNK_SIZE = 64 * 1024 // 64 KB
  const val VIDEO_RECORD_STRIDE = GCM_IV_BYTES + VIDEO_CHUNK_SIZE + GCM_TAG_BYTES

  private val secureRandom = SecureRandom()

  @Volatile
  private var testFallbackKey: SecretKey? = null

  fun generateSaltBase64(): String {
    val salt = ByteArray(16)
    secureRandom.nextBytes(salt)
    return Base64.encodeToString(salt, Base64.NO_WRAP)
  }

  fun hashPinBase64(pin: String, saltBase64: String): String {
    val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
    val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val derived = factory.generateSecret(spec).encoded
    spec.clearPassword()
    return Base64.encodeToString(derived, Base64.NO_WRAP)
  }

  fun verifyPin(pin: String, saltBase64: String, expectedHashBase64: String): Boolean {
    if (pin.isEmpty() || saltBase64.isBlank() || expectedHashBase64.isBlank()) return false
    val candidate = Base64.decode(hashPinBase64(pin, saltBase64), Base64.NO_WRAP)
    val expected = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
    return MessageDigest.isEqual(candidate, expected)
  }

  private fun getOrCreateMasterKey(): SecretKey {
    val hasAndroidKeyStore = Security.getProviders().any { it.name == ANDROID_KEYSTORE }
    if (!hasAndroidKeyStore) {
      // Used strictly in local JVM/Robolectric tests where hardware Keystore is absent
      return testFallbackKey ?: synchronized(this) {
        testFallbackKey ?: SecretKeySpec(ByteArray(32) { (it * 7 + 13).toByte() }, "AES")
          .also { testFallbackKey = it }
      }
    }

    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec = KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .build()
    keyGen.init(spec)
    return keyGen.generateKey()
  }

  fun encryptBytes(plain: ByteArray): ByteArray {
    val key = getOrCreateMasterKey()
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    val hasAndroidKeyStore = Security.getProviders().any { it.name == ANDROID_KEYSTORE }
    if (hasAndroidKeyStore) {
      cipher.init(Cipher.ENCRYPT_MODE, key)
    } else {
      val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
      cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    }
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(plain)
    val out = ByteArrayOutputStream(iv.size + ciphertext.size)
    out.write(iv)
    out.write(ciphertext)
    return out.toByteArray()
  }

  fun decryptBytes(encrypted: ByteArray): ByteArray {
    require(encrypted.size > GCM_IV_BYTES + GCM_TAG_BYTES) { "Corrupted encrypted payload" }
    val iv = encrypted.copyOfRange(0, GCM_IV_BYTES)
    val cipherPayload = encrypted.copyOfRange(GCM_IV_BYTES, encrypted.size)
    val key = getOrCreateMasterKey()
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(cipherPayload)
  }

  fun encryptString(plainText: String): String {
    val encrypted = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(encrypted, Base64.NO_WRAP)
  }

  fun decryptString(encodedCipher: String): String {
    if (encodedCipher.isBlank()) return ""
    val decoded = Base64.decode(encodedCipher, Base64.NO_WRAP)
    return String(decryptBytes(decoded), Charsets.UTF_8)
  }

  /**
   * Encrypts a video stream into fixed-stride authenticated 64 KB AES-GCM chunks so that
   * random-access playback via [EncryptedVideoDataSource] decrypts purely in RAM without ever
   * writing a plaintext file to disk.
   */
  fun encryptVideoStream(input: InputStream, outputFile: File): Long {
    val key = getOrCreateMasterKey()
    val hasAndroidKeyStore = Security.getProviders().any { it.name == ANDROID_KEYSTORE }
    val buffer = ByteArray(VIDEO_CHUNK_SIZE)
    var totalPlainBytes = 0L

    RandomAccessFile(outputFile, "rws").use { raf ->
      // Reserve 24-byte header
      raf.write(ByteArray(VIDEO_HEADER_BYTES))

      while (true) {
        var filled = 0
        while (filled < VIDEO_CHUNK_SIZE) {
          val read = input.read(buffer, filled, VIDEO_CHUNK_SIZE - filled)
          if (read <= 0) break
          filled += read
        }
        if (filled == 0) break

        val chunkPlain = if (filled == buffer.size) buffer else buffer.copyOf(filled)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        if (hasAndroidKeyStore) {
          cipher.init(Cipher.ENCRYPT_MODE, key)
        } else {
          val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
          cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val iv = cipher.iv
        require(iv.size == GCM_IV_BYTES)
        val encryptedChunk = cipher.doFinal(chunkPlain)

        raf.write(iv)
        raf.write(encryptedChunk)
        totalPlainBytes += filled.toLong()
      }

      // Write finalized header at offset 0
      val header = ByteBuffer.allocate(VIDEO_HEADER_BYTES)
        .putInt(VIDEO_MAGIC)
        .putInt(VIDEO_VERSION)
        .putInt(VIDEO_CHUNK_SIZE)
        .putLong(totalPlainBytes)
        .putInt(0)
        .array()
      raf.seek(0L)
      raf.write(header)
      raf.fd.sync()
    }

    return totalPlainBytes
  }

  data class VideoHeader(
    val chunkSize: Int,
    val totalPlainSize: Long,
  )

  fun readVideoHeader(raf: RandomAccessFile): VideoHeader {
    raf.seek(0L)
    val bytes = ByteArray(VIDEO_HEADER_BYTES)
    raf.readFully(bytes)
    val buf = ByteBuffer.wrap(bytes)
    val magic = buf.int
    val version = buf.int
    val chunkSize = buf.int
    val totalPlainSize = buf.long
    require(magic == VIDEO_MAGIC && version == VIDEO_VERSION) { "Invalid encrypted video header" }
    return VideoHeader(chunkSize = chunkSize, totalPlainSize = totalPlainSize)
  }

  fun decryptVideoChunk(
    raf: RandomAccessFile,
    header: VideoHeader,
    chunkIndex: Long,
  ): ByteArray {
    val key = getOrCreateMasterKey()
    val chunkStartPlain = chunkIndex * header.chunkSize.toLong()
    val expectedPlainLen = minOf(
      header.chunkSize.toLong(),
      (header.totalPlainSize - chunkStartPlain).coerceAtLeast(0L),
    ).toInt()
    if (expectedPlainLen <= 0) return ByteArray(0)

    val stride = GCM_IV_BYTES + header.chunkSize + GCM_TAG_BYTES
    val fileOffset = VIDEO_HEADER_BYTES.toLong() + chunkIndex * stride.toLong()
    val encryptedRecordLen = GCM_IV_BYTES + expectedPlainLen + GCM_TAG_BYTES

    val record = ByteArray(encryptedRecordLen)
    raf.seek(fileOffset)
    raf.readFully(record)

    val iv = record.copyOfRange(0, GCM_IV_BYTES)
    val cipherPayload = record.copyOfRange(GCM_IV_BYTES, encryptedRecordLen)
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(cipherPayload)
  }

  fun encryptChunkedStream(input: InputStream, outputFile: File): Long =
    encryptVideoStream(input, outputFile)

  /**
   * Decrypts an encrypted video or file back into an output stream (used only when user explicitly
   * exports/restores a file out of the Vault).
   */
  fun decryptVideoToStream(encryptedFile: File, output: OutputStream) {
    RandomAccessFile(encryptedFile, "r").use { raf ->
      val header = readVideoHeader(raf)
      val totalChunks = (header.totalPlainSize + header.chunkSize - 1) / header.chunkSize
      for (chunkIdx in 0L until totalChunks) {
        val plainChunk = decryptVideoChunk(raf, header, chunkIdx)
        output.write(plainChunk)
        plainChunk.fill(0)
      }
      output.flush()
    }
  }

  fun decryptChunkedToStream(encryptedFile: File, output: OutputStream) =
    decryptVideoToStream(encryptedFile, output)

  /**
   * Overwrites a file with random bytes, then zeros, syncs the file descriptor, truncates, and
   * unlinks it so no encrypted/thumbnail bytes or size remnants stay behind.
   */
  fun secureWipeFile(file: File): Boolean {
    if (!file.exists()) return true
    return runCatching {
      val length = file.length()
      if (length > 0L) {
        val wipeBuf = ByteArray(minOf(length, 64L * 1024L).toInt())
        RandomAccessFile(file, "rws").use { raf ->
          // Pass 1: random noise
          var remaining = length
          raf.seek(0L)
          while (remaining > 0L) {
            secureRandom.nextBytes(wipeBuf)
            val toWrite = minOf(remaining, wipeBuf.size.toLong()).toInt()
            raf.write(wipeBuf, 0, toWrite)
            remaining -= toWrite
          }
          raf.fd.sync()

          // Pass 2: zeros + truncate
          wipeBuf.fill(0)
          remaining = length
          raf.seek(0L)
          while (remaining > 0L) {
            val toWrite = minOf(remaining, wipeBuf.size.toLong()).toInt()
            raf.write(wipeBuf, 0, toWrite)
            remaining -= toWrite
          }
          raf.setLength(0L)
          raf.fd.sync()
        }
      }
      file.delete()
    }.getOrDefault(false)
  }
}
