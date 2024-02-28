package com.enginecal.kioskapp.fgService

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import com.enginecal.kioskapp.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class BleBatteryService : Service() {

    private val binder = LocalBinder()
    private var timerJob: Job? = null
    private val coroutineScope = CoroutineScope(Job())

    inner class LocalBinder : Binder() {
        fun getService(): BleBatteryService = this@BleBatteryService
    }
    override fun onBind(intent: Intent?): IBinder? {
     Log.d(TAG, "onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startAsForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        Toast.makeText(this, "Foreground Service created", Toast.LENGTH_SHORT).show()
//        startServiceRunningTicker()


        try {
            val filter2 = IntentFilter()
            filter2.addAction(Intent.ACTION_POWER_CONNECTED)
            filter2.addAction(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(receiver, filter2)
        } catch (e: Exception) {
           Log.e("Faris - catch ", e.localizedMessage)
        }


    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.e("Faris - BR ", " in receiver ")

                if (intent.action === Intent.ACTION_POWER_CONNECTED) {
                    Toast.makeText(
                        this@BleBatteryService,
                        "Battery connected ",
                        Toast.LENGTH_SHORT
                    ).show()

                    val mIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(mIntent)

                } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {

                    Toast.makeText(
                        this@BleBatteryService,
                        "Battery disconnected ",
                        Toast.LENGTH_SHORT
                    ).show()


                }
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        timerJob?.cancel()
        coroutineScope.coroutineContext.cancelChildren()
        unregisterReceiver(receiver);
        Toast.makeText(this, "Foreground Service destroyed", Toast.LENGTH_SHORT).show()
    }


    private fun startAsForegroundService() {
        // create the notification channel
        NotificationsHelper.createNotificationChannel(this)

        // promote service to foreground service
        ServiceCompat.startForeground(
            this,
            1,
            NotificationsHelper.buildNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        )
    }

    fun stopForegroundService() {
        stopSelf()
    }

    private fun startServiceRunningTicker() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            tickerFlow()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BleBatteryService,
                            "Foreground Service still running!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun tickerFlow(
        period: Duration = TICKER_PERIOD_SECONDS,
        initialDelay: Duration = TICKER_PERIOD_SECONDS
    ) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    companion object {
        lateinit var LocalBinder: Any
        private const val TAG = "BleBatteryService"
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
        private val TICKER_PERIOD_SECONDS = 5.seconds
    }




}