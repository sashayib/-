package com.example.game

import kotlin.math.roundToInt

object BalootEngine {

    val quickReactionEmojis = listOf(
        "😂",
        "🔥",
        "👏",
        "😎",
        "🤩",
        "😮",
        "😅",
        "😢",
        "😡",
        "👍",
        "❤️",
        "👑",
    )

    val quickChatPhrases = listOf(
        "كفو يا خوي!",
        "أبشر بعزك",
        "ارمِ حكم!",
        "صن يا وحش!",
        "الله يعين!",
        "سلمت يدك",
    )

    fun rankTierForRating(rating: Int): String = when {
        rating >= 18000 -> "ملك البلوت"
        rating >= 16000 -> "زعيم"
        rating >= 14000 -> "نوخذة"
        rating >= 12000 -> "ذيبان"
        rating >= 10000 -> "صنديد"
        rating >= 8000 -> "متمرس"
        rating >= 6000 -> "لاعب"
        rating >= 4000 -> "هاوي"
        rating >= 2000 -> "ناشئ"
        else -> "مبتدئ"
    }

    fun calculateMatchPointsDelta(didWin: Boolean, currentLossStreak: Int): Int {
        return if (didWin) {
            22
        } else {
            val consecutiveLosses = (currentLossStreak + 1).coerceAtLeast(1)
            -(7 * consecutiveLosses)
        }
    }

    fun calculateEloDelta(playerRating: Int, avgOpponentRating: Int, didWin: Boolean): Int {
        val expected = 1.0 / (1.0 + Math.pow(10.0, (avgOpponentRating - playerRating) / 400.0))
        val actual = if (didWin) 1.0 else 0.0
        val rawDelta = (36.0 * (actual - expected)).roundToInt()
        return if (didWin) rawDelta.coerceIn(14, 42) else rawDelta.coerceIn(-35, -10)
    }

