package lk.novalink.zerotrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.ui.components.ServerCard
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtAccentSoft
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBgElevated
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted

/**
 * Native Jetpack Compose implementation of Servers.tsx from React design system
 */
@Composable
fun ConfigsScreen(
    configs: List<ProxyConfig>,
    selectedConfigId: String?,
    onSelectConfig: (String) -> Unit,
    onEditConfig: (ProxyConfig) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onPingTest: (ProxyConfig) -> Unit,
    onPingAll: () -> Unit,
    onAddConfigClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var configToDelete by remember { mutableStateOf<ProxyConfig?>(null) }
    var configToShare by remember { mutableStateOf<ProxyConfig?>(null) }
    val activeConfig = configs.find { it.id == selectedConfigId }

    if (configToShare != null) {
        lk.novalink.zerotrace.ui.components.ShareConfigDialog(
            config = configToShare!!,
            onDismiss = { configToShare = null }
        )
    }

    Scaffold(
        containerColor = ZtBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddConfigClick,
                containerColor = ZtAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Config",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = ZtText
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column {
                        Text(
                            text = "Configs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ZtText
                        )
                        Text(
                            text = if (activeConfig != null) "Currently using ${activeConfig.name}" else "${configs.size} configs saved",
                            fontSize = 12.sp,
                            color = ZtTextMuted
                        )
                    }
                }

                // Ping All Action
                if (configs.isNotEmpty()) {
                    TextButton(
                        onClick = onPingAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = ZtAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Ping All",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ping All",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            if (configs.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ZtAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "No configs",
                                tint = ZtAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Configs Saved",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ZtText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Paste your Xray config link (VLESS, VMess, Trojan, Shadowsocks) to connect.",
                            fontSize = 12.5.sp,
                            color = ZtTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddConfigClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZtAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Config", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // List of Saved Configs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        ServerCard(
                            config = config,
                            isSelected = config.id == selectedConfigId,
                            onSelect = { onSelectConfig(config.id) },
                            onEdit = { onEditConfig(config) },
                            onShare = { configToShare = config },
                            onPingTest = { onPingTest(config) },
                            onDelete = { configToDelete = config }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = {
                Text(
                    text = "Delete Config",
                    fontWeight = FontWeight.Bold,
                    color = ZtText
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${configToDelete!!.name}\"?",
                    color = ZtTextMuted,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        configToDelete?.let { onDeleteConfig(it.id) }
                        configToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZtDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel", color = ZtTextMuted)
                }
            },
            containerColor = ZtBgElevated
        )
    }
}
