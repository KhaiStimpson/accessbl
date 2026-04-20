package com.accessswitch.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.accessswitch.R
import com.accessswitch.scanning.ScanningEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the app process alive while scanning is active.
 *
 * The actual overlay window is managed by [OverlayManager] within
 * [AccessSwitchAccessibilityService] (since TYPE_ACCESSIBILITY_OVERLAY
 * requires an AccessibilityService context).
 *
 * This service handles:
 * - Foreground notification (process persistence)
 * - Scan item loading coordination
 * - Starting/stopping the scanning engine
 */
@AndroidEntryPoint
class ScanningOverlayService : Service() {

    @Inject
    lateinit var scanningEngine: ScanningEngine

    @Inject
    lateinit var mainMenuProvider: MainMenuProvider

    @Inject
    lateinit var feedbackManager: FeedbackManager

    companion object {
        const val CHANNEL_ID = "scanning_service"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_SCANNING = "com.accessswitch.ACTION_START_SCANNING"
        const val ACTION_STOP_SCANNING = "com.accessswitch.ACTION_STOP_SCANNING"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SCANNING -> {
                scanningEngine.stop()
                stopSelf()
            }
            else -> {
                // Load main menu items and start scanning
                val menuItems = mainMenuProvider.buildMainMenu()
                scanningEngine.setItems(menuItems)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scanningEngine.stop()
        feedbackManager.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_scanning),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_scanning_active))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }
}
