package com.asif.locationfinder

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "Location Finder - Family Safety"
        title.textSize = 22f
        layout.addView(title)

        val status = TextView(this)
        status.text = "আপনার লোকেশন দেখতে নিচের বাটনে চাপ দিন।"
        layout.addView(status)

        val button = Button(this)
        button.text = "আমার লোকেশন দেখুন"
        layout.addView(button)

        val shareButton = Button(this)
        shareButton.text = "লোকেশন শেয়ার করুন"
        layout.addView(shareButton)

        setContentView(layout)

        button.setOnClickListener {
            status.text = "লোকেশন ফিচার পরবর্তী ধাপে যুক্ত করা হবে।"
        }

        shareButton.setOnClickListener {
            status.text = "শেয়ার ফিচার পরবর্তী ধাপে যুক্ত করা হবে।"
        }
    }
}
