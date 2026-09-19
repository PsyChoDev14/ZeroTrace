package lk.novalink.zerotrace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.novalink.zerotrace.data.model.SubscriptionInfo
import lk.novalink.zerotrace.data.repository.AuthState
import lk.novalink.zerotrace.ui.components.AccountAvatar
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtDanger
import lk.novalink.zerotrace.ui.theme.ZtDangerSoft
import lk.novalink.zerotrace.ui.theme.ZtSuccess
import lk.novalink.zerotrace.ui.theme.ZtSurface
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextFaint
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import java.util.Locale

/** Signed in: profile, LKR balance and subscriptions. Signed out: the login page. */
@Composable
fun AccountScreen(
    auth: AuthState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onCancelLogin: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val profile = auth.profile

    if (profile == null) {
        Box(modifier = modifier.fillMaxSize()) {
            LoginScreen(loading = auth.loading, error = auth.error, onLogin = onLogin, onCancel = onCancelLogin)
            IconButton(
                onClick = onBack,
                modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZtTextMuted)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZtBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZtTextMuted)
            }
            Text("Account", color = ZtText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (auth.loading) {
                CircularProgressIndicator(color = ZtAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 0.dp))
                Spacer(Modifier.width(12.dp))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ZtTextMuted)
                }
            }
        }

        auth.error?.let { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZtDangerSoft)
                    .border(1.dp, ZtDanger.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ZtDanger, modifier = Modifier.size(16.dp))
                Text(message, color = ZtDanger, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Retry", color = ZtDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRefresh() })
            }
        }

        // Profile card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ZtSurface)
                .border(1.dp, ZtBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccountAvatar(name = profile.name, url = profile.avatarUrl, size = 52.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { "ZeroTrace account" }, color = ZtText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile.email, color = ZtTextFaint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(ZtBorder))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ZtTextFaint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Balance", color = ZtTextFaint, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "LKR %,.2f", profile.balance),
                    color = ZtText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
            }
        }

        Text("SUBSCRIPTIONS", color = ZtTextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))

        if (auth.subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZtSurface)
                    .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active subscriptions on this account.", color = ZtTextFaint, fontSize = 13.sp)
            }
        } else {
            auth.subscriptions.forEach { SubscriptionCard(it) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ZtSurface)
                .border(1.dp, ZtBorder, RoundedCornerShape(14.dp))
                .clickable { onLogout() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ZtDanger, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", color = ZtDanger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SubscriptionCard(sub: SubscriptionInfo) {
    val expired = !sub.isActive
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZtSurface)
            .border(1.dp, ZtBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sub.planName.ifBlank { sub.packageName.ifBlank { "Subscription" } },
                color = ZtText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            val tint = if (expired) ZtDanger else ZtSuccess
            Text(
                (if (expired) "EXPIRED" else sub.status.ifBlank { "ACTIVE" }).uppercase(),
                color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = ZtTextFaint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                sub.packageName.ifBlank { sub.serverLocation },
                color = ZtTextFaint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ZtBorder))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    sub.expiry.neverExpires -> "Never expires"
                    sub.expiry.daysRemaining < 0 -> "Expired ${-sub.expiry.daysRemaining}d ago"
                    else -> "${sub.expiry.daysRemaining}d left"
                },
                color = ZtTextFaint, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
            Text(
                String.format(Locale.US, "%.1f / %s", sub.usage.totalGb, if (sub.usage.limitGb > 0) String.format(Locale.US, "%.0f GB", sub.usage.limitGb) else "∞"),
                color = ZtTextFaint, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}
