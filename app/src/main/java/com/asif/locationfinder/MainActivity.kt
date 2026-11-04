package com.asif.locationfinder

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import java.util.UUID

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_LOCATION = 4101
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database by lazy {
        FirebaseDatabase.getInstance(
            "https://locationfinder-3df35-default-rtdb.firebaseio.com/"
        ).reference
    }

    private lateinit var layout: LinearLayout
    private lateinit var message: TextView
    private var pendingSessionId: String? = null
    private var sessionListener: ValueEventListener? = null
    private var locationListener: ValueEventListener? = null
    private var currentSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun setupLayout(titleText: String) {
        removeSessionListeners()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        val scroll = ScrollView(this)
        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 36, 32, 24)
        }
        val title = TextView(this).apply {
            text = titleText
            textSize = 25f
            setTextColor(0xFF172033.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        }
        layout.addView(title)
        message = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF354052.toInt())
            setPadding(0, 0, 0, 18)
        }
        layout.addView(message)
        scroll.addView(layout)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val footer = TextView(this).apply {
            text = "Power by : MD Asif Sorkar"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF687386.toInt())
            setPadding(8, 12, 8, 16)
        }
        root.addView(footer)
        setContentView(root)
    }

    private fun addButton(text: String, action: () -> Unit) {
        val button = Button(this).apply { this.text = text }
        layout.addView(button, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })
        button.setOnClickListener { action() }
    }

    private fun showHome() {
        val user = auth.currentUser
        if (user != null && user.isEmailVerified) {
            showDashboard()
        } else {
            if (user != null) auth.signOut()
            showLogin()
        }
    }

    private fun showLogin() {
        setupLayout("Location Finder")
        val email = EditText(this).apply {
            hint = "ইমেইল ঠিকানা"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = EditText(this).apply {
            hint = "পাসওয়ার্ড"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(email)
        layout.addView(password)
        addButton("লগইন করুন") {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (e.isBlank() || p.isBlank()) {
                message.text = "ইমেইল ও পাসওয়ার্ড লিখুন।"
            } else {
                auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener { result ->
                        result.user?.reload()?.addOnSuccessListener {
                            if (result.user?.isEmailVerified == true) showDashboard()
                            else {
                                auth.signOut()
                                message.text = "আগে ইমেইল ভেরিফিকেশন সম্পন্ন করুন।"
                            }
                        }
                    }
                    .addOnFailureListener { message.text = friendlyError(it.message) }
            }
        }
        addButton("নতুন অ্যাকাউন্ট তৈরি করুন") { showRegister() }
        addButton("পাসওয়ার্ড ভুলে গেছেন?") { showResetPassword() }
    }

    private fun showRegister() {
        setupLayout("নতুন অ্যাকাউন্ট")
        val email = EditText(this).apply { hint = "আপনার ইমেইল" }
        val password = EditText(this).apply {
            hint = "কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(email)
        layout.addView(password)
        addButton("অ্যাকাউন্ট তৈরি করুন") {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (e.isBlank() || p.length < 6) {
                message.text = "সঠিক ইমেইল ও কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন।"
                return@addButton
            }
            auth.createUserWithEmailAndPassword(e, p)
                .addOnSuccessListener { result ->
                    result.user?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            auth.signOut()
                            showLogin()
                            message.text = "আপনার ইমেইলে ভেরিফিকেশন লিংক পাঠানো হয়েছে। ইমেইল যাচাই করে লগইন করুন।"
                        }
                        ?.addOnFailureListener { message.text = friendlyError(it.message) }
                }
                .addOnFailureListener { message.text = friendlyError(it.message) }
        }
        addButton("ফিরে যান") { showLogin() }
    }

    private fun showResetPassword() {
        setupLayout("পাসওয়ার্ড রিসেট")
        val email = EditText(this).apply { hint = "আপনার ইমেইল" }
        layout.addView(email)
        addButton("রিসেট লিংক পাঠান") {
            val e = email.text.toString().trim()
            if (e.isBlank()) message.text = "ইমেইল লিখুন।"
            else auth.sendPasswordResetEmail(e)
                .addOnSuccessListener { message.text = "পাসওয়ার্ড রিসেট লিংক ইমেইলে পাঠানো হয়েছে।" }
                .addOnFailureListener { message.text = friendlyError(it.message) }
        }
        addButton("ফিরে যান") { showLogin() }
    }

    private fun showDashboard() {
        setupLayout("Location Finder")
        message.text = "স্বাগতম! আমন্ত্রণ তৈরি করে কাউকে যুক্ত করুন, অথবা পাওয়া আমন্ত্রণ কোড গ্রহণ করুন।\n\nকেউ আমন্ত্রণ লিংক/কোড পেলেই লোকেশন দেখা যাবে না। দুজনের আলাদা সম্মতি আবশ্যক।"
        addButton("নতুন আমন্ত্রণ তৈরি করুন") { createInvitation() }
        addButton("আমন্ত্রণ কোড গ্রহণ করুন") { showJoinInvitation() }
        addButton("লগআউট") {
            stopLocalService()
            auth.signOut()
            showLogin()
        }
    }

    private fun createInvitation() {
        val user = auth.currentUser ?: return
        val sessionId = database.child("sessions").push().key ?: UUID.randomUUID().toString()
        val code = UUID.randomUUID().toString().replace("-", "")
            .take(12).uppercase(Locale.ROOT)
        val session = mapOf(
            "userA" to user.uid,
            "userB" to "",
            "inviteCode" to code,
            "consentA" to false,
            "consentB" to false,
            "active" to false,
            "stopped" to false,
            "createdAt" to ServerValue.TIMESTAMP
        )
        val invite = mapOf(
            "sessionId" to sessionId,
            "createdBy" to user.uid,
            "status" to "pending",
            "createdAt" to ServerValue.TIMESTAMP
        )
        val updates = hashMapOf<String, Any>(
            "sessions/$sessionId" to session,
            "invites/$code" to invite
        )
        database.updateChildren(updates)
            .addOnSuccessListener { showCreatedInvitation(sessionId, code) }
            .addOnFailureListener { message.text = "আমন্ত্রণ তৈরি হয়নি: ${friendlyError(it.message)}" }
    }

    private fun showCreatedInvitation(sessionId: String, code: String) {
        setupLayout("আমন্ত্রণ তৈরি হয়েছে")
        message.text = "আমন্ত্রণ কোড:\n$code\n\nএই কোডটি নির্দিষ্ট ব্যক্তিকে পাঠান। কোড পাওয়া মানেই লোকেশন শেয়ার শুরু নয়। আপনি নিজে সম্মতি না দেওয়া এবং অপর ব্যক্তি কোড গ্রহণ করে সম্মতি না দেওয়া পর্যন্ত লোকেশন বন্ধ থাকবে।"
        addButton("আমি লোকেশন শেয়ার করতে সম্মতি দিচ্ছি") {
            confirm("আপনি কি নিজের লাইভ লোকেশন এই ব্যক্তির সঙ্গে শেয়ার করতে সম্মত?") {
                giveConsent(sessionId, true)
            }
        }
        addButton("সেশন দেখুন") { watchSession(sessionId) }
        addButton("হোমে ফিরুন") { showDashboard() }
    }

    private fun showJoinInvitation() {
        setupLayout("আমন্ত্রণ গ্রহণ")
        val codeInput = EditText(this).apply {
            hint = "১২ অক্ষরের আমন্ত্রণ কোড"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        layout.addView(codeInput)
        addButton("কোড যাচাই করুন") {
            val code = codeInput.text.toString().trim().uppercase(Locale.ROOT)
            if (code.length != 12) {
                message.text = "সঠিক ১২ অক্ষরের কোড লিখুন।"
                return@addButton
            }
            database.child("invites").child(code).get()
                .addOnSuccessListener { invite ->
                    val status = invite.child("status").getValue(String::class.java)
                    val sessionId = invite.child("sessionId").getValue(String::class.java)
                    val creator = invite.child("createdBy").getValue(String::class.java)
                    if (!invite.exists() || status != "pending" || sessionId.isNullOrBlank()) {
                        message.text = "কোডটি সঠিক নয়, মেয়াদ শেষ, অথবা ইতিমধ্যে গ্রহণ করা হয়েছে।"
                    } else if (creator == auth.currentUser?.uid) {
                        message.text = "নিজের তৈরি আমন্ত্রণ নিজে গ্রহণ করা যাবে না।"
                    } else {
                        confirm("আপনি কি আমন্ত্রণটি গ্রহণ করে নিজের লাইভ লোকেশন শেয়ার করতে স্পষ্টভাবে সম্মতি দিচ্ছেন? সম্মতি না দিলে কোনো লোকেশন শেয়ার হবে না।") {
                            acceptInvitation(code, sessionId)
                        }
                    }
                }
                .addOnFailureListener { message.text = "কোড যাচাই করা যায়নি: ${friendlyError(it.message)}" }
        }
        addButton("ফিরে যান") { showDashboard() }
    }

    private fun acceptInvitation(code: String, sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        val sessionRef = database.child("sessions").child(sessionId)
        sessionRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (!data.hasChild("userA") || data.child("stopped").getValue(Boolean::class.java) == true) {
                    return Transaction.abort()
                }
                val existing = data.child("userB").getValue(String::class.java)
                if (!existing.isNullOrBlank() && existing != uid) return Transaction.abort()
                data.child("userB").value = uid
                data.child("consentB").value = true
                val consentA = data.child("consentA").getValue(Boolean::class.java) ?: false
                data.child("active").value = consentA
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null || !committed) {
                    message.text = "আমন্ত্রণটি আর গ্রহণ করা যাচ্ছে না। হয়তো অন্য কেউ গ্রহণ করেছেন বা সেশন বন্ধ।"
                    return
                }
                database.child("invites").child(code).child("status").setValue("accepted")
                    .addOnSuccessListener {
                        watchSession(sessionId)
                        ensureLocationPermissionAndStart(sessionId)
                    }
                    .addOnFailureListener { message.text = "সেশন গ্রহণ হয়েছে, তবে আমন্ত্রণের অবস্থা হালনাগাদ হয়নি: ${friendlyError(it.message)}" }
            }
        })
    }

    private fun giveConsent(sessionId: String, consent: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.child("sessions").child(sessionId)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.child("userA").getValue(String::class.java) != uid) return Transaction.abort()
                if (data.child("stopped").getValue(Boolean::class.java) == true) return Transaction.abort()
                data.child("consentA").value = consent
                val consentB = data.child("consentB").getValue(Boolean::class.java) ?: false
                val userB = data.child("userB").getValue(String::class.java)
                data.child("active").value = consent && consentB && !userB.isNullOrBlank()
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null || !committed) {
                    message.text = "সম্মতি সংরক্ষণ করা যায়নি: ${error?.message ?: "সেশন বন্ধ"}"
                    return
                }
                watchSession(sessionId)
                if (consent) ensureLocationPermissionAndStart(sessionId) else stopLocalService()
            }
        })
    }

    private fun watchSession(sessionId: String) {
        setupLayout("লোকেশন শেয়ারিং সেশন")
        currentSessionId = sessionId
        val sessionRef = database.child("sessions").child(sessionId)
        sessionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    message.text = "সেশনটি পাওয়া যাচ্ছে না।"
                    return
                }
                val uid = auth.currentUser?.uid ?: return
                val userA = snapshot.child("userA").getValue(String::class.java)
                val userB = snapshot.child("userB").getValue(String::class.java)
                val consentA = snapshot.child("consentA").getValue(Boolean::class.java) ?: false
                val consentB = snapshot.child("consentB").getValue(Boolean::class.java) ?: false
                val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val stopped = snapshot.child("stopped").getValue(Boolean::class.java) ?: false
                val otherUid = if (uid == userA) userB else userA
                message.text = "সেশন অবস্থা: ${if (stopped) "বন্ধ" else if (active && consentA && consentB) "দুজনের সম্মতিতে সক্রিয়" else "সম্মতির অপেক্ষায়"}\n\nআপনার সম্মতি: ${if ((uid == userA && consentA) || (uid == userB && consentB)) "দেওয়া হয়েছে" else "দেওয়া হয়নি"}\nঅপর পক্ষের সম্মতি: ${if ((uid == userA && consentB) || (uid == userB && consentA)) "দেওয়া হয়েছে" else "দেওয়া হয়নি"}"
                if (otherUid.isNullOrBlank()) {
                    message.append("\n\nঅপর ব্যক্তি এখনো কোড গ্রহণ করেননি।")
                } else {
                    listenForOtherLocation(sessionId, otherUid)
                }
                if (uid == userA && !consentA && !stopped) {
                    addButtonOnce("আমি নিজের লোকেশন শেয়ার করতে সম্মতি দিচ্ছি") {
                        confirm("নিজের লাইভ লোকেশন শেয়ার করতে সম্মত?") { giveConsent(sessionId, true) }
                    }
                }
                if (!stopped && (consentA || consentB)) {
                    addButtonOnce("আমার সম্মতি প্রত্যাহার করুন") {
                        confirm("আপনার সম্মতি প্রত্যাহার করলে লাইভ লোকেশন শেয়ারিং বন্ধ হবে।") {
                            withdrawMyConsent(sessionId, uid, userA, userB)
                        }
                    }
                }
                if (!stopped) {
                    addButtonOnce("সেশন সম্পূর্ণ বন্ধ করুন") {
                        confirm("এই সেশন বন্ধ করলে দুজনের লোকেশন শেয়ারিং বন্ধ হবে।") { stopSession(sessionId) }
                    }
                }
                addButtonOnce("হোমে ফিরুন") { showDashboard() }
            }
            override fun onCancelled(error: DatabaseError) {
                message.text = "সেশন পড়া যায়নি: ${error.message}"
            }
        }
        sessionRef.addValueEventListener(sessionListener!!)
    }

    private val addedButtons = mutableSetOf<String>()
    private fun addButtonOnce(label: String, action: () -> Unit) {
        if (addedButtons.add(label)) addButton(label, action)
    }

    private fun listenForOtherLocation(sessionId: String, otherUid: String) {
        locationListener?.let { database.child("sessions").child(sessionId).child("locations").child(otherUid).removeEventListener(it) }
        val ref = database.child("sessions").child(sessionId).child("locations").child(otherUid)
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lon = snapshot.child("longitude").getValue(Double::class.java)
                if (lat != null && lon != null) {
                    message.append("\n\nঅপর পক্ষের সর্বশেষ অবস্থান: %.5f, %.5f".format(Locale.US, lat, lon))
                    addButtonOnce("মানচিত্রে অবস্থান দেখুন") {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(locationListener!!)
    }

    private fun withdrawMyConsent(sessionId: String, uid: String, userA: String?, userB: String?) {
        val field = if (uid == userA) "consentA" else if (uid == userB) "consentB" else null
        if (field == null) return
        val updates = mapOf("sessions/$sessionId/$field" to false, "sessions/$sessionId/active" to false)
        database.updateChildren(updates)
            .addOnSuccessListener { stopLocalService(); watchSession(sessionId) }
            .addOnFailureListener { message.text = "সম্মতি প্রত্যাহার করা যায়নি: ${friendlyError(it.message)}" }
    }

    private fun stopSession(sessionId: String) {
        database.child("sessions").child(sessionId).updateChildren(
            mapOf("active" to false, "stopped" to true)
        ).addOnSuccessListener {
            stopLocalService()
            watchSession(sessionId)
        }.addOnFailureListener { message.text = "সেশন বন্ধ করা যায়নি: ${friendlyError(it.message)}" }
    }

    private fun ensureLocationPermissionAndStart(sessionId: String) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            startLocationService(sessionId)
        } else {
            pendingSessionId = sessionId
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    private fun startLocationService(sessionId: String) {
        val intent = Intent(this, LocationService::class.java)
            .putExtra(LocationService.EXTRA_SESSION_ID, sessionId)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (e: Exception) {
            message.text = "লোকেশন সার্ভিস চালু করা যায়নি। অ্যাপ খোলা রেখে আবার চেষ্টা করুন: ${e.message}"
        }
    }

    private fun stopLocalService() {
        stopService(Intent(this, LocationService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            val sessionId = pendingSessionId
            pendingSessionId = null
            if (granted && sessionId != null) startLocationService(sessionId)
            else message.text = "লোকেশন অনুমতি না দিলে আপনার ডিভাইসের অবস্থান শেয়ার হবে না।"
        }
    }

    private fun confirm(text: String, accepted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("স্পষ্ট সম্মতি")
            .setMessage(text)
            .setNegativeButton("না, ফিরে যান", null)
            .setPositiveButton("হ্যাঁ, সম্মতি দিচ্ছি") { _, _ -> accepted() }
            .show()
    }

    private fun friendlyError(error: String?): String = error ?: "অজানা সমস্যা হয়েছে।"

    private fun removeSessionListeners() {
        currentSessionId?.let { id ->
            sessionListener?.let { database.child("sessions").child(id).removeEventListener(it) }
            locationListener?.let { /* listener is removed when a new session screen is opened */ }
        }
        sessionListener = null
        locationListener = null
        currentSessionId = null
        addedButtons.clear()
    }

    override fun onDestroy() {
        removeSessionListeners()
        super.onDestroy()
    }
}
