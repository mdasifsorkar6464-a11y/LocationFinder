
package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
    private lateinit var copyButton: Button
    private lateinit var shareButton: Button

    private var latitude: Double? = null
    private var longitude: Double? = null

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
        title.textSize = 26f
        layout.addView(title)

        status = TextView(this)
        status.text = "আপনার বর্তমান লোকেশন দেখতে নিচের বাটনে চাপ দিন।"
        status.textSize = 18f
        layout.addView(status)

        val locationButton = Button(this)
        locationButton.text = "আমার লোকেশন দেখুন"
        layout.addView(locationButton)

        copyButton = Button(this)
        copyButton.text = "লোকেশন কপি করুন"
        copyButton.isEnabled = false
        layout.addView(copyButton)

        shareButton = Button(this)
        shareButton.text = "লোকেশন শেয়ার করুন"
        shareButton.isEnabled = false
        layout.addView(shareButton)

        locationButton.setOnClickListener {
            getLocation()
        }

        copyButton.setOnClickListener {
            copyLocation()
        }

        shareButton.setOnClickListener {
            shareLocation()
        }

        setContentView(layout)
    }

    private fun getLocation() {
        if (
            ContextCompat.checkSelfPermission(
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

        status.text = "আপনার লোকেশন খোঁজা হচ্ছে..."
        copyButton.isEnabled = false
        shareButton.isEnabled = false

        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                if (location == null) {
                    status.text = "লোকেশন পাওয়া যায়নি। আবার চেষ্টা করুন।"
                    return@addOnSuccessListener
                }

                latitude = location.latitude
                longitude = location.longitude

                status.text =
                    "আপনার বর্তমান লোকেশন:\n\nঅক্ষাংশ: $latitude\nদ্রাঘিমাংশ: $longitude"

                copyButton.isEnabled = true
                shareButton.isEnabled = true

            }.addOnFailureListener {
                status.text = "লোকেশন পেতে সমস্যা হয়েছে। আবার চেষ্টা করুন।"
            }
        } catch (e: SecurityException) {
            status.text = "লোকেশন অনুমতি পাওয়া যায়নি।"
        }
    }

    private fun getLocationText(): String {
        val lat = latitude ?: return ""
        val lon = longitude ?: return ""
        val mapUrl = "https://maps.google.com/?q=$lat,$lon"

        return "আমার বর্তমান লোকেশন:\nঅক্ষাংশ: $lat\nদ্রাঘিমাংশ: $lon\nGoogle Maps: $mapUrl"
    }

    private fun copyLocation() {
        if (latitude == null || longitude == null) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("আমার লোকেশন", getLocationText())
        )

        Toast.makeText(this, "লোকেশন কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    private fun shareLocation() {
        if (latitude == null || longitude == null) return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getLocationText())
        }

        startActivity(Intent.createChooser(intent, "লোকেশন শেয়ার করুন"))
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
