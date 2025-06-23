package com.example.demolocationshareapp

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp // To denote server timestamp for data class

// Data class to hold user location information
data class UserLocation(
    val uid: String = "", // User ID, initialize with empty string
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp // Annotation to indicate this field holds a server-generated timestamp
    val timestamp: com.google.firebase.Timestamp? = null // Firebase Timestamp type, nullable
) {
    // You might want a display name for the user. For now, we use UID.
    // If you add a "name" field to your "locations" document in Firestore,
    // you can include it here.
    @Exclude // Exclude from Firestore writes, as UID is the document ID
    fun getDisplayName(): String {
        return "User: $uid" // Simple display for now. Could be a fetched user name.
    }
}