package com.example.demolocationshareapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateTextView: TextView
    private lateinit var userAdapter: UserAdapter
    private lateinit var toolbar: Toolbar

    private val users = mutableListOf<UserLocation>()
    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "UserListActivity"
        private const val ACTIVE_DURATION_HOURS = 1L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        // Setup toolbar (simplified version)
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)

        // Setup RecyclerView
        userAdapter = UserAdapter(users) { uid ->
            val intent = Intent(this, ViewLocationActivity::class.java).apply {
                putExtra(ViewLocationActivity.EXTRA_SHARER_ID, uid)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Refresh triggered")
            startListeningForActiveUsers()
        }

        // Load data initially
        startListeningForActiveUsers()
    }

    private fun startListeningForActiveUsers() {
        showLoadingState()
        firestoreListener?.remove()

        val oneHourAgoSeconds = Timestamp.now().seconds - TimeUnit.HOURS.toSeconds(ACTIVE_DURATION_HOURS)
        val oneHourAgoTimestamp = Timestamp(oneHourAgoSeconds, 0)

        Log.d(TAG, "Fetching users active since ${oneHourAgoTimestamp.toDate()}")

        firestoreListener = db.collection("locations")
            .whereGreaterThan("timestamp", oneHourAgoTimestamp)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                hideLoadingState()

                if (e != null) {
                    Log.e(TAG, "Error loading users", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showEmptyState(true, "Failed to load users")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val activeUsers = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.id
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")
                        val timestamp = doc.getTimestamp("timestamp")
                        if (lat != null && lng != null && timestamp != null)
                            UserLocation(uid, lat, lng, timestamp)
                        else {
                            Log.w(TAG, "Invalid data for $uid")
                            null
                        }
                    }
                    userAdapter.updateData(activeUsers)
                    showEmptyState(activeUsers.isEmpty(), "No active users found.")
                } else {
                    userAdapter.updateData(emptyList())
                    showEmptyState(true, "No active users found.")
                }
            }
    }

    private fun showLoadingState() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateTextView.visibility = View.GONE
    }

    private fun hideLoadingState() {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
    }

    private fun showEmptyState(show: Boolean, message: String = "No data") {
        if (show) {
            emptyStateTextView.text = message
            emptyStateTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
        Log.d(TAG, "Firestore listener removed")
    }
}
