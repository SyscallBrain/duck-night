package pt.tiago.ducknight.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface SettingsSource {
    val settings: Flow<Settings>
    suspend fun current(): Settings
    suspend fun update(transform: (Settings) -> Settings)
}

class SettingsRepository(private val store: DataStore<Settings>) : SettingsSource {
    override val settings: Flow<Settings> = store.data
    override suspend fun current(): Settings = store.data.first()
    override suspend fun update(transform: (Settings) -> Settings) {
        store.updateData { transform(it).sanitized() }
    }

    companion object {
        fun create(context: Context): SettingsRepository = SettingsRepository(
            DataStoreFactory.create(
                serializer = SettingsSerializer,
                produceFile = {
                    File(context.applicationContext.filesDir, "progress/settings.json")
                        .also { it.parentFile?.mkdirs() }
                },
            ),
        )
    }
}

object SettingsSerializer : Serializer<Settings> {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings = runCatching {
        json.decodeFromString(Settings.serializer(), input.readBytes().toString(Charsets.UTF_8)).sanitized()
    }.getOrDefault(defaultValue)

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        output.write(json.encodeToString(Settings.serializer(), t.sanitized()).toByteArray(Charsets.UTF_8))
    }
}
