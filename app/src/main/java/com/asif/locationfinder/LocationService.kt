
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

    private var sessionListener: ValueEventListener? = null
    private var locationCallback: LocationCallback? = null

    private var sessionId: String? = null
    private var uid: String? = null
    private var locationStarted = false
    private var serviceStopped = false

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

        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        sessionRef = database.getReference("sessions").child(id)

        listenForSession()

        return START_NOT_STICKY
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun listenForSession() {

        sessionListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (serviceStopped) return

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

                val stopped = snapshot.child("stopped")
                    .getValue(Boolean::class.java) ?: false

                val isMember = currentUid == userA ||
                        currentUid == userB

                if (
                    snapshot.exists() &&
                    isMember &&
                    userA != null &&
                    !userB.isNullOrBlank() &&
                    consentA &&
                    consentB &&
                    active &&
                    !stopped
                ) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                    stopServicePermanently()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                stopLocationUpdates()
                stopServicePermanently()
            }
        }

        sessionRef.addValueEventListener(sessionListener!!)
    }

    private fun startLocationUpdates() {

        if (locationStarted || serviceStopped) return

        if (!hasLocationPermission()) {
            stopServicePermanently()
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
            .setMinUpdateIntervalMillis(
                TimeUnit.SECONDS.toMillis(5)
            )
            .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                if (serviceStopped) return

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
            ).addOnFailureListener {
                stopLocationUpdates()
                stopServicePermanently()
            }

            locationStarted = true

        } catch (e: SecurityException) {
            stopServicePermanently()
        }
    }

    private fun stopLocationUpdates() {

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }

        locationCallback = null
        locationStarted = false
    }

    private fun stopServicePermanently() {

        if (serviceStopped) return

        serviceStopped = true
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

        serviceStopped = true
        stopLocationUpdates()

        if (::sessionRef.isInitialized && sessionListener != null) {
            sessionRef.removeEventListener(sessionListener!!)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
