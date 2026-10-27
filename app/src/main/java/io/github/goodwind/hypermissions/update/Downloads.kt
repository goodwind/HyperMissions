package io.github.goodwind.hypermissions.update

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Getting a release's file, and being sure it is the one GitHub announced. */
object Downloads {
    /**
     * Copies [input] to [output] and checks it against [expected], GitHub's digest of the file: a file nobody vouched
     * for is not accepted either. [onProgress] gets the bytes copied so far. Returns the digest of what was copied.
     */
    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream, expected: String?, onProgress: (Long) -> Unit): String {
        if (expected.isNullOrBlank()) throw IOException("no digest announced for the file")
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            md.update(buffer, 0, n)
            output.write(buffer, 0, n)
            total += n
            onProgress(total)
        }
        val actual = md.digest().joinToString("") { "%02x".format(it) }
        if (!Digests.same(actual, expected)) throw IOException("digest mismatch: the file is not the one announced")
        return actual
    }

    /** Downloads [url] to [dest] (removed again if it does not check out). */
    @Throws(IOException::class)
    fun fetch(url: String, dest: File, expected: String?, onProgress: (done: Long, total: Long) -> Unit) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "HyperHub-updater")
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong
            try {
                c.inputStream.use { input -> dest.outputStream().use { out -> copy(input, out, expected) { onProgress(it, total) } } }
            } catch (e: IOException) {
                dest.delete()
                throw e
            }
        } finally {
            c.disconnect()
        }
    }
}
