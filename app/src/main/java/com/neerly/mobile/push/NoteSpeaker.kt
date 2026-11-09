package com.neerly.mobile.push

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads driver pushes aloud (NPDRing "New task, forty-two rupees, Road number twelve";
 * NPDHeadsUp note changes). The engine starts lazily and is released after speaking, so an
 * idle phone holds nothing open.
 */
@Singleton
class NoteSpeaker @Inject constructor(@ApplicationContext private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val queue = mutableListOf<String>()

    @Synchronized
    fun speak(text: String) {
        if (text.isBlank()) return
        if (ready) { say(text); return }
        queue += text
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            synchronized(this) {
                ready = status == TextToSpeech.SUCCESS
                if (!ready) { Timber.w("TTS unavailable (%d)", status); queue.clear(); return@synchronized }
                tts?.language = Locale("en", "IN")
                tts?.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                queue.forEach(::say); queue.clear()
            }
        }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "push-${text.hashCode()}")
    }

    @Synchronized
    fun stop() {
        tts?.stop(); tts?.shutdown(); tts = null; ready = false; queue.clear()
    }
}
