package xyz.myeoru.leaveworkalarm.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.myeoru.leaveworkalarm.MainActivity
import xyz.myeoru.leaveworkalarm.R

class AutoWorkService : Service() {
    companion object {
        private const val tag = "AutoWorkService"
        private const val CHANNEL_ID = "AUTO_WORK_CHECK"
        private const val NOTIFICATION_ID = 1000
        private const val LOCATION_CHECK_INTERVAL = 10000L

        private var serviceRunning = false
        val isServiceRunning get() = serviceRunning
    }

    private val binder = LocalBinder()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotification()
        initLocation()
        startLocationUpdates()
    }

    override fun onBind(intent: Intent): IBinder {
        serviceRunning = true
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceRunning = true
        return super.onStartCommand(intent, flags, START_STICKY)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceRunning = false
        stopLocationUpdates()
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

    private fun initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    location ?: return@forEach

                    CoroutineScope(Dispatchers.IO).launch {
                        val destinationAddress = "서울특별시 중구 소공동 세종대로18길 2"
                        getDistanceAddressAndLocationFlow(destinationAddress, location).catch {
                            Log.e(tag, it.message.toString())
                            cancel(it.message.toString(), it)
                        }.collectLatest { distance ->
                            Log.d(tag, distance.toString())
                        }
                    }
                }
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(LOCATION_CHECK_INTERVAL).apply {
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        }.build()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = createLocationRequest()
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private suspend fun getDistanceAddressAndLocationFlow(
        destinationAddress: String,
        currentLocation: Location
    ) = channelFlow {
        val addressFlow: Flow<Address> = callbackFlow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Geocoder(this@AutoWorkService).getFromLocationName(destinationAddress, 1) {
                    trySend(it.first())
                }
            } else {
                Geocoder(this@AutoWorkService).getFromLocationName(destinationAddress, 1)?.let {
                    trySend(it.first())
                }
            }
            awaitClose()
        }

        addressFlow.collectLatest {
            val location = Location("destination").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
            val distance = location.distanceTo(currentLocation)
            send(distance)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@AutoWorkService
    }
}