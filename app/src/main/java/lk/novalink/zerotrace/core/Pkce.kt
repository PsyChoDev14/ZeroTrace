package lk.novalink.zerotrace.core

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * RFC 7636 PKCE helpers for the NetchSuite sign-in. Kept free of android.* classes (own base64url
 * encoder) so they can be unit-tested on the plain JVM.
 */
object Pkce {
    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val B64URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val random = SecureRandom()

    private fun randomString(length: Int): String =
        buildString(length) { repeat(length) { append(UNRESERVED[random.nextInt(UNRESERVED.length)]) } }

    fun generateVerifier(): String = randomString(64)

    fun generateState(): String = randomString(32)

    fun challengeFor(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /** Unpadded URL-safe base64. */
    internal fun base64Url(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i + 2 < bytes.size) {
            val n = ((bytes[i].toInt() and 0xFF) shl 16) or
                ((bytes[i + 1].toInt() and 0xFF) shl 8) or
                (bytes[i + 2].toInt() and 0xFF)
            out.append(B64URL[(n shr 18) and 63]).append(B64URL[(n shr 12) and 63])
                .append(B64URL[(n shr 6) and 63]).append(B64URL[n and 63])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val n = (bytes[i].toInt() and 0xFF) shl 16
                out.append(B64URL[(n shr 18) and 63]).append(B64URL[(n shr 12) and 63])
            }
            2 -> {
                val n = ((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
                out.append(B64URL[(n shr 18) and 63]).append(B64URL[(n shr 12) and 63]).append(B64URL[(n shr 6) and 63])
            }
        }
        return out.toString()
    }
}
