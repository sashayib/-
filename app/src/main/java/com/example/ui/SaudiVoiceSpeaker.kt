package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class SaudiVoiceSpeaker(context: Context) {

    private data class PendingUtterance(
        val spokenText: String,
        val seatIndex: Int,
        val queueAdd: Boolean,
    )

    private val appContext = context.applicationContext
    private val saudiLocale: Locale = Locale.Builder().setLanguage("ar").setRegion("SA").build()
    private val arabicLocale: Locale = Locale.Builder().setLanguage("ar").build()

    @Volatile
    private var isReady: Boolean = false

    @Volatile
    private var isReleased: Boolean = false

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            if (value) {
                pending.clear()
                runCatching { tts?.stop() }
            }
        }

    private val pending = ArrayDeque<PendingUtterance>(3)
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (isReleased) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            configureMaleSaudiVoice(engine)
            isReady = true
            flushPending()
        }
    }

    private fun configureMaleSaudiVoice(engine: TextToSpeech) {
        runCatching {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
        }

        val saudiResult = engine.setLanguage(saudiLocale)
        if (saudiResult == TextToSpeech.LANG_MISSING_DATA || saudiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            val arabicResult = engine.setLanguage(arabicLocale)
            if (arabicResult == TextToSpeech.LANG_MISSING_DATA || arabicResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Arabic TTS locale not available on device")
            }
        }

        runCatching {
            val arabicVoices = engine.voices.orEmpty()
                .filter { it.locale.language.equals("ar", ignoreCase = true) }

            val bestVoice = arabicVoices.maxByOrNull { scoreMaleSaudiVoice(it) }
            if (bestVoice != null) {
                engine.voice = bestVoice
            }
        }
    }

    private fun scoreMaleSaudiVoice(voice: Voice): Int {
        val name = voice.name.lowercase(Locale.ROOT)
        val features = voice.features.orEmpty().joinToString(" ").lowercase(Locale.ROOT)
        var score = 0

        // Prefer Saudi Arabic locale first
        if (voice.locale.country.equals("SA", ignoreCase = true)) {
            score += 40
        }
        // Prefer offline/low-latency local voices
        if (!voice.isNetworkConnectionRequired) {
            score += 20
        }

        // Strongly prefer known male Arabic/Google TTS voices
        val maleHints = listOf(
            "male", "-ard-", "-are-", "-arn-", "-arm-",
            "maged", "tarik", "hamed", "hamid", "naayf", "majed", "omar", "zayd",
        )
        val femaleHints = listOf(
            "female", "-arz-", "-arc-", "-arb-",
            "laila", "mariam", "zariyah", "salma", "fatima", "hoda", "amira",
        )

        if (maleHints.any { name.contains(it) || features.contains(it) }) {
            score += 100
        }
        if (femaleHints.any { name.contains(it) || features.contains(it) }) {
            score -= 100
        }
        return score
    }

    fun speak(phrase: String, seatIndex: Int, queueAdd: Boolean = false) {
        if (isReleased || isMuted || phrase.isBlank()) return
        val spokenText = toSaudiSpokenForm(phrase)
        if (!isReady) {
            if (!queueAdd) pending.clear()
            while (pending.size >= 3) pending.removeFirst()
            pending.addLast(PendingUtterance(spokenText, seatIndex, queueAdd))
            return
        }
        speakNow(spokenText, seatIndex, queueAdd)
    }

    fun stop() {
        pending.clear()
        runCatching { tts?.stop() }
    }

    fun release() {
        isReleased = true
        isReady = false
        pending.clear()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun flushPending() {
        while (pending.isNotEmpty() && !isMuted && !isReleased) {
            val item = pending.removeFirst()
            speakNow(item.spokenText, item.seatIndex, item.queueAdd)
        }
    }

    private fun speakNow(spokenText: String, seatIndex: Int, queueAdd: Boolean) {
        val engine = tts ?: return
        if (isReleased || isMuted) return
        // Distinct masculine vocal pitches across the 4 seats
        val pitch = when (seatIndex.mod(4)) {
            0 -> 0.84f
            1 -> 0.78f
            2 -> 0.89f
            else -> 0.74f
        }
        val isShortCall = spokenText.length <= 12
        val speechRate = if (isShortCall) 1.04f else 0.98f
        runCatching {
            engine.setPitch(pitch)
            engine.setSpeechRate(speechRate)
            val mode = if (queueAdd) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
            engine.speak(spokenText, mode, null, "shaddaha_tts_${System.nanoTime()}")
        }
    }

    private fun toSaudiSpokenForm(phrase: String): String = when (phrase.trim()) {
        "حكم" -> "حُكُمْ"
        "صن" -> "صَنْ"
        "بس" -> "بَسْ"
        "ولا" -> "وَلَا"
        "وحش يا خوي" -> "وَحْش يَا خُوي"
        else -> phrase
    }

    private companion object {
        const val TAG = "SaudiVoiceSpeaker"
    }
}
