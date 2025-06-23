package com.example.demolocationshareapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TrackLocationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_location)

        val uidInput = findViewById<EditText>(R.id.edit_uid)
        val trackBtn = findViewById<Button>(R.id.btn_track_now)

        trackBtn.setOnClickListener {
            val uid = uidInput.text.toString().trim()
            if (uid.isNotEmpty()) {
                val intent = Intent(this, ViewLocationActivity::class.java)
                intent.putExtra(ViewLocationActivity.EXTRA_SHARER_ID, uid)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a valid UID", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
