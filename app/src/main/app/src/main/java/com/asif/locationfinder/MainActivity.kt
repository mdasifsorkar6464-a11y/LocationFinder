package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.ViewGroup
import android.view.Gravity
import android.graphics.Color
import android.util.TypedValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private val requestCode = 100
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Location Finder"
            textSize = 26f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "নিজের অবস্থান জানতে নিচের বাটনে চাপ দিন"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        val button = Button(this).apply {
            text = "আমার অবস্থান শেয়ার করুন"
            setOnClickListener { shareLocation() }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)
    }

    private fun shareLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestCode
            )
            return
        }

        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location == null) {
                status.text = "অবস্থান পাওয়া যায়নি। GPS চালু করে আবার চেষ্টা করুন।"
                return
            }

            val link = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "আমার অবস্থান: $link")
            }
            startActivity(Intent.createChooser(intent, "অবস্থান শেয়ার করুন"))
        } catch (e: SecurityException) {
            status.text = "অবস্থানের অনুমতি পাওয়া যায়নি।"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            shareLocation()
        } else {
            Toast.makeText(this, "অবস্থানের অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }
}
