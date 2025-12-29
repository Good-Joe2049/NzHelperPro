package me.neko.nzhelper.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import java.time.LocalDateTime

// 公共数据类
data class Session(
    val timestamp: LocalDateTime,
    val duration: Int,
    val remark: String,
    val location: String,
    val watchedMovie: Boolean,
    val climax: Boolean,
    val rating: Float,
    val mood: String,
    val props: String
)

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                gson.fromJson(json, sessionsTypeToken) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(sessions)
            prefs.edit { putString(KEY_SESSIONS, json) }
        }
}