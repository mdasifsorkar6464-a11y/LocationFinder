package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var status: TextView
    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "Location Finder"
        title.textSize = 24f
        layout.addView(title)

        status = TextView(this)
        status.text = "আপনার লোকেশন দেখতে বাটনে চাপ দিন।"
        status.textSize = 18f
        layout.addView(status)

        val button = Button(this)
        button.text = "আমার লোকেশন দেখুন"
        layout.addView(button)

        button.setOnClickListener {
            getLocation()
        }

        setContentView(layout)
    }

    private fun getLocation() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
            val providers = manager.getProviders(true)

            var location: Location? = null

            for (provider in providers) {
                val found = manager.getLastKnownLocation(provider)
                if (found != null &&
                    (location == null ||
                    found.time > location.time)
                ) {
                    location = found
                }
            }

            if (location == null) {
                status.text = "লোকেশন পাওয়া যায়নি। GPS চালু করে আবার চেষ্টা করুন।"
                return
            }

            val lat = location.latitude
            val lon = location.longitude

            status.text = "Latitude: $lat\nLongitude: $lon"

            val mapUrl = "https://maps.google.com/?q=$lat,$lon"
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            )

        } catch (e: SecurityException) {
            status.text = "লোকেশন অনুমতি পাওয়া যায়নি।"
        } catch (e: Exception) {
            status.text = "লোকেশন পেতে সমস্যা হয়েছে।"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == this.requestCode &&
            grantResults.any {
                it == PackageManager.PERMISSION_GRANTED
            }
        ) {
            getLocation()
        } else {
            Toast.makeText(
                this,
                "লোকেশন অনুমতি দেওয়া হয়নি।",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
