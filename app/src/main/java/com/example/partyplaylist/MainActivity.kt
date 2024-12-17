package com.example.partyplaylist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.partyplaylist.utils.TokenManager

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logoactivity)

        val tokenManager = TokenManager(this)

        // Refresh token if needed
        tokenManager.refreshTokenIfNeeded { accessToken ->
            if (accessToken != null) {
                // User is logged in, redirect to HomePageActivity
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finish() // Close the MainActivity
            } else {
                // Token refresh failed, proceed to login activity
                Handler().postDelayed({
                    // Start the login activity
                    val intent = Intent(this, LoginMultiActivity::class.java)
                    startActivity(intent)
                    finish() // Close the MainActivity
                }, 2000)
            }
        }
    }
}
