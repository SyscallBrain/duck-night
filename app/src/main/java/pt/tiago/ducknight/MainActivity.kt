package pt.tiago.ducknight

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.tiago.ducknight.i18n.AppLanguage
import pt.tiago.ducknight.ui.DuckNightApp

class MainActivity : ComponentActivity() {
    private val container: AppContainer by lazy { (application as App).container }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DuckNightApp(container) }
    }

    override fun onStop() {
        container.music.pause()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        container.music.resume()
    }

    override fun onDestroy() {
        if (isFinishing) container.release()
        super.onDestroy()
    }
}
