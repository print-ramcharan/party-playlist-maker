package com.example.partyplaylist

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailsActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val imageView: ImageView = findViewById(R.id.detailsImageView)
        val textView: TextView = findViewById(R.id.detailsTextView)

        // Retrieve data from the intent
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val artistName = intent.getStringExtra("ARTIST_NAME")

        // Set data to the views
        imageUrl?.let { Glide.with(this).load(it).into(imageView) }
        textView.text = artistName
    }
}
