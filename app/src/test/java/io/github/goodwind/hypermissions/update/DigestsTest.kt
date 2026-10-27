package io.github.goodwind.hypermissions.update

import org.junit.Assert.assertEquals
import org.junit.Test

class DigestsTest {
    @Test fun `sha256 of known text`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Digests.sha256Hex("abc".toByteArray()))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Digests.sha256Hex(ByteArray(0)))
    }

    @Test fun `a stream gives the same as the bytes`() {
        val data = ByteArray(100_000) { (it % 251).toByte() }
        assertEquals(Digests.sha256Hex(data), Digests.sha256Hex(data.inputStream()))
    }

    @Test fun `hex of a digest to compare, ignoring case and a prefix`() {
        assertEquals(true, Digests.same("ABC123", "abc123"))
        assertEquals(true, Digests.same("sha256:ABC123", "abc123"))
        assertEquals(false, Digests.same("abc123", "abc124"))
        assertEquals(false, Digests.same(null, "abc123"))
    }
}
