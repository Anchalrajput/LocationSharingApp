package com.example.demolocationshareapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue // Import FieldValue if you also retrieve timestamps

class ViewLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap // Renamed 'map' to 'googleMap' for clarity
    private var locationMarker: Marker? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null // Renamed 'listener' for clarity

    private var sharerId: String? = null // Make sharerId a class property

    companion object {
        private const val TAG = "ViewLocationActivity"
        const val EXTRA_SHARER_ID = "sharerId" // Define a constant for the intent extra key
        private const val DEFAULT_ZOOM_LEVEL = 16f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_location)

        sharerId = intent.getStringExtra(EXTRA_SHARER_ID)

        if (sharerId.isNullOrEmpty()) {
            Log.e(TAG, "No sharerId provided. Finishing activity.")
            Toast.makeText(this, "Error: No user ID to view location.", Toast.LENGTH_LONG).show()
            finish() // Finish if no ID is provided
            return
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        // Ensure mapFragment is not null before proceeding
        if (mapFragment == null) {
            Log.e(TAG, "Map fragment not found with ID R.id.map")
            Toast.makeText(this, "Error: Map component not available.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "GoogleMap is ready.")

        // Basic map settings
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false // Usually not needed for viewing another's location

        // Start listening for location updates from Firestore
        startLocationListener()
    }

    private fun startLocationListener() {
        val currentSharerId = sharerId ?: run {
            Log.e(TAG, "Sharer ID is null when trying to start listener. Aborting.")
            Toast.makeText(this, "Failed to start location updates (user ID missing).", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        firestoreListener = firestore.collection("locations").document(currentSharerId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    Toast.makeText(this, "Error fetching location: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("latitude")
                    val lng = snapshot.getDouble("longitude")
                    // You might also want to display the timestamp
                    // val timestamp = snapshot.get("timestamp") as? com.google.firebase.Timestamp

                    if (lat == null || lng == null) {
                        Log.w(TAG, "Latitude or longitude is null for sharerId: $currentSharerId")
                        Toast.makeText(this, "Location data incomplete.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val newLocation = LatLng(lat, lng)

                    if (locationMarker == null) {
                        // Add marker for the first time
                        locationMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(newLocation)
                                .title("User Location")
                                .snippet("Last updated: (Timestamp or just user)") // Consider adding timestamp
                        )
                        // For initial load, animate smoothly
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, DEFAULT_ZOOM_LEVEL))
                        Log.d(TAG, "Initial marker added and camera moved.")
                    } else {
                        // Update existing marker's position
                        locationMarker?.position = newLocation
                        // Optionally update snippet with new timestamp:
                        // locationMarker?.snippet = "Last updated: ${timestamp?.toDate()?.toLocaleString()}"
                        // If you want to always center, use this. Otherwise, just update marker.
                        // Only move camera if user hasn't manually moved it, or if location significantly changes.
                        // For simplicity, we keep moving it for now.
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(newLocation)) // Just move to new LatLng without zoom if already zoomed
                        Log.d(TAG, "Marker position updated to $newLocation.")
                    }
                } else {
                    Log.d(TAG, "Current data: null or document does not exist for sharerId: $currentSharerId")
                    // If the document doesn't exist or is empty, remove the marker
                    locationMarker?.remove()
                    locationMarker = null
                    Toast.makeText(this, "Location data not available or stopped sharing.", Toast.LENGTH_LONG).show()
                    // Optionally, move camera to a default location or finish activity
                }
            }
        Log.d(TAG, "Firestore listener started for sharerId: $currentSharerId")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the Firestore listener to prevent memory leaks
        firestoreListener?.remove()
        Log.d(TAG, "Firestore listener removed in onDestroy.")
    }
}