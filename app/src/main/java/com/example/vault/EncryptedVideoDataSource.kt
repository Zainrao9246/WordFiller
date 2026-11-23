package com.example.vault

import android.media.MediaDataSource
import java.io.File
import java.io.RandomAccessFile

/**
 * Seekable in-memory MediaDataSource that decrypts 64 KB AES-GCM chunks on demand for Android's
 * MediaPlayer, keeping zero decrypted video bytes on disk at all times.
 */
class EncryptedVideoDataSource(private val encryptedFile: File) : MediaDataSource() {

  private val lock = Any()
  private val raf = RandomAccessFile(encryptedFile, "r")
  private val header = VaultCrypto.readVideoHeader(raf)

  private var cachedChunkIndex: Long = -1L
  private var cachedChunkBytes: ByteArray = ByteArray(0)

  override fun getSize(): Long = header.totalPlainSize

  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
    if (size <= 0) return 0
    if (position >= header.totalPlainSize) return -1

    synchronized(lock) {
      val chunkSize = header.chunkSize.toLong()
      var currentPos = position
      var bytesRead = 0
      val toReadTotal = minOf(size.toLong(), header.totalPlainSize - position).toInt()

      while (bytesRead < toReadTotal) {
        val chunkIndex = currentPos / chunkSize
        val offsetInChunk = (currentPos % chunkSize).toInt()

        if (chunkIndex != cachedChunkIndex) {
          if (cachedChunkBytes.isNotEmpty()) {
            cachedChunkBytes.fill(0)
          }
          cachedChunkBytes = VaultCrypto.decryptVideoChunk(raf, header, chunkIndex)
          cachedChunkIndex = chunkIndex
        }

        val available = cachedChunkBytes.size - offsetInChunk
        if (available <= 0) break

        val copyLen = minOf(available, toReadTotal - bytesRead)
        System.arraycopy(cachedChunkBytes, offsetInChunk, buffer, offset + bytesRead, copyLen)
        bytesRead += copyLen
        currentPos += copyLen
      }
      return if (bytesRead == 0) -1 else bytesRead
    }
  }

  override fun close() {
    synchronized(lock) {
      if (cachedChunkBytes.isNotEmpty()) {
        cachedChunkBytes.fill(0)
        cachedChunkBytes = ByteArray(0)
      }
      cachedChunkIndex = -1L
      raf.close()
    }
  }
}
