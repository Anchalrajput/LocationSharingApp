package com.example.demolocationshareapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val shareBtn = findViewById<Button>(R.id.btn_share_location)
        val viewBtn = findViewById<Button>(R.id.btn_view_users)
        val trackFriendBtn = findViewById<Button>(R.id.btn_track_friend)
        trackFriendBtn.setOnClickListener {
            val intent = Intent(this, TrackLocationActivity::class.java)
            startActivity(intent)
        }

        shareBtn.setOnClickListener {
            startActivity(Intent(this, ShareLocationActivity::class.java))
        }

        viewBtn.setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java))
        }
    }
}
