package com.example.demolocationshareapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class ShareLocationActivity : AppCompatActivity() {

    private val requestLocationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
                requestBackgroundLocationPermission()
            } else {
                Toast.makeText(this, "Tap a button again to start sharing.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Location permission is required for sharing.", Toast.LENGTH_LONG).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Tap a button again to start sharing.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Notification permission is recommended.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_location)

        val oneHourBtn = findViewById<Button>(R.id.btn_one_hour)
        val fiveHourBtn = findViewById<Button>(R.id.btn_five_hour)
        val fullDayBtn = findViewById<Button>(R.id.btn_full_day)
        val stopSharingBtn = findViewById<Button>(R.id.btn_stop_sharing)

        oneHourBtn.setOnClickListener { checkPermissionsAndAuthThenStart(1) }
        fiveHourBtn.setOnClickListener { checkPermissionsAndAuthThenStart(5) }
        fullDayBtn.setOnClickListener { checkPermissionsAndAuthThenStart(24) }
        stopSharingBtn.setOnClickListener { stopSharing() }
    }

    private fun checkPermissionsAndAuthThenStart(hours: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        if (!hasLocationPermissions()) {
            requestLocationPermissions()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
            requestBackgroundLocationPermission()
            return
        }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Toast.makeText(this, "Signed in: ${it.user?.uid}", Toast.LENGTH_SHORT).show()
                    startLocationService(hours)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            startLocationService(hours)
        }
    }

    private fun startLocationService(hours: Int) {
        val durationMillis = TimeUnit.HOURS.toMillis(hours.toLong())
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START_LOCATION_SERVICE
            putExtra(LocationService.EXTRA_DURATION_MILLIS, durationMillis)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Sharing started for $hours hour(s).", Toast.LENGTH_LONG).show()
        //finish()
    }

    private fun stopSharing() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP_LOCATION_SERVICE
        }
        startService(intent) // <-- Do NOT use stopService here
        Toast.makeText(this, "Location sharing stopped.", Toast.LENGTH_SHORT).show()
        finish()
    }


    private fun hasLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineLocationGranted || coarseLocationGranted
    }

    private fun isBackgroundLocationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestLocationPermissions() {
        requestLocationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestLocationPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
    }
}
