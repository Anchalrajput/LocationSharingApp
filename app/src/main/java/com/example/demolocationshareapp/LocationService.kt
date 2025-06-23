package com.example.demolocationshareapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LocationService : Service() {

    companion object {
        const val ACTION_START_LOCATION_SERVICE = "START_LOCATION_SERVICE"
        const val ACTION_STOP_LOCATION_SERVICE = "STOP_LOCATION_SERVICE"
        const val EXTRA_DURATION_MILLIS = "duration"
        private const val CHANNEL_ID = "location_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var stopTimeMillis: Long = 0L

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_SERVICE -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_MILLIS, 3600000L) // 1 hour default
                stopTimeMillis = System.currentTimeMillis() + duration
                startForeground(NOTIFICATION_ID, buildNotification())
                startLocationUpdates()
            }

            ACTION_STOP_LOCATION_SERVICE -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = 5000L
            fastestInterval = 3000L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                Log.d("LocationService", "Received location: ${result.locations}")
                for (location in result.locations) {
                    uploadLocation(location)
                }

                if (System.currentTimeMillis() >= stopTimeMillis) {
                    stopLocationUpdates()
                    stopSelf()
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Missing location permission.")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        Log.d("LocationService", "Started location updates")
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationService", "Stopped location updates")
        }
    }

    private fun uploadLocation(location: Location) {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("LocationService", "User not authenticated. Cannot upload location.")
            return
        }

        val data = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to Date()
        )

        firestore.collection("locations").document(uid).set(data)
            .addOnSuccessListener {
                Log.d("LocationService", "Location updated: $data")
            }
            .addOnFailureListener {
                Log.e("LocationService", "Failed to upload location", it)
            }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Sharing Active")
            .setContentText("Your real-time location is being shared.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        Log.d("LocationService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
