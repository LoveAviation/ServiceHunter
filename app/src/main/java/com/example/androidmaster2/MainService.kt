package com.example.androidmaster2

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
@RequiresApi(Build.VERSION_CODES.O)
class MainService : Service() {

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "CHANNEL_ID"
    private val notificationId = 1

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationIntent: Intent
    private lateinit var pendingIntent: PendingIntent

    var isRunning = false
    private var counter = 21

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            channelId, "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        notificationIntent = Intent(this, MainActivity::class.java)
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.deleteNotificationChannel(channelId)
    }

    fun getCurrentProgress(): Int {
        return counter
    }

    fun restartCountdown(): Int {
        counter = 20
        updateForegroundServiceCounter(counter)
        return counter
    }

    fun startCountdown(): Int {
        counter = 20
        isRunning = true
        startForegroundService()

        handler.post(object : Runnable {
            override fun run() {
                if (counter > 0) {
                    handler.postDelayed(this, 1000)
                    counter--
                    updateForegroundServiceCounter(counter)
                } else {
                    isRunning = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        })
        return counter
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Service Notification")
            .setContentText("Time: $counter")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)
    }

    private fun updateForegroundServiceCounter(counter: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Service Notification")
            .setContentText("Time: $counter")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
