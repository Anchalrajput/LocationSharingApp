package com.example.demolocationshareapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UserAdapter(
    private val userList: MutableList<UserLocation>, // Changed to MutableList for easier updates
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // Inner class for ViewHolder
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textViewUser)
        val lastActiveTextView: TextView = view.findViewById(R.id.textViewLastActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.nameTextView.text = user.getDisplayName() // Using the display name function

        // Format and display the last active time
        val lastActiveText = user.timestamp?.let {
            val now = System.currentTimeMillis()
            val timeDiffMillis = now - it.toDate().time

            when {
                timeDiffMillis < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                timeDiffMillis < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(timeDiffMillis)} minutes ago"
                timeDiffMillis < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(timeDiffMillis)} hours ago"
                else -> {
                    // For longer periods, show exact date
                    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(it.toDate())
                }
            }
        } ?: "Unknown" // If timestamp is null
        holder.lastActiveTextView.text = "Last active: $lastActiveText"

        // Set click listener for the whole item view
        holder.itemView.setOnClickListener { onClick(user.uid) }
    }

    override fun getItemCount() = userList.size

    // Method to update data in the adapter
    fun updateData(newUsers: List<UserLocation>) {
        userList.clear()
        userList.addAll(newUsers)
        notifyDataSetChanged() // Notify the RecyclerView that the data has changed
    }
}