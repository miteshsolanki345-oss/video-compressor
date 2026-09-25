package com.videocompressor.app.data.service

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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.videocompressor.app.MainActivity
import com.videocompressor.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoCompressionService : Service {

    constructor() : super()

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VideoCompressor::EncodingWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: "video.mp4"
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max safety
                _isServiceRunning.value = true

                val notification = buildNotification(
                    title = "Compressing $filename",
                    progress = 0,
                    status = "Starting Same-to-Same HEVC encoder..."
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE_PROGRESS -> {
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val statusText = intent.getStringExtra(EXTRA_STATUS) ?: ""
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: "Video"
                val notification = buildNotification(
                    title = "Compressing $filename ($progress%)",
                    progress = progress,
                    status = statusText
                )
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP, ACTION_CANCEL -> {
                stopCompressionService()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopCompressionService() {
        _isServiceRunning.value = false
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        _isServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, progress: Int, status: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, VideoCompressionService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Compression Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of on-device video encoding"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "video_compressor_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.videocompressor.app.START"
        const val ACTION_UPDATE_PROGRESS = "com.videocompressor.app.UPDATE"
        const val ACTION_STOP = "com.videocompressor.app.STOP"
        const val ACTION_CANCEL = "com.videocompressor.app.CANCEL"

        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS = "extra_status"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context, filename: String) {
            val intent = Intent(context, VideoCompressionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FILENAME, filename)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, filename: String, progress: Int, status: String) {
            val intent = Intent(context, VideoCompressionService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_STATUS, status)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VideoCompressionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
