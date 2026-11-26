package com.example.game

enum class Suit(
    val code: String,
    val symbol: String,
    val arabicName: String,
    val isRed: Boolean,
) {
    SPADES("spades", "♠", "سبيت", false),
    HEARTS("hearts", "♥", "هاص", true),
    DIAMONDS("diamonds", "♦", "ديمن", true),
    CLUBS("clubs", "♣", "شيريا", false);

    companion object {
        fun fromCode(raw: String): Suit? = entries.firstOrNull { it.code == raw }
        fun fromChar(ch: Char): Suit? = when (ch) {
            'S' -> SPADES
            'H' -> HEARTS
            'D' -> DIAMONDS
            'C' -> CLUBS
            else -> null
        }
    }

    fun shortChar(): Char = when (this) {
        SPADES -> 'S'
        HEARTS -> 'H'
        DIAMONDS -> 'D'
        CLUBS -> 'C'
    }
}

enum class Rank(
    val code: String,
    val label: String,
    val sunOrder: Int,
    val hokmOrder: Int,
    val sunPoints: Int,
    val hokmPoints: Int,
    val sequenceOrder: Int,
) {
    SEVEN("7", "7", 0, 0, 0, 0, 0),
    EIGHT("8", "8", 1, 1, 0, 0, 1),
    NINE("9", "9", 2, 6, 0, 14, 2),
    TEN("10", "10", 6, 4, 10, 10, 3),
    JACK("J", "J", 3, 7, 2, 20, 4),
    QUEEN("Q", "Q", 4, 2, 3, 3, 5),
    KING("K", "K", 5, 3, 4, 4, 6),
    ACE("A", "A", 7, 5, 11, 11, 7);

    companion object {
        fun fromCode(raw: String): Rank? = entries.firstOrNull { it.code == raw }
    }
}

data class BalootCard(
    val rank: Rank,
    val suit: Suit,
) {
    val code: String get() = "${rank.code}${suit.shortChar()}"

    companion object {
        fun parse(raw: String): BalootCard? {
            if (raw.length < 2) return null
            val suitChar = raw.last()
            val rankStr = raw.dropLast(1)
            val suit = Suit.fromChar(suitChar) ?: return null
            val rank = Rank.fromCode(rankStr) ?: return null
            return BalootCard(rank, suit)
        }
    }
}

enum class GameMode(val code: String, val arabicName: String) {
    NONE("none", "بدون"),
    SUN("sun", "صن"),
    HOKM("hokm", "حكم");

    companion object {
        fun fromCode(raw: String): GameMode = entries.firstOrNull { it.code == raw } ?: NONE
    }
}

enum class GamePhase(val code: String) {
    LOBBY("lobby"),
    BIDDING_R1("bidding_r1"),
    BIDDING_R2("bidding_r2"),
    DOUBLING("doubling"),
    PLAYING("playing"),
    ROUND_SUMMARY("round_summary"),
    MATCH_FINISHED("match_finished");

    companion object {
        fun fromCode(raw: String): GamePhase = entries.firstOrNull { it.code == raw } ?: LOBBY
    }
}

enum class ProjectKind(
    val arabicName: String,
    val rawPoints: Int,
    val sunAbnat: Int,
    val hokmAbnat: Int,
) {
    SIRA("سرا", 20, 4, 2),
    FIFTY("خمسين", 50, 10, 5),
    HUNDRED("مية", 100, 20, 10),
    FOUR_HUNDRED("أربعمية", 400, 40, 20),
    BALOOT("بلوت", 20, 0, 2);
}

data class DetectedProject(
    val kind: ProjectKind,
    val seatIndex: Int,
    val description: String,
)

data class PlayedCardEntry(
    val seatIndex: Int,
    val card: BalootCard,
) {
    fun serialize(): String = "$seatIndex:${card.code}"

    companion object {
        fun parse(raw: String): PlayedCardEntry? {
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val seat = parts[0].toIntOrNull() ?: return null
            val card = BalootCard.parse(parts[1]) ?: return null
            return PlayedCardEntry(seat, card)
        }
    }
}

data class BalootSeat(
    val seatIndex: Int,
    val uid: String,
    val name: String,
    val rating: Int,
    val isBot: Boolean,
)

data class RoundSummaryInfo(
    val roundNumber: Int,
    val mode: GameMode,
    val trumpSuit: Suit?,
    val bidderSeat: Int,
    val bidLevel: Int,
    val usAbnat: Int,
    val themAbnat: Int,
    val usCardPoints: Int,
    val themCardPoints: Int,
    val usProjects: List<String>,
    val themProjects: List<String>,
    val isKaboot: Boolean,
    val isKhassara: Boolean,
    val buyingTeam: Int,
)

data class BalootTableState(
    val roomCode: String?,
    val isOnline: Boolean,
    val hostId: String,
    val targetScore: Int,
    val mySeatIndex: Int,
    val seats: List<BalootSeat>,
    val usScore: Int,
    val themScore: Int,
    val roundNumber: Int,
    val turnIndex: Int,
    val dealerIndex: Int,
    val phase: GamePhase,
    val mode: GameMode,
    val trumpSuit: Suit?,
    val bidderIndex: Int,
    val bidLevel: Int,
    val openCard: BalootCard?,
    val hands: Map<Int, List<BalootCard>>,
    val tableCards: List<PlayedCardEntry>,
    val lastTrickCards: List<PlayedCardEntry>,
    val lastTrickWinner: Int,
    val usTricksCount: Int,
    val themTricksCount: Int,
    val usRoundPoints: Int,
    val themRoundPoints: Int,
    val usProjects: List<String>,
    val themProjects: List<String>,
    val winnerTeam: Int,
    val lastActionText: String,
    val biddingPasses: Int = 0,
    val highestBidSeat: Int = -1,
    val highestBidMode: GameMode = GameMode.NONE,
    val highestBidSuit: Suit? = null,
)
