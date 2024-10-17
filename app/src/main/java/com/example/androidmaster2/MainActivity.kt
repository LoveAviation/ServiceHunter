package com.example.androidmaster2


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmaster2.MainService.Companion.GET_CURRENT
import com.example.androidmaster2.MainService.Companion.IS_RUNNING
import com.example.androidmaster2.MainService.Companion.RESTART_TIMER
import com.example.androidmaster2.MainService.Companion.START_TIMER
import com.example.androidmaster2.databinding.ActivityMainBinding

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var myMessenger: Messenger? = null
    private var myService: Messenger? = null
    private var isServiceBound = false
    private var isCounterRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myService = Messenger(service)
            isServiceBound = true
            sendMsgToService(IS_RUNNING)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myService = null
            isServiceBound = false
        }
    }

    @SuppressLint("HandlerLeak")
    inner class ResponseHandler(): Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                START_TIMER -> {
                    startPolling()
                    isCounterRunning = true
                    binding.button.text = getString(R.string.restart_service)
                    updateUI(msg.arg1)
                }
                RESTART_TIMER -> {
                    updateUI(msg.arg1)
                }
                GET_CURRENT -> {
                    updateUI(msg.arg1)
                }
                IS_RUNNING -> {
                    if (msg.arg1 == 1){
                        isCounterRunning = true
                        startPolling()
                        binding.button.text = getString(R.string.restart_service)
                    }else{
                        isCounterRunning = false
                        binding.button.text = getString(R.string.start_service)
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myMessenger = Messenger(ResponseHandler())
        startService()

        binding.button.setOnClickListener{
            if (isCounterRunning){
                sendMsgToService(RESTART_TIMER)
            }else{
                sendMsgToService(START_TIMER)
            }
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, MainService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    private fun startPolling() {
        handler.post(object : Runnable {
            override fun run() {
                sendMsgToService(GET_CURRENT)
                if (isCounterRunning) { handler.postDelayed(this, 1000) }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(counter: Int){
        if (counter > 0){
            binding.text.text = counter.toString()
        }else{
            binding.button.text = getString(R.string.start_service)
            binding.text.text = ""
            isCounterRunning = false
        }
    }

    private fun sendMsgToService(messageId: Int){
        val msg = Message.obtain(null, messageId, 0, 0)
        msg.replyTo = myMessenger
        try {
            myService?.send(msg)
        }catch (e: Exception){
            Log.e(TAG, e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }

    private fun unbindService() {
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }
}
