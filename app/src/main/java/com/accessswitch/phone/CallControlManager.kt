package com.accessswitch.phone

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-call controls: answer, end, mute, speaker, volume.
 *
 * Uses TelecomManager for call answer/end and AudioManager for audio controls.
 *
 * Note: TelecomManager.acceptRingingCall() and endCall() are deprecated at API 29
 * but still functional on current Android versions. The alternative (InCallService)
 * requires being the default phone app, which is too invasive for an accessibility app.
 */
@Singleton
class CallControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telecomManager: TelecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var isMuted = false
    private var isSpeakerOn = false

    /**
     * Answer an incoming ringing call.
     * Requires ANSWER_PHONE_CALLS permission (API 26+).
     */
    @Suppress("DEPRECATION", "MissingPermission")
    fun answerCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.acceptRingingCall()
            }
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * End the current active or ringing call.
     * Uses TelecomManager.endCall() which is deprecated at API 29
     * but still works on current versions.
     */
    @Suppress("DEPRECATION", "MissingPermission", "NewApi")
    fun endCall() {
        try {
            telecomManager.endCall()
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Toggle microphone mute during a call.
     */
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        audioManager.isMicrophoneMute = isMuted
        return isMuted
    }

    /**
     * Toggle speaker phone during a call.
     */
    fun toggleSpeaker(): Boolean {
        isSpeakerOn = !isSpeakerOn
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = isSpeakerOn
        return isSpeakerOn
    }

    /**
     * Increase call volume.
     */
    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Decrease call volume.
     */
    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Reset audio state (called when call ends).
     */
    fun resetAudioState() {
        isMuted = false
        isSpeakerOn = false
        audioManager.isMicrophoneMute = false
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
    }

    /**
     * Check if telephony is available on this device.
     * Returns false on devices without a SIM (e.g., Chromebooks).
     */
    fun isTelephonyAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_TELEPHONY
        )
    }

    /** Current mute state */
    fun isMuted(): Boolean = isMuted

    /** Current speaker state */
    fun isSpeakerOn(): Boolean = isSpeakerOn
}
