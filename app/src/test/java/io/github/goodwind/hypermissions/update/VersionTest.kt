package io.github.goodwind.hypermissions.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test fun `a tag or a version name is read as numbers`() {
        assertEquals(Version(listOf(1, 2, 3)), Version.parse("v1.2.3"))
        assertEquals(Version(listOf(0, 1)), Version.parse("0.1"))
        assertEquals(Version(listOf(1, 2, 3)), Version.parse("1.2.3-rc1"))
    }

    @Test fun `what is not a version is null`() {
        assertNull(Version.parse("abc"))
        assertNull(Version.parse(""))
        assertNull(Version.parse(null))
    }

    @Test fun `numbers are compared as numbers`() {
        assertTrue(Version.parse("1.10.0")!! > Version.parse("1.9.9")!!)
        assertTrue(Version.parse("0.2.0")!! > Version.parse("0.1.9")!!)
        assertTrue(Version.parse("1.0")!! < Version.parse("1.0.1")!!)
    }

    @Test fun `missing parts count as zero`() {
        assertEquals(0, Version.parse("0.1")!!.compareTo(Version.parse("0.1.0")!!))
        assertEquals(0, Version.parse("v2")!!.compareTo(Version.parse("2.0.0")!!))
    }
}
