package io.github.goodwind.hypermissions.update

import java.io.InputStream
import java.security.MessageDigest

object Digests {
    fun sha256Hex(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun sha256Hex(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            md.update(buffer, 0, n)
        }
        return hex(md.digest())
    }

    /** Two digests as people write them (case, a `sha256:` prefix) are the same; a missing one is not. */
    fun same(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        return a.removePrefix("sha256:").equals(b.removePrefix("sha256:"), ignoreCase = true)
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