    fun fullDeck(): List<BalootCard> {
        val cards = mutableListOf<BalootCard>()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(BalootCard(rank, suit))
            }
        }
        return cards
    }

    fun dealNewRound(
        dealerIndex: Int,
        roundNumber: Int,
        seats: List<BalootSeat>,
        existingUsScore: Int,
        existingThemScore: Int,
        targetScore: Int,
        roomCode: String?,
        isOnline: Boolean,
        hostId: String,
        mySeatIndex: Int,
    ): BalootTableState {
        val deck = fullDeck().shuffled()
        val hands = mutableMapOf<Int, List<BalootCard>>()
        for (seat in 0..3) {
            val start = seat * 5
            hands[seat] = sortHand(deck.subList(start, start + 5), GameMode.SUN, null)
        }
        val openCard = deck[20]
        val firstTurn = (dealerIndex + 1) % 4
        return BalootTableState(
            roomCode = roomCode,
            isOnline = isOnline,
            hostId = hostId,
            targetScore = targetScore,
            mySeatIndex = mySeatIndex,
            seats = seats,
            usScore = existingUsScore,
            themScore = existingThemScore,
            roundNumber = roundNumber,
            turnIndex = firstTurn,
            dealerIndex = dealerIndex,
            phase = GamePhase.BIDDING_R1,
            mode = GameMode.NONE,
            trumpSuit = null,
            bidderIndex = -1,
            bidLevel = 1,
            openCard = openCard,
            hands = hands,
            tableCards = emptyList(),
            lastTrickCards = emptyList(),
            lastTrickWinner = -1,
            usTricksCount = 0,
            themTricksCount = 0,
            usRoundPoints = 0,
            themRoundPoints = 0,
            usProjects = emptyList(),
            themProjects = emptyList(),
            winnerTeam = -1,
            lastActionText = "توزيع الصكة رقم $roundNumber — الورقة المكشوفة ${openCard.rank.label}${openCard.suit.symbol}",
            biddingPasses = 0,
            highestBidSeat = -1,
            highestBidMode = GameMode.NONE,
            highestBidSuit = null,
        )
    }

    fun sortHand(hand: List<BalootCard>, mode: GameMode, trumpSuit: Suit?): List<BalootCard> {
        val suitOrder = mapOf(
            Suit.SPADES to 0,
            Suit.HEARTS to 1,
            Suit.CLUBS to 2,
            Suit.DIAMONDS to 3,
        )
        return hand.sortedWith(
            compareBy<BalootCard>(
                { if (mode == GameMode.HOKM && it.suit == trumpSuit) -1 else (suitOrder[it.suit] ?: 0) },
                { cardStrength(it, mode, trumpSuit) },
            )
        )
    }

    fun cardStrength(card: BalootCard, mode: GameMode, trumpSuit: Suit?): Int {
        return if (mode == GameMode.HOKM && card.suit == trumpSuit) {
            card.rank.hokmOrder
        } else {
            card.rank.sunOrder
        }
    }

    fun cardPoints(card: BalootCard, mode: GameMode, trumpSuit: Suit?): Int {
        return if (mode == GameMode.HOKM && card.suit == trumpSuit) {
            card.rank.hokmPoints
        } else {
            card.rank.sunPoints
        }
    }

    fun completeDealAfterBid(
        state: BalootTableState,
        buyerSeat: Int,
        mode: GameMode,
        trumpSuit: Suit?,
    ): BalootTableState {
        val currentDealt = state.hands.values.flatten().toMutableSet()
        state.openCard?.let { currentDealt.add(it) }
        val remainingDeck = fullDeck().filter { it !in currentDealt }.shuffled()
        var cursor = 0
        val newHands = mutableMapOf<Int, List<BalootCard>>()
        for (seat in 0..3) {
            val existing = state.hands[seat].orEmpty().toMutableList()
            if (seat == buyerSeat) {
                state.openCard?.let { existing.add(it) }
                repeat(2) {
                    if (cursor < remainingDeck.size) existing.add(remainingDeck[cursor++])
                }
            } else {
                repeat(3) {
                    if (cursor < remainingDeck.size) existing.add(remainingDeck[cursor++])
                }
            }
            newHands[seat] = sortHand(existing, mode, trumpSuit)
        }

        // Detect projects: auto-declare for bots (and Baloot K+Q); human players choose Sira/Fifty/Hundred/400 before playing
        val autoProjects = (0..3).flatMap { seat ->
            val detected = detectProjectsForHand(newHands[seat].orEmpty(), seat, mode, trumpSuit)
            val isBotSeat = state.seats.getOrNull(seat)?.isBot == true
            if (isBotSeat) {
                detected
            } else {
                detected.filter { it.kind == ProjectKind.BALOOT }
            }
        }
        val usProj = autoProjects.filter { it.seatIndex % 2 == 0 }.map { it.description }.take(8)
        val themProj = autoProjects.filter { it.seatIndex % 2 == 1 }.map { it.description }.take(8)

        val buyerName = state.seats.getOrNull(buyerSeat)?.name ?: "لاعب"
        val contractDesc = if (mode == GameMode.SUN) "صن" else "حكم ${trumpSuit?.symbol.orEmpty()}"
        val firstDefender = (buyerSeat + 1) % 4

        return state.copy(
            phase = GamePhase.DOUBLING,
            mode = mode,
            trumpSuit = trumpSuit,
            bidderIndex = buyerSeat,
            bidLevel = 1,
            openCard = null,
            hands = newHands,
            usProjects = usProj,
            themProjects = themProj,
            turnIndex = firstDefender,
            lastActionText = "$buyerName اشترى $contractDesc",
        )
    }

    fun detectDeclarableProjectsForHand(
        hand: List<BalootCard>,
        seatIndex: Int,
        mode: GameMode,
        trumpSuit: Suit?,
    ): List<DetectedProject> {
        return detectProjectsForHand(hand, seatIndex, mode, trumpSuit)
            .filter {
                it.kind == ProjectKind.SIRA ||
                    it.kind == ProjectKind.FIFTY ||
                    it.kind == ProjectKind.HUNDRED ||
                    it.kind == ProjectKind.FOUR_HUNDRED
            }
    }

    fun detectProjectsForHand(
        hand: List<BalootCard>,
        seatIndex: Int,
        mode: GameMode,
        trumpSuit: Suit?,
    ): List<DetectedProject> {
        val result = mutableListOf<DetectedProject>()
        // 1. Baloot (K + Q of trump in Hokm)
        if (mode == GameMode.HOKM && trumpSuit != null) {
            val hasK = hand.any { it.suit == trumpSuit && it.rank == Rank.KING }
            val hasQ = hand.any { it.suit == trumpSuit && it.rank == Rank.QUEEN }
            if (hasK && hasQ) {
                result.add(DetectedProject(ProjectKind.BALOOT, seatIndex, "بلوت"))
            }
        }
        // 2. Four of a kind
        val byRank = hand.groupBy { it.rank }
        for ((rank, cards) in byRank) {
            if (cards.size == 4) {
                when (rank) {
                    Rank.ACE -> if (mode == GameMode.SUN) {
                        result.add(DetectedProject(ProjectKind.FOUR_HUNDRED, seatIndex, "أربعمية"))
                    } else {
                        result.add(DetectedProject(ProjectKind.HUNDRED, seatIndex, "مية (إكة)"))
                    }
                    Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN -> {
                        result.add(DetectedProject(ProjectKind.HUNDRED, seatIndex, "مية (${rank.label})"))
                    }
                    else -> Unit
                }
            }
        }
        // 3. Sequences (3+ consecutive in same suit)
        val bySuit = hand.groupBy { it.suit }
        for ((_, suitCards) in bySuit) {
            val sorted = suitCards.sortedBy { it.rank.sequenceOrder }
            var runLen = 1
            for (i in 1 until sorted.size) {
                if (sorted[i].rank.sequenceOrder == sorted[i - 1].rank.sequenceOrder + 1) {
                    runLen++
                } else {
                    addSequenceProject(runLen, seatIndex, result)
                    runLen = 1
                }
            }
            addSequenceProject(runLen, seatIndex, result)
        }
        return result
    }

    private fun addSequenceProject(
        runLen: Int,
        seatIndex: Int,
        out: MutableList<DetectedProject>,
    ) {
        when {
            runLen >= 5 -> out.add(DetectedProject(ProjectKind.HUNDRED, seatIndex, "مية (تسلسل ٥)"))
            runLen == 4 -> out.add(DetectedProject(ProjectKind.FIFTY, seatIndex, "خمسين"))
            runLen == 3 -> out.add(DetectedProject(ProjectKind.SIRA, seatIndex, "سرا"))
        }
    }

    fun projectAbnat(desc: String, mode: GameMode): Int {
        return when {
            desc.startsWith("أربعمية") -> if (mode == GameMode.SUN) 40 else 20
            desc.startsWith("مية") -> if (mode == GameMode.SUN) 20 else 10
            desc.startsWith("خمسين") -> if (mode == GameMode.SUN) 10 else 5
            desc.startsWith("سرا") -> if (mode == GameMode.SUN) 4 else 2
            desc == "بلوت" -> if (mode == GameMode.HOKM) 2 else 0
            else -> 0
        }
    }

    fun getLegalCards(
        hand: List<BalootCard>,
        tableCards: List<PlayedCardEntry>,
        mode: GameMode,
        trumpSuit: Suit?,
        seatIndex: Int,
    ): List<BalootCard> {
        if (tableCards.isEmpty()) return hand
        val ledSuit = tableCards.first().card.suit
        val sameSuit = hand.filter { it.suit == ledSuit }
        if (sameSuit.isNotEmpty()) {
            // In Hokm, if led suit is trump, must over-trump if possible
            if (mode == GameMode.HOKM && ledSuit == trumpSuit) {
                val highestTrumpOnTable = tableCards
                    .filter { it.card.suit == trumpSuit }
                    .maxOfOrNull { it.card.hokmOrderOrZero() } ?: -1
                val higherTrumps = sameSuit.filter { it.rank.hokmOrder > highestTrumpOnTable }
                if (higherTrumps.isNotEmpty()) return higherTrumps
            }
            return sameSuit
        }

        // Void in led suit
        if (mode == GameMode.HOKM && trumpSuit != null) {
            val currentWinner = determineTrickWinner(tableCards, mode, trumpSuit)
            val partnerWinning = currentWinner >= 0 && (currentWinner % 2 == seatIndex % 2)
            val trumpsInHand = hand.filter { it.suit == trumpSuit }
            if (!partnerWinning && trumpsInHand.isNotEmpty()) {
                val highestTrumpOnTable = tableCards
                    .filter { it.card.suit == trumpSuit }
                    .maxOfOrNull { it.card.hokmOrderOrZero() } ?: -1
                val higherTrumps = trumpsInHand.filter { it.rank.hokmOrder > highestTrumpOnTable }
                return if (higherTrumps.isNotEmpty()) higherTrumps else trumpsInHand
            }
        }
        return hand
    }

    private fun BalootCard.hokmOrderOrZero(): Int = rank.hokmOrder

    fun determineTrickWinner(
        tableCards: List<PlayedCardEntry>,
        mode: GameMode,
        trumpSuit: Suit?,
    ): Int {
        if (tableCards.isEmpty()) return -1
        val ledSuit = tableCards.first().card.suit
        if (mode == GameMode.HOKM && trumpSuit != null) {
            val trumps = tableCards.filter { it.card.suit == trumpSuit }
            if (trumps.isNotEmpty()) {
                return trumps.maxBy { it.card.rank.hokmOrder }.seatIndex
            }
        }
        return tableCards
            .filter { it.card.suit == ledSuit }
            .maxBy { it.card.rank.sunOrder }
            .seatIndex
    }

    fun applyCardPlay(state: BalootTableState, seatIndex: Int, card: BalootCard): BalootTableState {
        val hand = state.hands[seatIndex].orEmpty()
        val updatedHand = hand.filterNot { it == card }
        val newHands = state.hands.toMutableMap().apply { put(seatIndex, updatedHand) }
        val newTable = state.tableCards + PlayedCardEntry(seatIndex, card)
        val playerName = state.seats.getOrNull(seatIndex)?.name ?: "لاعب"

        if (newTable.size < 4) {
            return state.copy(
                hands = newHands,
                tableCards = newTable,
                turnIndex = (seatIndex + 1) % 4,
                lastActionText = "$playerName لعب ${card.rank.label}${card.suit.symbol}",
            )
        }

        // Trick complete (4 cards)
        val winnerSeat = determineTrickWinner(newTable, state.mode, state.trumpSuit)
        val trickRawPoints = newTable.sumOf { cardPoints(it.card, state.mode, state.trumpSuit) }
        val isLastTrick = newHands.values.all { it.isEmpty() }
        val bonusLastTrick = if (isLastTrick) 10 else 0
        val winnerTeam = winnerSeat % 2
        val newUsPoints = state.usRoundPoints + if (winnerTeam == 0) trickRawPoints + bonusLastTrick else 0
        val newThemPoints = state.themRoundPoints + if (winnerTeam == 1) trickRawPoints + bonusLastTrick else 0
        val newUsTricks = state.usTricksCount + if (winnerTeam == 0) 1 else 0
        val newThemTricks = state.themTricksCount + if (winnerTeam == 1) 1 else 0
        val winnerName = state.seats.getOrNull(winnerSeat)?.name ?: "الفائز"

        if (!isLastTrick) {
            return state.copy(
                hands = newHands,
                tableCards = emptyList(),
                lastTrickCards = newTable,
                lastTrickWinner = winnerSeat,
                turnIndex = winnerSeat,
                usRoundPoints = newUsPoints,
                themRoundPoints = newThemPoints,
                usTricksCount = newUsTricks,
                themTricksCount = newThemTricks,
                lastActionText = "$winnerName أكل اللفة (+${trickRawPoints})",
            )
        }

        // Round complete! Calculate Abnat, Kaboot, Khassara, Projects, and Doubling
        val buyingTeam = if (state.bidderIndex >= 0) state.bidderIndex % 2 else 0
        val usBaloot = if (state.usProjects.contains("بلوت") && state.mode == GameMode.HOKM) 2 else 0
        val themBaloot = if (state.themProjects.contains("بلوت") && state.mode == GameMode.HOKM) 2 else 0
        val usOtherProj = state.usProjects.filterNot { it == "بلوت" }.sumOf { projectAbnat(it, state.mode) }
        val themOtherProj = state.themProjects.filterNot { it == "بلوت" }.sumOf { projectAbnat(it, state.mode) }

        val isKabootUs = newUsTricks == 8
        val isKabootThem = newThemTricks == 8
        val baseTotalAbnat = if (state.mode == GameMode.SUN) 26 else 16

        var usAbnat: Int
        var themAbnat: Int
        var isKhassara = false

        if (isKabootUs) {
            usAbnat = (if (state.mode == GameMode.SUN) 44 else 25) + usOtherProj + usBaloot
            themAbnat = themBaloot
        } else if (isKabootThem) {
            themAbnat = (if (state.mode == GameMode.SUN) 44 else 25) + themOtherProj + themBaloot
            usAbnat = usBaloot
        } else {
            val usCardAbnat = convertRawToAbnat(newUsPoints, state.mode)
            val themCardAbnat = baseTotalAbnat - usCardAbnat
            val usTotalRawCompare = usCardAbnat + usOtherProj
            val themTotalRawCompare = themCardAbnat + themOtherProj

            if (buyingTeam == 0 && usTotalRawCompare < themTotalRawCompare) {
                // Team Us bought and lost (Khassara)
                isKhassara = true
                usAbnat = usBaloot
                themAbnat = (baseTotalAbnat + usOtherProj + themOtherProj) * state.bidLevel + themBaloot
            } else if (buyingTeam == 1 && themTotalRawCompare < usTotalRawCompare) {
                // Team Them bought and lost (Khassara)
                isKhassara = true
                themAbnat = themBaloot
                usAbnat = (baseTotalAbnat + usOtherProj + themOtherProj) * state.bidLevel + usBaloot
            } else {
                usAbnat = (usCardAbnat + usOtherProj) * state.bidLevel + usBaloot
                themAbnat = (themCardAbnat + themOtherProj) * state.bidLevel + themBaloot
            }
        }

        // Handle Qahwa (bidLevel == 4)
        if (state.bidLevel >= 4 && !isKabootUs && !isKabootThem) {
            if (usAbnat > themAbnat) usAbnat = state.targetScore else themAbnat = state.targetScore
        }

        val updatedUsScore = state.usScore + usAbnat
        val updatedThemScore = state.themScore + themAbnat
        val matchFinished = updatedUsScore >= state.targetScore || updatedThemScore >= state.targetScore
        val finalWinner = if (!matchFinished) -1 else if (updatedUsScore >= updatedThemScore) 0 else 1

        return state.copy(
            hands = newHands,
            tableCards = emptyList(),
            lastTrickCards = newTable,
            lastTrickWinner = winnerSeat,
            turnIndex = winnerSeat,
            usRoundPoints = newUsPoints,
            themRoundPoints = newThemPoints,
            usTricksCount = newUsTricks,
            themTricksCount = newThemTricks,
            usScore = updatedUsScore,
            themScore = updatedThemScore,
            phase = if (matchFinished) GamePhase.MATCH_FINISHED else GamePhase.ROUND_SUMMARY,
            winnerTeam = finalWinner,
            lastActionText = when {
                isKabootUs -> "كبوت لصالح فريقنا! (+$usAbnat)"
                isKabootThem -> "كبوت لصالح الخصم! (+$themAbnat)"
                isKhassara -> "خسارة المشتري!"
                else -> "انتهت الصكة: لنا +$usAbnat | لهم +$themAbnat"
            },
        )
    }

    private fun convertRawToAbnat(rawPoints: Int, mode: GameMode): Int {
        return if (mode == GameMode.SUN) {
            ((rawPoints * 2) + 5) / 10
        } else {
            (rawPoints + 5) / 10
        }
    }

    // --- Smart AI Bot Logic ---
    enum class BotBidChoice { PASS, HOKM_OPEN, HOKM_SUIT, SUN }

    data class BotBidDecision(
        val choice: BotBidChoice,
        val suit: Suit? = null,
    )

    fun chooseBotBid(
        hand: List<BalootCard>,
        openCard: BalootCard?,
        isRound2: Boolean,
        currentHighestMode: GameMode,
    ): BotBidDecision {
        // Count strong Sun cards (A, 10)
        val sunPower = hand.sumOf {
            when (it.rank) {
                Rank.ACE -> 3
                Rank.TEN -> 2
                Rank.KING -> 1
                else -> 0
            }
        }
        if (sunPower >= 6 && currentHighestMode != GameMode.SUN) {
            return BotBidDecision(BotBidChoice.SUN)
        }
        if (!isRound2 && openCard != null && currentHighestMode == GameMode.NONE) {
            val openSuitCount = hand.count { it.suit == openCard.suit }
            val hasJackOrNine = hand.any {
                it.suit == openCard.suit && (it.rank == Rank.JACK || it.rank == Rank.NINE)
            }
            if (openSuitCount >= 2 || (hasJackOrNine && openSuitCount >= 1) || openCard.rank == Rank.JACK) {
                return BotBidDecision(BotBidChoice.HOKM_OPEN, openCard.suit)
            }
        }
        if (isRound2 && currentHighestMode == GameMode.NONE) {
            val bestSuit = Suit.entries
                .filter { it != openCard?.suit }
                .maxByOrNull { suit ->
                    hand.filter { it.suit == suit }.sumOf { it.rank.hokmOrder + 2 }
                }
            if (bestSuit != null) {
                val countInSuit = hand.count { it.suit == bestSuit }
                val hasStrongTrump = hand.any {
                    it.suit == bestSuit && (it.rank == Rank.JACK || it.rank == Rank.NINE || it.rank == Rank.ACE)
                }
                if (countInSuit >= 3 || (countInSuit >= 2 && hasStrongTrump)) {
                    return BotBidDecision(BotBidChoice.HOKM_SUIT, bestSuit)
                }
            }
        }
        return BotBidDecision(BotBidChoice.PASS)
    }

    fun chooseBotDoubling(hand: List<BalootCard>, mode: GameMode, trumpSuit: Suit?): Boolean {
        val power = if (mode == GameMode.SUN) {
            hand.count { it.rank == Rank.ACE || it.rank == Rank.TEN }
        } else {
            hand.count { it.suit == trumpSuit && (it.rank == Rank.JACK || it.rank == Rank.NINE || it.rank == Rank.ACE) }
        }
        return power >= 4
    }

    fun chooseBotCard(
        hand: List<BalootCard>,
        tableCards: List<PlayedCardEntry>,
        mode: GameMode,
        trumpSuit: Suit?,
        seatIndex: Int,
    ): BalootCard {
        val legal = getLegalCards(hand, tableCards, mode, trumpSuit, seatIndex)
        if (legal.size == 1) return legal.first()

        // Leading a trick: lead Aces in Sun or strong trumps/aces in Hokm
        if (tableCards.isEmpty()) {
            val strongLead = legal.maxByOrNull { cardStrength(it, mode, trumpSuit) }
            return strongLead ?: legal.first()
        }

        val currentWinner = determineTrickWinner(tableCards, mode, trumpSuit)
        val partnerWinning = currentWinner >= 0 && (currentWinner % 2 == seatIndex % 2)
        return if (partnerWinning) {
            // Partner is winning: feed safe medium/high non-trump points or lowest card
            val nonTrumpHigh = legal.filter { !(mode == GameMode.HOKM && it.suit == trumpSuit) }
                .maxByOrNull { cardPoints(it, mode, trumpSuit) }
            if (tableCards.size == 3 && nonTrumpHigh != null && cardPoints(nonTrumpHigh, mode, trumpSuit) >= 10) {
                nonTrumpHigh
            } else {
                legal.minBy { cardStrength(it, mode, trumpSuit) }
            }
        } else {
            // Opponent winning: try to win cheaply with smallest winning card, otherwise throw lowest
            val winningCandidates = legal.filter { candidate ->
                val hypothetical = tableCards + PlayedCardEntry(seatIndex, candidate)
                determineTrickWinner(hypothetical, mode, trumpSuit) == seatIndex
            }
            if (winningCandidates.isNotEmpty()) {
                winningCandidates.minBy { cardStrength(it, mode, trumpSuit) }
            } else {
                legal.minBy { cardPoints(it, mode, trumpSuit) * 10 + cardStrength(it, mode, trumpSuit) }
            }
        }
    }
}
