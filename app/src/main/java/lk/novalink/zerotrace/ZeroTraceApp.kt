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

    lateinit var authRepository: lk.novalink.zerotrace.data.repository.AuthRepository
        private set

    lateinit var trafficStatsRepository: TrafficStatsRepository
        private set

    lateinit var installedAppsRepository: lk.novalink.zerotrace.data.repository.InstalledAppsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configRepository = ConfigRepository(this)
        settingsRepository = SettingsRepository(this)
        authRepository = lk.novalink.zerotrace.data.repository.AuthRepository(this, configRepository)
        trafficStatsRepository = TrafficStatsRepository(this)
        installedAppsRepository = lk.novalink.zerotrace.data.repository.InstalledAppsRepository(this)

        // Schedule battery-friendly background checks for updates & notifications
        lk.novalink.zerotrace.core.UpdateCheckWorker.schedulePeriodicCheck(this)
    }

    companion object {
        lateinit var instance: ZeroTraceApp
            private set
    }
}
