package lk.novalink.zerotrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.parser.ConfigParser
import lk.novalink.zerotrace.ui.components.ProtocolBadge
import lk.novalink.zerotrace.ui.theme.AccentBlue
import lk.novalink.zerotrace.ui.theme.BorderSubtle
import lk.novalink.zerotrace.ui.theme.SapphireCore
import lk.novalink.zerotrace.ui.theme.SapphireLight
import lk.novalink.zerotrace.ui.theme.StatusConnected
import lk.novalink.zerotrace.ui.theme.SurfaceCard
import lk.novalink.zerotrace.ui.theme.SurfaceDark
import lk.novalink.zerotrace.ui.theme.SurfaceElevated
import lk.novalink.zerotrace.ui.theme.TextDim
import lk.novalink.zerotrace.ui.theme.TextMuted
import lk.novalink.zerotrace.ui.theme.TextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditConfigDialog(
    config: ProxyConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (ProxyConfig) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Form Fields, 1: Raw Config Link

    // Form fields
    var name by remember { mutableStateOf(config.name) }
    var server by remember { mutableStateOf(config.server) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var uuid by remember { mutableStateOf(config.uuid) }
    var sni by remember { mutableStateOf(config.sni) }
    var path by remember { mutableStateOf(config.path) }
    var network by remember { mutableStateOf(config.network) }
    var security by remember { mutableStateOf(config.security) }
    var rawConfigText by remember { mutableStateOf(config.rawConfig) }

    val commonSniPresets = listOf(
        Pair("Zoom Free", "zoom.us"),
        Pair("Netflix", "www.netflix.com"),
        Pair("YouTube", "www.youtube.com"),
        Pair("MS Teams", "teams.microsoft.com"),
        Pair("WhatsApp", "web.whatsapp.com"),
        Pair("Direct Host", server)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, BorderSubtle, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SapphireCore.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = SapphireLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Edit Node Config",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProtocolBadge(protocol = config.protocol)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.protocol.displayName,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Parameters vs Raw URI
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceElevated,
                    contentColor = TextWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SapphireLight
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Server Details",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) SapphireLight else TextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Raw Config Link",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) SapphireLight else TextMuted
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 1: FORM FIELDS

                    // Config Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Server Display Name", color = TextMuted) },
                        singleLine = true,
                        colors = customFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SNI / Bug Host (Critical for Sri Lanka free packages)
                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it },
                        label = { Text("SNI / Bug Host (e.g. Zoom / Netflix)", color = TextMuted) },
                        placeholder = { Text("www.netflix.com or zoom.us", color = TextDim) },
                        singleLine = true,
                        colors = customFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick SNI Presets
                    Text(
                        text = "Quick SNI / Bug Host Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonSniPresets.forEach { (label, host) ->
                            val isSelected = sni.trim().equals(host.trim(), ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SapphireCore else SurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) SapphireLight else BorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { sni = host }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Server Address & Port
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = server,
                            onValueChange = { server = it },
                            label = { Text("Server Host / IP", color = TextMuted) },
                            singleLine = true,
                            colors = customFieldColors(),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port", color = TextMuted) },
                            singleLine = true,
                            colors = customFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // UUID / Password
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text("UUID / Password / Key", color = TextMuted) },
                        singleLine = true,
                        colors = customFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transport & Path
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = network,
                            onValueChange = { network = it },
                            label = { Text("Network (tcp/ws/grpc)", color = TextMuted) },
                            singleLine = true,
                            colors = customFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = { Text("Path / Service", color = TextMuted) },
                            singleLine = true,
                            colors = customFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                } else {
                    // TAB 2: RAW CONFIG URI / JSON
                    Text(
                        text = "You can update the full raw configuration link (vless://, vmess://, etc.) directly. Saving will re-parse the parameters.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rawConfigText,
                        onValueChange = { rawConfigText = it },
                        label = { Text("Raw Config String", color = TextMuted) },
                        maxLines = 8,
                        minLines = 4,
                        colors = customFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Cancel Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedConfig = if (selectedTab == 1 && rawConfigText.isNotBlank()) {
                                val reParsed = ConfigParser.parseSingle(rawConfigText)
                                if (reParsed != null) {
                                    reParsed.copy(
                                        id = config.id, // preserve existing id
                                        name = if (name.isNotBlank()) name else reParsed.name,
                                        pingMs = config.pingMs
                                    )
                                } else {
                                    config.copy(
                                        name = name,
                                        server = server.trim(),
                                        port = port.toIntOrNull() ?: config.port,
                                        uuid = uuid.trim(),
                                        sni = sni.trim(),
                                        path = path.trim(),
                                        network = network.trim(),
                                        rawConfig = rawConfigText
                                    )
                                }
                            } else {
                                config.copy(
                                    name = name.ifBlank { config.name },
                                    server = server.trim().ifBlank { config.server },
                                    port = port.toIntOrNull() ?: config.port,
                                    uuid = uuid.trim().ifBlank { config.uuid },
                                    sni = sni.trim(),
                                    path = path.trim(),
                                    network = network.trim().ifBlank { config.network }
                                )
                            }
                            onSaveConfig(updatedConfig)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SapphireCore,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SapphireLight,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    cursorColor = SapphireLight
)
