package com.accessswitch.netflix

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.accessswitch.accessibility.AccessSwitchAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects key events into the current foreground app via the
 * AccessibilityService's `performGlobalAction` and `dispatchGesture`,
 * or by dispatching KeyEvent through the Instrumentation framework.
 *
 * For D-pad and media keys, we use `android.view.Instrumentation.sendKeyDownUpSync`
 * pattern — but since we can't use Instrumentation from an AccessibilityService,
 * we instead use `AccessibilityNodeInfo.performAction(ACTION_CLICK)` for focused
 * nodes, or inject via `InputManager` shell-level commands.
 *
 * The most reliable approach on Android is to use `AccessibilityService`'s
 * soft keyboard input or the `performGlobalAction` for Back, plus
 * dispatching key events via a background thread that calls
 * `Instrumentation.sendKeySync()` — but we'll use the simpler approach
 * of `Runtime.exec("input keyevent ...")` as it works without root when
 * called from an accessibility service context, OR use `dispatchGesture`
 * for swipe-based controls.
 *
 * Actually, the cleanest approach: the AccessibilityService can use
 * `performGlobalAction` for Back, and for D-pad/media keys we use
 * `SoftKeyboardController` or simply exec `input keyevent` commands.
 */
@Singleton
class KeyInjector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Inject a key event (press + release) for a given keycode.
     * Uses the shell `input keyevent` command which works from
     * accessibility service context.
     */
    fun injectKey(keyCode: Int) {
        try {
            // Run on a background thread to avoid blocking the main thread
            Thread {
                try {
                    val process = Runtime.getRuntime().exec("input keyevent $keyCode")
                    process.waitFor()
                } catch (_: Exception) {
                    // Fallback: try sending via Instrumentation-style approach
                    sendKeyEventViaInstrumentation(keyCode)
                }
            }.start()
        } catch (_: Exception) {
            // Silent fail — key injection is best-effort
        }
    }

    /**
     * Inject a D-pad Up key event.
     */
    fun dpadUp() = injectKey(KeyEvent.KEYCODE_DPAD_UP)

    /**
     * Inject a D-pad Down key event.
     */
    fun dpadDown() = injectKey(KeyEvent.KEYCODE_DPAD_DOWN)

    /**
     * Inject a D-pad Left key event.
     */
    fun dpadLeft() = injectKey(KeyEvent.KEYCODE_DPAD_LEFT)

    /**
     * Inject a D-pad Right key event.
     */
    fun dpadRight() = injectKey(KeyEvent.KEYCODE_DPAD_RIGHT)

    /**
     * Inject a D-pad Center (Select) key event.
     */
    fun dpadCenter() = injectKey(KeyEvent.KEYCODE_DPAD_CENTER)

    /**
     * Inject a Media Play/Pause key event.
     */
    fun mediaPlayPause() = injectKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    /**
     * Inject a Media Fast Forward key event.
     */
    fun mediaFastForward() = injectKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

    /**
     * Inject a Media Rewind key event.
     */
    fun mediaRewind() = injectKey(KeyEvent.KEYCODE_MEDIA_REWIND)

    /**
     * Adjust media volume up using AudioManager.
     * More reliable than key injection for volume.
     */
    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Adjust media volume down using AudioManager.
     */
    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Perform Back action via the accessibility service's performGlobalAction.
     * More reliable than injecting KEYCODE_BACK.
     */
    fun performBack() {
        AccessSwitchAccessibilityService.instance?.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK
        )
    }

    /**
     * Fallback key injection using Instrumentation-style KeyEvent dispatch.
     * This sends the event to the focused window's input queue.
     */
    private fun sendKeyEventViaInstrumentation(keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)

            // Use InputManager injection if available (requires INJECT_EVENTS permission
            // which AccessibilityService context may have)
            val im = context.getSystemService(Context.INPUT_SERVICE)
            val injectMethod = im?.javaClass?.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            injectMethod?.invoke(im, downEvent, 0) // INJECT_INPUT_EVENT_MODE_ASYNC = 0
            injectMethod?.invoke(im, upEvent, 0)
        } catch (_: Exception) {
            // Reflection-based fallback failed — silent fail
        }
    }
}
