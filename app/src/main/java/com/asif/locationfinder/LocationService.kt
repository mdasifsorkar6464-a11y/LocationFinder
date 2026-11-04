
package com.asif.locationfinder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val CHANNEL_ID = "location_sharing_channel"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var sessionRef: DatabaseReference
    private lateinit var locationRef: DatabaseReference

    private var locationCallback: LocationCallback? = null
    private var sessionListener: ValueEventListener? = null
    private var sessionId: String? = null
    private var uid: String? = null
    private var locationStarted = false

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        database = FirebaseDatabase.getInstance(
            "https://locationfinder-3df35-default-rtdb.firebaseio.com/"
        )

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val id = intent?.getStringExtra(EXTRA_SESSION_ID)

        if (id.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        sessionId = id
        uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        sessionRef = database.getReference("sessions").child(id)

        listenForConsent()

        return START_NOT_STICKY
    }

    private fun listenForConsent() {
        sessionListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val currentUid = uid ?: return

                val userA = snapshot.child("userA")
                    .getValue(String::class.java)

                val userB = snapshot.child("userB")
                    .getValue(String::class.java)

                val consentA = snapshot.child("consentA")
                    .getValue(Boolean::class.java) ?: false

                val consentB = snapshot.child("consentB")
                    .getValue(Boolean::class.java) ?: false

                val active = snapshot.child("active")
                    .getValue(Boolean::class.java) ?: false

                val isMember = currentUid == userA ||
                        currentUid == userB

                if (active && consentA && consentB && isMember) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                    stopSelf()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                stopLocationUpdates()
                stopSelf()
            }
        }

        sessionRef.addValueEventListener(sessionListener!!)
    }

    private fun startLocationUpdates() {

        if (locationStarted) return

        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            stopSelf()
            return
        }

        val currentUid = uid ?: return
        val currentSession = sessionId ?: return

        locationRef = database.getReference("sessions")
            .child(currentSession)
            .child("locations")
            .child(currentUid)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(10)
        )
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
            .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                val data = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "updatedAt" to ServerValue.TIMESTAMP
                )

                locationRef.setValue(data)
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                request,
                locationCallback!!,
                mainLooper
            )

            locationStarted = true

        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }

        locationCallback = null
        locationStarted = false
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Finder")
            .setContentText("আপনার লোকেশন শেয়ারিং চালু আছে")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        stopLocationUpdates()

        if (::sessionRef.isInitialized && sessionListener != null) {
            sessionRef.removeEventListener(sessionListener!!)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
