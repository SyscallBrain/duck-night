package pt.tiago.ducknight

import android.app.Application
import pt.tiago.ducknight.crash.CrashLog

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        container = AppContainer(this)
    }
}
