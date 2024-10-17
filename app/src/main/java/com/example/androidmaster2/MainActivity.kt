package com.example.androidmaster2


import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmaster2.databinding.ActivityMainBinding

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var myService: MainService? = null
    private var isServiceBound = false
    private val handler = Handler(Looper.getMainLooper())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MainService.LocalBinder
            myService = binder.getService()
            isServiceBound = true

            if (myService?.isRunning == true) {
                binding.button.text = getString(R.string.restart_service)
                startPolling()
            } else {
                binding.text.text = ""
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.text.text = ""
        startService()

        binding.button.setOnClickListener {
            if (myService?.isRunning == true) {
                binding.text.text = myService?.restartCountdown().toString()
            } else {
                binding.text.text = myService?.startCountdown().toString()
                binding.button.text = getString(R.string.restart_service)
                startPolling()
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
                val counter = myService?.getCurrentProgress()
                if (counter != null && counter > 0) {
                    binding.text.text = counter.toString()
                    handler.postDelayed(this, 1000)
                } else {
                    binding.button.text = getString(R.string.start_service)
                    binding.text.text = ""
                }
            }
        })
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
