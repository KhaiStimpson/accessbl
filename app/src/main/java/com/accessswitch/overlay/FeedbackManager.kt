package com.accessswitch.overlay

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages haptic and audio feedback for scanning events.
 *
 * - Haptic buzz on item selection
 * - Short click tone on highlight advance (when audio feedback enabled)
 */
@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    /**
     * Called when the scan highlight advances to the next item.
     * Plays a short click tone if audio feedback is enabled.
     */
    fun onHighlightAdvance() {
        val settings = settingsRepository.currentSettings
        if (settings.audioFeedback) {
            playTick()
        }
    }

    /**
     * Called when an item is selected (switch press in auto/step/inverse mode).
     * Provides haptic feedback if enabled.
     */
    fun onItemSelected() {
        val settings = settingsRepository.currentSettings
        if (settings.hapticFeedback) {
            vibrateSelection()
        }
        if (settings.audioFeedback) {
            playSelect()
        }
    }

    /**
     * Short vibration for selection confirmation.
     */
    private fun vibrateSelection() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    SELECTION_VIBRATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(SELECTION_VIBRATION_MS)
        }
    }

    /**
     * Short click tone for highlight advance.
     */
    private fun playTick() {
        try {
            ensureToneGenerator()
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TICK_TONE_MS)
        } catch (_: Exception) {
            // Tone generation can fail on some devices
        }
    }

    /**
     * Selection confirmation tone.
     */
    private fun playSelect() {
        try {
            ensureToneGenerator()
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, SELECT_TONE_MS)
        } catch (_: Exception) {
            // Tone generation can fail on some devices
        }
    }

    private fun ensureToneGenerator() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, TONE_VOLUME)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val SELECTION_VIBRATION_MS = 50L
        private const val TICK_TONE_MS = 30
        private const val SELECT_TONE_MS = 80
        private const val TONE_VOLUME = 50 // 0-100
    }
}
