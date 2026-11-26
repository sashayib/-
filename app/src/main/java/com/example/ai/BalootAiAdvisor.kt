package com.example.ai

import com.example.game.BalootEngine
import com.example.game.BalootTableState
import com.example.game.GameMode
import com.example.game.GamePhase
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AiCoachUiState {
    data object Idle : AiCoachUiState
    data object Loading : AiCoachUiState
    data class Streaming(val text: String) : AiCoachUiState
    data class Success(val text: String) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}

class BalootAiAdvisor {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "أنت خبير ومستشار بلوت سعودي محترف في تطبيق شدها. " +
                        "أجب دائماً بالعربية الواضحة والمختصرة وبأسلوب ودي، " +
                        "واشرح القرار الأنسب وفق قوانين البلوت (الصن والحكم والمشاريع والأكل)."
                )
            },
        )
    }

    fun streamHandAdvice(table: BalootTableState): Flow<String> {
        val mySeat = table.mySeatIndex
        val myHand = table.hands[mySeat].orEmpty()
        val handStr = myHand.joinToString("، ") { "${it.rank.label}${it.suit.symbol}(${it.suit.arabicName})" }
        val tableStr = if (table.tableCards.isEmpty()) {
            "الطاولة فارغة (أنت البادئ)"
        } else {
            table.tableCards.joinToString("، ") {
                val seatName = table.seats.getOrNull(it.seatIndex)?.name ?: "لاعب"
                "$seatName: ${it.card.rank.label}${it.card.suit.symbol}"
            }
        }
        val legalStr = if (table.phase == GamePhase.PLAYING) {
            BalootEngine.getLegalCards(myHand, table.tableCards, table.mode, table.trumpSuit, mySeat)
                .joinToString("، ") { "${it.rank.label}${it.suit.symbol}" }
        } else {
            "مرحلة المشترى/التدبيل"
        }
        val modeLabel = when (table.mode) {
            GameMode.SUN -> "صن"
            GameMode.HOKM -> "حكم (${table.trumpSuit?.arabicName.orEmpty()} ${table.trumpSuit?.symbol.orEmpty()})"
            GameMode.NONE -> "لم يتحدد بعد"
        }
        val openCardDesc = table.openCard?.let { "${it.rank.label}${it.suit.symbol} (${it.suit.arabicName})" } ?: "لا يوجد"
        val myTeam = mySeat % 2
        val ourScore = if (myTeam == 0) table.usScore else table.themScore
        val oppScore = if (myTeam == 0) table.themScore else table.usScore

        val prompt = """
            حلّل موقفي الحالي في لعبة البلوت واقترح أفضل خطوة في 3 إلى 4 أسطر فقط:
            - المرحلة: ${table.phase.code}
            - نوع اللعب: $modeLabel
            - الورقة المكشوفة: $openCardDesc
            - أوراقي الحالية: $handStr
            - الأوراق الملعوبة على الطاولة: $tableStr
            - الأوراق النظامية المسموح لعبها: $legalStr
            - النتيجة الحالية: لنا $ourScore - لهم $oppScore (الهدف ${table.targetScore})
            ما هو القرار الأفضل ولماذا؟
        """.trimIndent()

        return model.generateContentStream(prompt).map { it.text.orEmpty() }
    }

    fun streamRulesQuestion(question: String): Flow<String> {
        val prompt = "سؤال من لاعب بلوت: $question\nأجب بدقة واختصار وفق قواعد البلوت المعتمدة."
        return model.generateContentStream(prompt).map { it.text.orEmpty() }
    }
}
