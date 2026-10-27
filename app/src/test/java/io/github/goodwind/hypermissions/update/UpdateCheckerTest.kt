package io.github.goodwind.hypermissions.update

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeGet(private val reply: Any) : Transport {
    val requests = mutableListOf<Request>()
    override fun post(request: Request): Reply = error("not used")
    override fun get(request: Request): Reply {
        requests += request
        if (reply is IOException) throw reply
        return reply as Reply
    }
}

class UpdateCheckerTest {
    private fun body(url: String = "https://github.com/a/b/releases/download/v0.2.0/HyperMissions-0.2.0.apk") =
        """{"tag_name":"v0.2.0","draft":false,"prerelease":false,"html_url":"h","assets":[{"name":"HyperMissions-0.2.0.apk","browser_download_url":"$url","size":1}]}"""
    private val body = body()
    private val apk = { n: String -> n.startsWith("HyperMissions-") }

    @Test fun `it asks github for the latest release of the repository`() {
        val t = FakeGet(Reply(200, body))
        UpdateChecker(t, "goodwind/HyperMissions").check("0.1.0", apk)
        val r = t.requests.single()
        assertEquals("https://api.github.com/repos/goodwind/HyperMissions/releases/latest", r.url)
        assertEquals("application/vnd.github+json", r.headers["Accept"])
        assertTrue(r.headers.getValue("User-Agent").isNotBlank())
    }

    @Test fun `an answer is turned into a result`() {
        assertTrue(UpdateChecker(FakeGet(Reply(200, body)), "a/b").check("0.1.0", apk) is UpdateResult.Available)
        assertEquals(UpdateResult.UpToDate, UpdateChecker(FakeGet(Reply(200, body)), "a/b").check("0.2.0", apk))
    }

    @Test fun `no release yet is not a failure`() {
        assertEquals(UpdateResult.NoRelease, UpdateChecker(FakeGet(Reply(404, "{}")), "a/b").check("0.1.0", apk))
    }

    @Test fun `a refusal or a lost connection is a failure that says so`() {
        assertTrue(UpdateChecker(FakeGet(Reply(403, "{}")), "a/b").check("0.1.0", apk) is UpdateResult.Failed)
        assertTrue(UpdateChecker(FakeGet(IOException("down")), "a/b").check("0.1.0", apk) is UpdateResult.Failed)
    }

    @Test fun `a file that is not of this repository's releases is not offered`() {
        for (url in listOf("https://evil.example/releases/download/v0.2.0/x.apk", "http://github.com/a/b/releases/download/v0.2.0/x.apk", "https://github.com/a/other/releases/download/v0.2.0/x.apk", "https://github.com/a/b/releases/download/../../x.apk")) {
            assertEquals(url, UpdateResult.BadAnswer, UpdateChecker(FakeGet(Reply(200, body(url))), "a/b").check("0.1.0", apk))
        }
    }
}
