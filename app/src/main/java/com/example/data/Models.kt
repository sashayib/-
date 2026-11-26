package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object PlayerIdGenerator {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun forUid(uid: String): String {
        if (uid.isBlank()) return "SH000000"
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
        val suffix = buildString(6) {
            for (i in 0 until 6) {
                val idx = (digest[i].toInt() and 0xFF) % ALPHABET.length
                append(ALPHABET[idx])
            }
        }
        return "SH$suffix"
    }

    fun normalizeInput(raw: String): String =
        raw.trim().uppercase().replace("-", "").replace(" ", "")
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val playerCode: String = "",
    val presenceStatus: String = "offline",
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    val visibility: String = "public",
    val rating: Long = 22L,
    val rankTier: String = "مبتدئ",
    val wins: Long = 0L,
    val losses: Long = 0L,
    val matchesPlayed: Long = 0L,
    val streak: Long = 0L,
    val lossStreak: Long = 0L,
    val totalPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun effectivePlayerCode(): String {
        if (playerCode.isNotBlank()) return playerCode
        val uid = userId.ifBlank { id }
        return PlayerIdGenerator.forUid(uid)
    }

    fun effectivePresence(nowMs: Long = System.currentTimeMillis()): String {
        if (presenceStatus == "offline") return "offline"
        val seenMs = lastSeenAt?.toDate()?.time ?: return presenceStatus
        return if (nowMs - seenMs <= PRESENCE_TTL_MS) presenceStatus else "offline"
    }

    companion object {
        const val PRESENCE_TTL_MS = 120_000L
    }
}

@Keep
@IgnoreExtraProperties
data class FriendEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendUid: String = "",
    val friendPlayerCode: String = "",
    val friendName: String = "",
    val friendRating: Long = 22L,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class FriendWithPresence(
    val friendUid: String,
    val playerCode: String,
    val displayName: String,
    val avatarUrl: String?,
    val rating: Int,
    val rankTier: String,
    val presenceStatus: String,
    val lastSeenAt: Timestamp?,
) {
    fun effectivePresence(nowMs: Long = System.currentTimeMillis()): String {
        if (presenceStatus == "offline") return "offline"
        val seenMs = lastSeenAt?.toDate()?.time ?: return presenceStatus
        return if (nowMs - seenMs <= PlayerProfile.PRESENCE_TTL_MS) presenceStatus else "offline"
    }

    fun isOnlineNow(nowMs: Long = System.currentTimeMillis()): Boolean =
        effectivePresence(nowMs) != "offline"
}

@Keep
@IgnoreExtraProperties
data class TableInvite(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromPlayerCode: String = "",
    val toUid: String = "",
    val toName: String = "",
    val roomCode: String = "",
    val targetScore: Long = 152L,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val roomType: String = "public",
    val status: String = "waiting",
    val targetScore: Long = 152L,
    val memberUids: List<String> = emptyList(),
    val seat0Uid: String = "",
    val seat1Uid: String = "",
    val seat2Uid: String = "",
    val seat3Uid: String = "",
    val seat0Name: String = "",
    val seat1Name: String = "",
    val seat2Name: String = "",
    val seat3Name: String = "",
    val seat0Rating: Long = 0L,
    val seat1Rating: Long = 0L,
    val seat2Rating: Long = 0L,
    val seat3Rating: Long = 0L,
    val usScore: Long = 0L,
    val themScore: Long = 0L,
    val roundNumber: Long = 0L,
    val turnIndex: Long = 0L,
    val dealerIndex: Long = 0L,
    val gamePhase: String = "lobby",
    val gameMode: String = "none",
    val trumpSuit: String = "none",
    val bidderIndex: Long = -1L,
    val bidLevel: Long = 1L,
    val openCard: String = "",
    val hands: Map<String, List<String>> = mapOf(
        "0" to emptyList(),
        "1" to emptyList(),
        "2" to emptyList(),
        "3" to emptyList(),
    ),
    val tableCards: List<String> = emptyList(),
    val lastTrickCards: List<String> = emptyList(),
    val lastTrickWinner: Long = -1L,
    val usTricksCount: Long = 0L,
    val themTricksCount: Long = 0L,
    val usRoundPoints: Long = 0L,
    val themRoundPoints: Long = 0L,
    val usProjects: List<String> = emptyList(),
    val themProjects: List<String> = emptyList(),
    val winnerTeam: Long = -1L,
    val lastActionText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun seatUid(seatIndex: Int): String = when (seatIndex) {
        0 -> seat0Uid
        1 -> seat1Uid
        2 -> seat2Uid
        3 -> seat3Uid
        else -> ""
    }

    fun seatName(seatIndex: Int): String = when (seatIndex) {
        0 -> seat0Name
        1 -> seat1Name
        2 -> seat2Name
        3 -> seat3Name
        else -> ""
    }

    fun seatRating(seatIndex: Int): Int = when (seatIndex) {
        0 -> seat0Rating.toInt()
        1 -> seat1Rating.toInt()
        2 -> seat2Rating.toInt()
        3 -> seat3Rating.toInt()
        else -> 22
    }

    fun mySeatIndex(uid: String): Int = when (uid) {
        seat0Uid -> 0
        seat1Uid -> 1
        seat2Uid -> 2
        seat3Uid -> 3
        else -> -1
    }

    fun humanSeatCount(): Int = listOf(seat0Uid, seat1Uid, seat2Uid, seat3Uid)
        .count { it.isNotBlank() && !it.startsWith("BOT_") }
}

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String? = null,
    val mode: String = "solo",
    val usScore: Long = 0L,
    val themScore: Long = 0L,
    val didWin: Boolean = false,
    val ratingDelta: Long = 0L,
    val opponentSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TableChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val seatIndex: Int = 0,
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
