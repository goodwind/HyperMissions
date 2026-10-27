package io.github.goodwind.hypermissions.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    private val sha = "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"
    private fun release(tag: String = "v0.2.0", draft: Boolean = false, pre: Boolean = false, assets: String = """
        {"name":"HyperMissions-Installer-0.2.0.apk","browser_download_url":"https://x/installer.apk","size":10,"digest":"sha256:$sha"},
        {"name":"HyperMissions-0.2.0.apk","browser_download_url":"https://x/app.apk","size":20,"digest":"sha256:$sha"}""") =
        """{"tag_name":"$tag","draft":$draft,"prerelease":$pre,"html_url":"https://github.com/goodwind/HyperMissions/releases/tag/$tag","assets":[$assets]}"""

    private val appApk = { name: String -> name.startsWith("HyperMissions-") && !name.contains("Installer") }

    @Test fun `a newer release is an update with where to get it and its checksum`() {
        val r = Updates.latest(release(), "0.1.0", appApk) as UpdateResult.Available
        assertEquals("0.2.0", r.update.version)
        assertEquals("v0.2.0", r.update.tag)
        assertEquals("https://x/app.apk", r.update.apkUrl)
        assertEquals("HyperMissions-0.2.0.apk", r.update.apkName)
        assertEquals(sha.lowercase(), r.update.sha256)
        assertEquals("https://github.com/goodwind/HyperMissions/releases/tag/v0.2.0", r.update.pageUrl)
    }

    @Test fun `the same or an older release is not an update`() {
        assertEquals(UpdateResult.UpToDate, Updates.latest(release(), "0.2.0", appApk))
        assertEquals(UpdateResult.UpToDate, Updates.latest(release(), "0.3.0", appApk))
    }

    @Test fun `a version not known at all is anything newer`() {
        assertTrue(Updates.latest(release(), "", appApk) is UpdateResult.Available)
    }

    @Test fun `the asset is the one the caller asks for, not just any apk`() {
        val installer = Updates.latest(release(), "", { it.contains("Installer") }) as UpdateResult.Available
        assertEquals("https://x/installer.apk", installer.update.apkUrl)
    }

    @Test fun `a release without that apk is no release`() {
        assertEquals(UpdateResult.NoRelease, Updates.latest(release(assets = """{"name":"notes.txt","browser_download_url":"u","size":1}"""), "0.1.0", appApk))
        assertEquals(UpdateResult.NoRelease, Updates.latest(release(assets = ""), "0.1.0", appApk))
    }

    @Test fun `a draft or a pre-release is not offered`() {
        assertEquals(UpdateResult.NoRelease, Updates.latest(release(draft = true), "0.1.0", appApk))
        assertEquals(UpdateResult.NoRelease, Updates.latest(release(pre = true), "0.1.0", appApk))
    }

    @Test fun `a checksum that is missing is null, not a guess`() {
        val r = Updates.latest(release(assets = """{"name":"HyperMissions-0.2.0.apk","browser_download_url":"https://x/app.apk","size":20}"""), "0.1.0", appApk) as UpdateResult.Available
        assertNull(r.update.sha256)
    }

    @Test fun `a tag that is not a version and a body that is not json are a bad answer`() {
        assertEquals(UpdateResult.BadAnswer, Updates.latest(release(tag = "nightly"), "0.1.0", appApk))
        assertEquals(UpdateResult.BadAnswer, Updates.latest("<html>", "0.1.0", appApk))
    }
}
