package lk.novalink.zerotrace.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.net.URLEncoder
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a Bitmap QR code from text with custom dimensions and colors.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts or constructs a shareable URI for any ProxyConfig.
     */
    fun getShareableUri(config: ProxyConfig): String {
        if (config.rawConfig.isNotBlank() && (
                    config.rawConfig.startsWith("vless://") ||
                    config.rawConfig.startsWith("vmess://") ||
                    config.rawConfig.startsWith("trojan://") ||
                    config.rawConfig.startsWith("ss://")
                )) {
            return config.rawConfig
        }

        // Reconstruct standard URI if rawConfig is absent
        return when (config.protocol) {
            ProxyProtocol.VLESS -> {
                val sb = StringBuilder("vless://${config.uuid}@${config.server}:${config.port}?")
                sb.append("type=${config.network}")
                sb.append("&security=${config.security}")
                if (config.flow.isNotBlank()) sb.append("&flow=${config.flow}")
                if (config.sni.isNotBlank()) sb.append("&sni=${config.sni}")
                if (config.path.isNotBlank()) sb.append("&path=${URLEncoder.encode(config.path, "UTF-8")}")
                if (config.publicKey.isNotBlank()) sb.append("&pbk=${config.publicKey}")
                if (config.shortId.isNotBlank()) sb.append("&sid=${config.shortId}")
                if (config.fingerprint.isNotBlank()) sb.append("&fp=${config.fingerprint}")
                sb.append("#${URLEncoder.encode(config.name, "UTF-8")}")
                sb.toString()
            }
            ProxyProtocol.TROJAN -> {
                val sb = StringBuilder("trojan://${config.uuid}@${config.server}:${config.port}?")
                sb.append("type=${config.network}")
                sb.append("&security=${config.security}")
                if (config.sni.isNotBlank()) sb.append("&sni=${config.sni}")
                if (config.path.isNotBlank()) sb.append("&path=${URLEncoder.encode(config.path, "UTF-8")}")
                sb.append("#${URLEncoder.encode(config.name, "UTF-8")}")
                sb.toString()
            }
            else -> config.rawConfig.ifBlank { "${config.server}:${config.port}" }
        }
    }
}
