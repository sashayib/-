package com.example.billing

import android.content.Context
import com.example.data.GameRoom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailySessionQuotaStorage {
    const val FREE_DAILY_SESSION_LIMIT = 2
    private const val PREFS_NAME = "shaddaha_daily_session_quota"

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun countPrefKey(userId: String, day: String = todayKey()): String {
        return "created_${userId}_$day"
    }

    fun getLocalCreatedToday(context: Context, userId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(countPrefKey(userId), 0).coerceAtLeast(0)
    }

    fun countFirestoreRoomsHostedToday(rooms: List<GameRoom>, userId: String): Int {
        val today = todayKey()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return rooms.count { room ->
            room.hostId == userId &&
                room.createdAt?.toDate()?.let { formatter.format(it) == today } == true
        }
    }

    fun getEffectiveCreatedToday(
        context: Context,
        userId: String,
        firestoreMyRooms: List<GameRoom> = emptyList(),
    ): Int {
        val local = getLocalCreatedToday(context, userId)
        val remote = countFirestoreRoomsHostedToday(firestoreMyRooms, userId)
        return maxOf(local, remote)
    }

    fun recordSessionCreated(context: Context, userId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = countPrefKey(userId)
        val updated = prefs.getInt(key, 0).coerceAtLeast(0) + 1
        prefs.edit().putInt(key, updated).apply()
        return updated
    }
}
