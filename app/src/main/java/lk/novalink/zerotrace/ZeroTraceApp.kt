package lk.novalink.zerotrace

import android.app.Application
import lk.novalink.zerotrace.data.repository.ConfigRepository
import lk.novalink.zerotrace.data.repository.SettingsRepository
import lk.novalink.zerotrace.data.repository.TrafficStatsRepository

class ZeroTraceApp : Application() {

    lateinit var configRepository: ConfigRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var trafficStatsRepository: TrafficStatsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configRepository = ConfigRepository(this)
        settingsRepository = SettingsRepository(this)
        trafficStatsRepository = TrafficStatsRepository(this)
    }

    companion object {
        lateinit var instance: ZeroTraceApp
            private set
    }
}
