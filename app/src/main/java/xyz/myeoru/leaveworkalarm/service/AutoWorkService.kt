package xyz.myeoru.leaveworkalarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import xyz.myeoru.leaveworkalarm.MainActivity
import xyz.myeoru.leaveworkalarm.R

class AutoWorkService : Service() {
    companion object {
        private const val CHANNEL_ID = "AUTO_WORK_CHECK"
        private const val NOTIFICATION_ID = 1000

        var isServiceRunning = false
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotification()

    }

    override fun onBind(intent: Intent): IBinder {
        isServiceRunning = true
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        return super.onStartCommand(intent, flags, START_STICKY)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle("자동 체크 중")
        }
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "자동 체크", NotificationManager.IMPORTANCE_HIGH)
        )

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@AutoWorkService
    }
}