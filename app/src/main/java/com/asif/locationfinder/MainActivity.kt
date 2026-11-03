
package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : Activity() {

    private lateinit var status: TextView
    private val requestCode = 100
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

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
        status.text = "আপনার বর্তমান লোকেশন দেখতে নিচের বাটনে চাপ দিন।"
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
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
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

        status.text = "বর্তমান লোকেশন খোঁজা হচ্ছে..."

        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location == null) {
                    status.text = "লোকেশন পাওয়া যায়নি। আবার চেষ্টা করুন।"
                    return@addOnSuccessListener
                }

                val lat = location.latitude
                val lon = location.longitude

                status.text = "আপনার লোকেশন:\n$lat\n$lon"

                val url = "https://maps.google.com/?q=$lat,$lon"
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                )
            }.addOnFailureListener {
                status.text = "লোকেশন পেতে সমস্যা হয়েছে। আবার চেষ্টা করুন।"
            }
        } catch (e: SecurityException) {
            status.text = "লোকেশন অনুমতি পাওয়া যায়নি।"
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
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
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
