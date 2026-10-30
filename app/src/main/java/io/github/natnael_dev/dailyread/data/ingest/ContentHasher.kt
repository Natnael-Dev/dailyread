package io.github.natnael_dev.dailyread.data.ingest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object ContentHasher {
    private const val CHUNK_SIZE = 64 * 1024 // 64 KiB

    /**
     * Tier 1 cheap key: size + SHA-256 of first and last 64 KiB.
     */
    suspend fun computeTier1Key(sizeBytes: Long, inputStream: InputStream): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(CHUNK_SIZE)
        var bytesRead = 0
        var totalRead = 0L

        // Read first 64 KiB
        val firstChunk = inputStream.read(buffer, 0, CHUNK_SIZE)
        if (firstChunk > 0) {
            digest.update(buffer, 0, firstChunk)
            totalRead += firstChunk
        }

        // If file is larger than 2 * 64 KiB, skip to the last 64 KiB
        if (sizeBytes > 2 * CHUNK_SIZE) {
            val bytesToSkip = sizeBytes - CHUNK_SIZE - totalRead
            if (bytesToSkip > 0) {
                var skipped = 0L
                while (skipped < bytesToSkip) {
                    val s = inputStream.skip(bytesToSkip - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }
        }

        // Read remaining / last chunk
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        val hashBytes = digest.digest()
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        "t1_${sizeBytes}_$hexString"
    }

    /**
     * Tier 2 full content SHA-256.
     */
    suspend fun computeFullHash(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        hashBytes.joinToString("") { "%02x".format(it) }
    }
}
