package com.example.data.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.NarratorProfile
import com.example.data.model.RawiCatalogConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsPlaybackState(
    val isPlaying: Boolean = false,
    val activeParagraphIndex: Int = -1,
    val selectedNarrator: NarratorProfile = RawiCatalogConstants.NARRATOR_PROFILES.first(),
    val customSpeedMultiplier: Float = 1.0f,
    val isReady: Boolean = false
)

class NarratorTtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var currentParagraphs: List<String> = emptyList()
    private var currentLanguage: String = "العربية"

    private val _state = MutableStateFlow(TtsPlaybackState())
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _state.value = _state.value.copy(isReady = true)
                configureUtteranceListener()
            }
        }
    }

    private fun configureUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.removePrefix("para_")?.toIntOrNull() ?: return
                _state.value = _state.value.copy(isPlaying = true, activeParagraphIndex = index)
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.removePrefix("para_")?.toIntOrNull() ?: return
                val nextIndex = index + 1
                if (nextIndex < currentParagraphs.size && _state.value.isPlaying) {
                    speakParagraphAt(nextIndex)
                } else {
                    _state.value = _state.value.copy(isPlaying = false, activeParagraphIndex = -1)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = _state.value.copy(isPlaying = false)
            }
        })
    }

    fun selectNarratorProfile(profile: NarratorProfile) {
        _state.value = _state.value.copy(selectedNarrator = profile)
        applyVoiceParams(profile, _state.value.customSpeedMultiplier, currentLanguage)
        if (_state.value.isPlaying && _state.value.activeParagraphIndex >= 0) {
            speakParagraphAt(_state.value.activeParagraphIndex)
        }
    }

    fun setCustomSpeedMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(0.6f, 2.0f)
        _state.value = _state.value.copy(customSpeedMultiplier = clamped)
        applyVoiceParams(_state.value.selectedNarrator, clamped, currentLanguage)
        if (_state.value.isPlaying && _state.value.activeParagraphIndex >= 0) {
            speakParagraphAt(_state.value.activeParagraphIndex)
        }
    }

    fun startReading(paragraphs: List<String>, startIndex: Int = 0, language: String = "العربية") {
        if (paragraphs.isEmpty()) return
        currentParagraphs = paragraphs
        currentLanguage = language
        val safeStart = startIndex.coerceIn(0, paragraphs.lastIndex)
        applyVoiceParams(_state.value.selectedNarrator, _state.value.customSpeedMultiplier, language)
        _state.value = _state.value.copy(isPlaying = true, activeParagraphIndex = safeStart)
        speakParagraphAt(safeStart)
    }

    private fun speakParagraphAt(index: Int) {
        if (index !in currentParagraphs.indices) {
            stop()
            return
        }
        _state.value = _state.value.copy(isPlaying = true, activeParagraphIndex = index)
        val engine = tts ?: return
        val params = Bundle()
        engine.speak(
            currentParagraphs[index],
            TextToSpeech.QUEUE_FLUSH,
            params,
            "para_$index"
        )
    }

    private fun applyVoiceParams(profile: NarratorProfile, speedMultiplier: Float, language: String) {
        val engine = tts ?: return
        val locale = when (language) {
            "العربية" -> Locale("ar")
            "English" -> Locale.US
            "Français" -> Locale.FRENCH
            "Español" -> Locale("es", "ES")
            "Türkçe" -> Locale("tr", "TR")
            "Deutsch" -> Locale.GERMAN
            "日本語" -> Locale.JAPANESE
            else -> Locale("ar")
        }
        try {
            engine.language = locale
        } catch (ignored: Exception) {
        }
        engine.setPitch(profile.pitch)
        engine.setSpeechRate((profile.speechRate * speedMultiplier).coerceIn(0.5f, 2.2f))
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false, activeParagraphIndex = -1)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
