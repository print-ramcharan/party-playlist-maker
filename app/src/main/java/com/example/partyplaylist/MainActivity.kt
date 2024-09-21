package com.example.partyplaylist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity


@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logoactivity)
        val token = getAccessToken()
        if (token != null) {

//             User is logged in, redirect to HomePageActivity
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            // Close SpotifyLoginActivity

        } else {
            Handler().postDelayed({
                // Start the login activity
                val intent = Intent(this, LoginMultiActivity::class.java)
                startActivity(intent)
                // Finish the introductory activity so it's removed from the back stack
                finish()
            }, 2000)
        }

    }


    private fun getAccessToken(): String? {
        val prefs = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
        return prefs.getString("access_token", null)
    }
}

