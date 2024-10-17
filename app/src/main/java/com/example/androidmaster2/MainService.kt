package com.example.androidmaster2

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
@RequiresApi(Build.VERSION_CODES.O)
class MainService : Service() {

    private lateinit var mMessenger: Messenger
    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "CHANNEL_ID"
    private val notificationId = 1

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationIntent: Intent
    private lateinit var pendingIntent: PendingIntent

    var isRunning = false
    private var counter = 21

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler(): Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            var clientMessenger = msg.replyTo
            var replyArg = 0

            when(msg.what){
                START_TIMER -> replyArg = startCountdown()
                RESTART_TIMER -> replyArg = restartCountdown()
                GET_CURRENT -> replyArg = getCurrentProgress()
                IS_RUNNING -> replyArg = if (isRunning) 1 else 0
                else -> super.handleMessage(msg)
            }

            val msg: Message = Message.obtain(null, msg.what, replyArg, 0)
            try {
                clientMessenger?.send(msg)
            }catch (e: Exception){
                Log.e(TAG, e.toString())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            channelId, "Foreground Messenger Service Channel(FMSC)",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        notificationIntent = Intent(this, MainActivity::class.java)
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        mMessenger = Messenger(IncomingHandler())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
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
        startForeground(notificationId, getNotification())
    }

    private fun updateForegroundServiceCounter(counter: Int) {
        notificationManager.notify(notificationId, getNotification())
    }

    private fun getNotification(): Notification{
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Title")
            .setContentText("Remaining time: $counter")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object{
        const val START_TIMER = 1
        const val RESTART_TIMER = 2
        const val GET_CURRENT = 3
        const val IS_RUNNING = 4
    }
}
