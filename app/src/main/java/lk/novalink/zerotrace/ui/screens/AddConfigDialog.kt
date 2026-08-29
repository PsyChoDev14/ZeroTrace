package lk.novalink.zerotrace.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.parser.ConfigParser
import lk.novalink.zerotrace.ui.components.ProtocolBadge
import lk.novalink.zerotrace.ui.components.QrCodeScannerDialog
import lk.novalink.zerotrace.ui.theme.BorderSubtle
import lk.novalink.zerotrace.ui.theme.SapphireLight
import lk.novalink.zerotrace.ui.theme.StatusConnected
import lk.novalink.zerotrace.ui.theme.StatusRed
import lk.novalink.zerotrace.ui.theme.SurfaceDark
import lk.novalink.zerotrace.ui.theme.TextMuted
import lk.novalink.zerotrace.ui.theme.TextWhite
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtBorder

@Composable
fun AddConfigDialog(
    onDismiss: () -> Unit,
    onSaveConfig: (ProxyConfig) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var parsedConfig by remember { mutableStateOf<ProxyConfig?>(null) }
    var parseError by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    fun updateInput(text: String) {
        val clean = text.trim()
        inputText = clean
        if (clean.isNotBlank()) {
            val parsed = ConfigParser.parseSingle(clean)
            if (parsed != null) {
                parsedConfig = parsed
                customName = parsed.name
                parseError = false
            } else {
                parsedConfig = null
                parseError = true
            }
        } else {
            parsedConfig = null
            parseError = false
        }
    }

    if (showQrScanner) {
        QrCodeScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrCodeScanned = { scanned ->
                showQrScanner = false
                updateInput(scanned)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZtBgElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, ZtBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Xray Config",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextWhite
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Text(
                    text = "Scan QR or paste your vless://, vmess://, trojan:// link.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Quick Import Buttons (Scan QR Code & Paste from Clipboard)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scan QR Button
                    TextButton(
                        onClick = { showQrScanner = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = ZtAccent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ZtAccentSoft)
                            .border(1.dp, ZtAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = ZtAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan QR",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }

                    // Paste Button
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.isNotBlank()) {
                                updateInput(clipText)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextWhite),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1B20))
                            .border(1.dp, ZtBorder, RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste Link",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Config Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { updateInput(it) },
                    label = { Text("Config Link or JSON", color = TextMuted) },
                    placeholder = { Text("vless://... or vmess://...", color = Color(0xFF475569)) },
                    maxLines = 4,
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZtAccent,
                        unfocusedBorderColor = ZtBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ZtAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Validation / Status Feedback
                if (parsedConfig != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1A00E676))
                            .border(1.dp, StatusConnected.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = StatusConnected
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProtocolBadge(protocol = parsedConfig!!.protocol)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Valid config detected",
                                fontSize = 12.sp,
                                color = StatusConnected,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Name Customizer
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Custom Server Name", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZtAccent,
                            unfocusedBorderColor = ZtBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = ZtAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (parseError) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1AFF5252))
                            .border(1.dp, StatusRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Invalid format. Please check your Xray config link.",
                            fontSize = 12.sp,
                            color = StatusRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            parsedConfig?.let {
                                val finalConfig = it.copy(
                                    name = if (customName.isNotBlank()) customName else it.name
                                )
                                onSaveConfig(finalConfig)
                            }
                        },
                        enabled = parsedConfig != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZtAccent,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF263346),
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Save Config",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
