package lk.novalink.zerotrace.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LiveTrafficData(
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val connectionSeconds: Long = 0L,
    val dailyHistory: List<Long> = listOf(0L, 0L, 0L, 0L, 0L, 0L, 0L) // Past 7 days bytes
)

class TrafficStatsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _todayStats = MutableStateFlow(LiveTrafficData())
    val todayStats: StateFlow<LiveTrafficData> = _todayStats.asStateFlow()

    private val _weekStats = MutableStateFlow(LiveTrafficData())
    val weekStats: StateFlow<LiveTrafficData> = _weekStats.asStateFlow()

    private val _monthStats = MutableStateFlow(LiveTrafficData())
    val monthStats: StateFlow<LiveTrafficData> = _monthStats.asStateFlow()

    init {
        loadStats()
    }

    @Synchronized
    fun addTraffic(downloadBytes: Long, uploadBytes: Long) {
        if (downloadBytes <= 0 && uploadBytes <= 0) return

        val todayKey = getTodayKey()
        val currentDown = prefs.getLong("down_$todayKey", 0L) + downloadBytes
        val currentUp = prefs.getLong("up_$todayKey", 0L) + uploadBytes

        prefs.edit()
            .putLong("down_$todayKey", currentDown)
            .putLong("up_$todayKey", currentUp)
            .apply()

        loadStats()
    }

    @Synchronized
    fun addUptimeSecond() {
        val todayKey = getTodayKey()
        val currentSec = prefs.getLong("time_$todayKey", 0L) + 1
        prefs.edit()
            .putLong("time_$todayKey", currentSec)
            .apply()

        loadStats()
    }

    fun loadStats() {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cal = Calendar.getInstance()

        // 1. Today Stats
        val todayKey = sdf.format(cal.time)
        val todayDown = prefs.getLong("down_$todayKey", 0L)
        val todayUp = prefs.getLong("up_$todayKey", 0L)
        val todaySec = prefs.getLong("time_$todayKey", 0L)

        // 2. Past 7 days history
        val past7Days = mutableListOf<Long>()
        var weekDown = 0L
        var weekUp = 0L
        var weekSec = 0L

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val k = sdf.format(c.time)
            val d = prefs.getLong("down_$k", 0L)
            val u = prefs.getLong("up_$k", 0L)
            val s = prefs.getLong("time_$k", 0L)

            past7Days.add(d + u)
            weekDown += d
            weekUp += u
            weekSec += s
        }

        // 3. This Month Stats (past 30 days)
        var monthDown = 0L
        var monthUp = 0L
        var monthSec = 0L
        for (i in 29 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val k = sdf.format(c.time)
            monthDown += prefs.getLong("down_$k", 0L)
            monthUp += prefs.getLong("up_$k", 0L)
            monthSec += prefs.getLong("time_$k", 0L)
        }

        _todayStats.value = LiveTrafficData(
            downloadBytes = todayDown,
            uploadBytes = todayUp,
            connectionSeconds = todaySec,
            dailyHistory = past7Days
        )

        _weekStats.value = LiveTrafficData(
            downloadBytes = weekDown,
            uploadBytes = weekUp,
            connectionSeconds = weekSec,
            dailyHistory = past7Days
        )

        _monthStats.value = LiveTrafficData(
            downloadBytes = monthDown,
            uploadBytes = monthUp,
            connectionSeconds = monthSec,
            dailyHistory = past7Days
        )
    }

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }

    companion object {
        private const val PREFS_NAME = "zerotrace_traffic_stats"
    }
}
