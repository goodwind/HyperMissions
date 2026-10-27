package io.github.goodwind.hypermissions.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class Request(val url: String, val headers: Map<String, String> = emptyMap(), val body: ByteArray = ByteArray(0))

class Reply(val code: Int, val body: String) {
    override fun equals(other: Any?) = other is Reply && other.code == code && other.body == body
    override fun hashCode() = 31 * code + body.hashCode()
    override fun toString() = "Reply($code)"
}

/** How a request gets to a server. Faked in the tests; [HttpTransport] is the real one. */
interface Transport {
    @Throws(IOException::class)
    fun post(request: Request): Reply

    @Throws(IOException::class)
    fun get(request: Request): Reply
}

class HttpTransport(private val connectMs: Int = 8_000, private val readMs: Int = 15_000) : Transport {
    override fun post(request: Request): Reply = send("POST", request)

    override fun get(request: Request): Reply = send("GET", request)

    private fun send(method: String, request: Request): Reply {
        val c = URL(request.url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = connectMs
            c.readTimeout = readMs
            c.requestMethod = method
            request.headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (method == "POST") {
                c.doOutput = true
                c.outputStream.use { it.write(request.body) }
            }
            val code = c.responseCode
            val stream = if (code < 400) c.inputStream else c.errorStream
            return Reply(code, stream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty())
        } finally {
            c.disconnect()
        }
    }
}
