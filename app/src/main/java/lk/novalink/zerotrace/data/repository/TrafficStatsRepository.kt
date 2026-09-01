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

data class ChartBarData(
    val label: String,          // e.g. "16h", "Tue", "W3"
    val sublabel: String = "",  // e.g. "16:00 - 20:00", "01 Sep", "Aug 25-31"
    val bytes: Long = 0L,       // Total download + upload
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val isCurrent: Boolean = false
)

data class LiveTrafficData(
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val connectionSeconds: Long = 0L,
    val chartData: List<ChartBarData> = emptyList()
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

        val now = Date()
        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val hourKey = SimpleDateFormat("yyyyMMdd_HH", Locale.US).format(now)

        val currentDown = prefs.getLong("down_$todayKey", 0L) + downloadBytes
        val currentUp = prefs.getLong("up_$todayKey", 0L) + uploadBytes

        val currentHourDown = prefs.getLong("down_$hourKey", 0L) + downloadBytes
        val currentHourUp = prefs.getLong("up_$hourKey", 0L) + uploadBytes

        prefs.edit()
            .putLong("down_$todayKey", currentDown)
            .putLong("up_$todayKey", currentUp)
            .putLong("down_$hourKey", currentHourDown)
            .putLong("up_$hourKey", currentHourUp)
            .apply()

        loadStats()
    }

    @Synchronized
    fun addUptimeSecond() {
        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val currentSec = prefs.getLong("time_$todayKey", 0L) + 1
        prefs.edit()
            .putLong("time_$todayKey", currentSec)
            .apply()

        loadStats()
    }

    fun loadStats() {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        val currentCal = Calendar.getInstance()
        val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
        val todayKey = dayFormat.format(currentCal.time)

        // ─────────────────────────────────────────────────────────────
        // 1. TODAY STATS (Broken down into 6 4-hour buckets)
        // ─────────────────────────────────────────────────────────────
        val todayDown = prefs.getLong("down_$todayKey", 0L)
        val todayUp = prefs.getLong("up_$todayKey", 0L)
        val todaySec = prefs.getLong("time_$todayKey", 0L)

        val todayChartBars = mutableListOf<ChartBarData>()
        val hourSlots = listOf(
            Pair("00h", 0..3),
            Pair("04h", 4..7),
            Pair("08h", 8..11),
            Pair("12h", 12..15),
            Pair("16h", 16..19),
            Pair("20h", 20..23)
        )

        for ((slotLabel, range) in hourSlots) {
            var slotDown = 0L
            var slotUp = 0L
            for (h in range) {
                val hStr = String.format(Locale.US, "%02d", h)
                val hk = "${todayKey}_$hStr"
                slotDown += prefs.getLong("down_$hk", 0L)
                slotUp += prefs.getLong("up_$hk", 0L)
            }
            // If historical hourly keys weren't present before this update but today has data,
            // attribute today's existing total to current active hour slot
            val isCurrentSlot = currentHour in range
            val totalSlotBytes = if (slotDown + slotUp > 0) {
                slotDown + slotUp
            } else if (isCurrentSlot && (todayDown + todayUp > 0)) {
                todayDown + todayUp
            } else {
                0L
            }

            val startH = String.format(Locale.US, "%02d:00", range.first)
            val endH = String.format(Locale.US, "%02d:59", range.last)

            todayChartBars.add(
                ChartBarData(
                    label = slotLabel,
                    sublabel = "$startH - $endH",
                    bytes = totalSlotBytes,
                    downloadBytes = slotDown,
                    uploadBytes = slotUp,
                    isCurrent = isCurrentSlot
                )
            )
        }

        _todayStats.value = LiveTrafficData(
            downloadBytes = todayDown,
            uploadBytes = todayUp,
            connectionSeconds = todaySec,
            chartData = todayChartBars
        )

        // ─────────────────────────────────────────────────────────────
        // 2. THIS WEEK STATS (Past 7 days with day labels)
        // ─────────────────────────────────────────────────────────────
        val weekChartBars = mutableListOf<ChartBarData>()
        var weekDown = 0L
        var weekUp = 0L
        var weekSec = 0L

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val k = dayFormat.format(c.time)
            val dayName = dayNameFormat.format(c.time)
            val dateLabel = SimpleDateFormat("dd MMM", Locale.US).format(c.time)

            val d = prefs.getLong("down_$k", 0L)
            val u = prefs.getLong("up_$k", 0L)
            val s = prefs.getLong("time_$k", 0L)

            weekDown += d
            weekUp += u
            weekSec += s

            weekChartBars.add(
                ChartBarData(
                    label = dayName,
                    sublabel = dateLabel,
                    bytes = d + u,
                    downloadBytes = d,
                    uploadBytes = u,
                    isCurrent = (i == 0)
                )
            )
        }

        _weekStats.value = LiveTrafficData(
            downloadBytes = weekDown,
            uploadBytes = weekUp,
            connectionSeconds = weekSec,
            chartData = weekChartBars
        )

        // ─────────────────────────────────────────────────────────────
        // 3. THIS MONTH STATS (4 7-day weekly blocks)
        // ─────────────────────────────────────────────────────────────
        val monthChartBars = mutableListOf<ChartBarData>()
        var monthDown = 0L
        var monthUp = 0L
        var monthSec = 0L

        val weekBlocks = listOf(
            Triple("W1", 27 downTo 21, "3 weeks ago"),
            Triple("W2", 20 downTo 14, "2 weeks ago"),
            Triple("W3", 13 downTo 7, "Last week"),
            Triple("W4", 6 downTo 0, "This week")
        )

        for ((wLabel, dayRange, sublabel) in weekBlocks) {
            var blockDown = 0L
            var blockUp = 0L
            for (dayOffset in dayRange) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -dayOffset)
                val k = dayFormat.format(c.time)
                val d = prefs.getLong("down_$k", 0L)
                val u = prefs.getLong("up_$k", 0L)
                val s = prefs.getLong("time_$k", 0L)
                blockDown += d
                blockUp += u
                monthDown += d
                monthUp += u
                monthSec += s
            }

            monthChartBars.add(
                ChartBarData(
                    label = wLabel,
                    sublabel = sublabel,
                    bytes = blockDown + blockUp,
                    downloadBytes = blockDown,
                    uploadBytes = blockUp,
                    isCurrent = (wLabel == "W4")
                )
            )
        }

        _monthStats.value = LiveTrafficData(
            downloadBytes = monthDown,
            uploadBytes = monthUp,
            connectionSeconds = monthSec,
            chartData = monthChartBars
        )
    }

    companion object {
        private const val PREFS_NAME = "zerotrace_traffic_stats"
    }
}
