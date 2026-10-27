package io.github.goodwind.hypermissions.update

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DownloadsTest {
    private val data = ByteArray(200_000) { (it % 251).toByte() }

    @Test fun `copies the bytes and says what it copied`() {
        val out = ByteArrayOutputStream()
        val sha = Downloads.copy(ByteArrayInputStream(data), out, Digests.sha256Hex(data)) { }
        assertArrayEquals(data, out.toByteArray())
        assertEquals(Digests.sha256Hex(data), sha)
    }

    @Test fun `reports how far it has got, up to the whole`() {
        val seen = mutableListOf<Long>()
        Downloads.copy(ByteArrayInputStream(data), ByteArrayOutputStream(), Digests.sha256Hex(data)) { seen += it }
        assertTrue(seen.isNotEmpty())
        assertEquals(seen.sorted(), seen)
        assertEquals(data.size.toLong(), seen.last())
    }

    @Test fun `a file that is not the one announced is refused`() {
        try {
            Downloads.copy(ByteArrayInputStream(data), ByteArrayOutputStream(), Digests.sha256Hex(byteArrayOf(1, 2, 3))) { }
            fail("accepted a wrong file")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("digest"))
        }
    }

    @Test fun `the announced digest may be written the way github writes it`() {
        Downloads.copy(ByteArrayInputStream(data), ByteArrayOutputStream(), "sha256:" + Digests.sha256Hex(data).uppercase()) { }
    }

    @Test fun `without an announced digest nothing is accepted`() {
        try {
            Downloads.copy(ByteArrayInputStream(data), ByteArrayOutputStream(), null) { }
            fail("accepted a file nobody vouched for")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("digest"))
        }
    }
}
