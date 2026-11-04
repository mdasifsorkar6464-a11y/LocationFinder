
package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var layout: LinearLayout
    private lateinit var status: TextView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    private var latitude: Double? = null
    private var longitude: Double? = null

    private val requestCode = 100

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val splash = TextView(this).apply {
            text = "Power by : MD Asif Sorkar"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }

        setContentView(splash)

        Handler(Looper.getMainLooper()).postDelayed({
            showNextScreen()
        }, 1800)
    }

    private fun showNextScreen() {
        val user = auth.currentUser

        if (user != null && user.isEmailVerified) {
            showLocationScreen()
        } else {
            if (user != null) auth.signOut()
            showLoginScreen()
        }
    }

    private fun makeLayout() {
        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
    }

    private fun showLoginScreen() {
        makeLayout()

        val title = TextView(this).apply {
            text = "Location Finder"
            textSize = 28f
        }
        layout.addView(title)

        status = TextView(this).apply {
            text = "আপনার অ্যাকাউন্টে লগইন করুন"
            textSize = 17f
        }
        layout.addView(status)

        emailInput = EditText(this).apply {
            hint = "আপনার Gmail / Email"
            inputType = 33
        }
        layout.addView(emailInput)

        passwordInput = EditText(this).apply {
            hint = "পাসওয়ার্ড"
            inputType = 129
        }
        layout.addView(passwordInput)

        val loginButton = Button(this).apply {
            text = "লগইন করুন"
            setOnClickListener { loginUser() }
        }
        layout.addView(loginButton)

        val registerButton = Button(this).apply {
            text = "নতুন অ্যাকাউন্ট তৈরি করুন"
            setOnClickListener { registerUser() }
        }
        layout.addView(registerButton)

        val resetButton = Button(this).apply {
            text = "পাসওয়ার্ড ভুলে গেছেন?"
            setOnClickListener { resetPassword() }
        }
        layout.addView(resetButton)

        setContentView(layout)
    }

    private fun registerUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.length < 6) {
            status.text = "সঠিক ইমেইল দিন এবং কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন।"
            return
        }

        status.text = "অ্যাকাউন্ট তৈরি হচ্ছে..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verification ->
                            auth.signOut()

                            if (verification.isSuccessful) {
                                status.text =
                                    "আপনার ইমেইলে ভেরিফিকেশন লিংক পাঠানো হয়েছে। ইমেইল যাচাই করে আবার লগইন করুন।"
                            } else {
                                status.text =
                                    "ভেরিফিকেশন ইমেইল পাঠানো যায়নি। পরে আবার চেষ্টা করুন।"
                            }
                        }
                } else {
                    status.text =
                        task.exception?.localizedMessage ?: "অ্যাকাউন্ট তৈরি হয়নি।"
                }
            }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            status.text = "ইমেইল ও পাসওয়ার্ড লিখুন।"
            return
        }

        status.text = "লগইন হচ্ছে..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.reload()?.addOnCompleteListener {
                        if (user.isEmailVerified) {
                            showLocationScreen()
                        } else {
                            auth.signOut()
                            status.text =
                                "আগে আপনার ইমেইলে পাঠানো ভেরিফিকেশন লিংকে চাপ দিন।"
                        }
                    }
                } else {
                    status.text =
                        task.exception?.localizedMessage ?: "লগইন করা যায়নি।"
                }
            }
    }

    private fun resetPassword() {
        val email = emailInput.text.toString().trim()

        if (email.isEmpty()) {
            status.text = "আগে আপনার ইমেইল লিখুন।"
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                status.text = if (task.isSuccessful) {
                    "পাসওয়ার্ড পরিবর্তনের লিংক ইমেইলে পাঠানো হয়েছে।"
                } else {
                    task.exception?.localizedMessage ?: "লিংক পাঠানো যায়নি।"
                }
            }
    }

    private fun showLocationScreen() {
        makeLayout()

        val title = TextView(this).apply {
            text = "Location Finder"
            textSize = 26f
        }
        layout.addView(title)

        status = TextView(this).apply {
            text = "আপনার বর্তমান লোকেশন দেখতে নিচের বাটনে চাপ দিন।"
            textSize = 18f
        }
        layout.addView(status)

        val locationButton = Button(this).apply {
            text = "আমার লোকেশন দেখুন"
            setOnClickListener { getLocation() }
        }
        layout.addView(locationButton)

        val copyButton = Button(this).apply {
            text = "লোকেশন কপি করুন"
            isEnabled = false
            setOnClickListener { copyLocation() }
        }
        layout.addView(copyButton)

        val shareButton = Button(this).apply {
            text = "লোকেশন শেয়ার করুন"
            isEnabled = false
            setOnClickListener { shareLocation() }
        }
        layout.addView(shareButton)

        val mapsButton = Button(this).apply {
            text = "Google Maps-এ দেখুন"
            isEnabled = false
            setOnClickListener { openGoogleMaps() }
        }
        layout.addView(mapsButton)

        val logoutButton = Button(this).apply {
            text = "লগআউট"
            setOnClickListener {
                auth.signOut()
                showLoginScreen()
            }
        }
        layout.addView(logoutButton)

        this.copyButton = copyButton
        this.shareButton = shareButton
        this.mapsButton = mapsButton

        setContentView(layout)
    }

    private lateinit var copyButton: Button
    private lateinit var shareButton: Button
    private lateinit var mapsButton: Button

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
        mapsButton.isEnabled = false

        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
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
                mapsButton.isEnabled = true
            }.addOnFailureListener {
                status.text = "লোকেশন পেতে সমস্যা হয়েছে।"
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

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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

    private fun openGoogleMaps() {
        val lat = latitude ?: return
        val lon = longitude ?: return

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://maps.google.com/?q=$lat,$lon")
        )
        startActivity(intent)
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
