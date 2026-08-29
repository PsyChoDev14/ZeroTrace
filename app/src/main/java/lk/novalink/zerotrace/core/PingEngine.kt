package lk.novalink.zerotrace.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingEngine {

    /**
     * Tests TCP latency to a host and port in milliseconds.
     * Returns -1 if unreachable or timed out.
     */
    suspend fun testLatency(host: String, port: Int, timeoutMs: Int = 3000): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val latency = System.currentTimeMillis() - start
            latency
        } catch (e: Exception) {
            -1L
        }
    }
}
