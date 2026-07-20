package com.audiojournal.app.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.audiojournal.app.AudioJournalApp
import com.audiojournal.app.MainActivity
import com.audiojournal.app.R
import com.audiojournal.app.upload.UploadFolder
import com.audiojournal.app.upload.UploadScheduler

/**
 * Foreground service that keeps the microphone alive while recording, even
 * when the screen is off or the app is in the background. The actual state
 * lives in the shared [RecordingEngine]; this service drives its transitions
 * in response to intents sent from the UI.
 */
class RecordingService : Service() {

    private val engine: RecordingEngine
        get() = (application as AudioJournalApp).container.recordingEngine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startAsForeground(getString(R.string.notification_recording))
                if (!engine.start()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            ACTION_PAUSE -> {
                engine.pause()
                updateNotification(getString(R.string.notification_paused))
            }

            ACTION_RESUME -> {
                engine.resume()
                updateNotification(getString(R.string.notification_recording))
            }

            ACTION_STOP -> {
                val saved = engine.stop()
                if (saved != null) {
                    val folder = intent.getStringExtra(EXTRA_FOLDER_ID)?.let { id ->
                        UploadFolder(
                            label = intent.getStringExtra(EXTRA_FOLDER_LABEL) ?: id,
                            folderId = id,
                        )
                    }
                    UploadScheduler.enqueue(applicationContext, saved.file, folder)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_DISCARD -> {
                engine.discard()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.audiojournal.app.action.START"
        const val ACTION_PAUSE = "com.audiojournal.app.action.PAUSE"
        const val ACTION_RESUME = "com.audiojournal.app.action.RESUME"
        const val ACTION_STOP = "com.audiojournal.app.action.STOP"
        const val ACTION_DISCARD = "com.audiojournal.app.action.DISCARD"
        const val EXTRA_FOLDER_ID = "com.audiojournal.app.extra.FOLDER_ID"
        const val EXTRA_FOLDER_LABEL = "com.audiojournal.app.extra.FOLDER_LABEL"

        fun sendAction(context: Context, action: String, folder: UploadFolder? = null) {
            val intent = Intent(context, RecordingService::class.java).setAction(action)
            if (folder != null) {
                intent.putExtra(EXTRA_FOLDER_ID, folder.folderId)
                intent.putExtra(EXTRA_FOLDER_LABEL, folder.label)
            }
            if (action == ACTION_START) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
