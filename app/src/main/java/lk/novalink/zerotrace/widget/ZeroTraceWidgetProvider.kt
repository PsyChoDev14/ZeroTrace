package lk.novalink.zerotrace.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import android.widget.RemoteViews
import lk.novalink.zerotrace.MainActivity
import lk.novalink.zerotrace.R
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.core.VpnTunnelManager

class ZeroTraceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val currentState = VpnTunnelManager.vpnState.value
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentState)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val currentState = VpnTunnelManager.vpnState.value
            Log.d(TAG, "Widget toggle clicked. Current state: $currentState")

            when (currentState) {
                is VpnState.Connected, is VpnState.Connecting -> {
                    // Disconnect immediately
                    VpnTunnelManager.stopVpn(context)
                }
                is VpnState.Stopping -> {
                    // Ignore tap while stopping to avoid race condition
                    Log.d(TAG, "Ignored widget toggle while VPN is stopping")
                }
                is VpnState.Disconnected, is VpnState.Error -> {
                    // Check VPN permission first
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        launchMainActivity(context)
                        return
                    }

                    // Retrieve selected / default config
                    val app = context.applicationContext as? ZeroTraceApp ?: ZeroTraceApp.instance
                    val configs = app.configRepository.configs.value
                    val selectedId = app.configRepository.selectedConfigId.value
                    val targetConfig = configs.find { it.id == selectedId } ?: configs.firstOrNull()

                    if (targetConfig != null) {
                        VpnTunnelManager.startVpn(context, targetConfig)
                    } else {
                        // No configs found, launch app so user can add one
                        launchMainActivity(context)
                    }
                }
            }

            // Immediately refresh widget views
            updateAllWidgets(context, VpnTunnelManager.vpnState.value)
        }
    }

    private fun launchMainActivity(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(launchIntent)
    }

    companion object {
        private const val TAG = "ZeroTraceWidget"
        const val ACTION_WIDGET_TOGGLE = "lk.novalink.zerotrace.widget.ACTION_TOGGLE"

        fun updateAllWidgets(context: Context, state: VpnState) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, ZeroTraceWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                if (appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId, state)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            state: VpnState
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_zerotrace_toggle)

            // Setup click PendingIntent
            val toggleIntent = Intent(context, ZeroTraceWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_button_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Apply visual styles based on VPN state
            when (state) {
                is VpnState.Connected -> {
                    views.setImageViewResource(
                        R.id.widget_circle_bg,
                        R.drawable.widget_circle_connected
                    )
                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_stop)
                    views.setTextViewText(R.id.widget_label, "Connected")
                }
                is VpnState.Connecting -> {
                    views.setImageViewResource(
                        R.id.widget_circle_bg,
                        R.drawable.widget_circle_connecting
                    )
                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_sync)
                    views.setTextViewText(R.id.widget_label, "Connecting…")
                }
                is VpnState.Stopping -> {
                    views.setImageViewResource(
                        R.id.widget_circle_bg,
                        R.drawable.widget_circle_connecting
                    )
                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_sync)
                    views.setTextViewText(R.id.widget_label, "Stopping…")
                }
                is VpnState.Disconnected, is VpnState.Error -> {
                    views.setImageViewResource(
                        R.id.widget_circle_bg,
                        R.drawable.widget_circle_disconnected
                    )
                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_play)
                    views.setTextViewText(R.id.widget_label, context.getString(R.string.app_name))
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
