
package com.asif.locationfinder

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

class MainActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy {
        FirebaseDatabase.getInstance(
            "https://locationfinder-3df35-default-rtdb.firebaseio.com/"
        ).reference
    }

    private lateinit var root: LinearLayout
    private lateinit var message: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun setup(titleText: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 30)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = titleText
            textSize = 25f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        message = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 20)
        }
        root.addView(message)

        setContentView(root)
    }

    private fun addButton(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
        }
        root.addView(button)
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
        setup("Location Finder")

        email = EditText(this).apply {
            hint = "ইমেইল ঠিকানা"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        root.addView(email)

        password = EditText(this).apply {
            hint = "পাসওয়ার্ড"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(password)

        addButton("লগইন করুন") { loginUser() }
        addButton("নতুন অ্যাকাউন্ট তৈরি করুন") { showRegister() }
        addButton("পাসওয়ার্ড ভুলে গেছেন?") { showReset() }
    }

    private fun showRegister() {
        setup("নতুন অ্যাকাউন্ট")

        email = EditText(this).apply {
            hint = "ইমেইল ঠিকানা"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        root.addView(email)

        password = EditText(this).apply {
            hint = "কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(password)

        addButton("অ্যাকাউন্ট তৈরি করুন") {
            val e = email.text.toString().trim()
            val p = password.text.toString()

            if (e.isBlank() || p.length < 6) {
                message.text = "সঠিক ইমেইল ও পাসওয়ার্ড দিন।"
                return@addButton
            }

            auth.createUserWithEmailAndPassword(e, p)
                .addOnSuccessListener { result ->
                    result.user?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            auth.signOut()
                            showLogin()
                            message.text =
                                "ইমেইলে ভেরিফিকেশন লিংক পাঠানো হয়েছে।"
                        }
                        ?.addOnFailureListener {
                            message.text = it.message ?: "ইমেইল পাঠানো যায়নি।"
                        }
                }
                .addOnFailureListener {
                    message.text = it.message ?: "অ্যাকাউন্ট তৈরি হয়নি।"
                }
        }

        addButton("ফিরে যান") { showLogin() }
    }

    private fun loginUser() {
        val e = email.text.toString().trim()
        val p = password.text.toString()

        auth.signInWithEmailAndPassword(e, p)
            .addOnSuccessListener { result ->
                result.user?.reload()?.addOnSuccessListener {
                    if (auth.currentUser?.isEmailVerified == true) {
                        showDashboard()
                    } else {
                        auth.signOut()
                        showLogin()
                        message.text = "আগে ইমেইল ভেরিফিকেশন করুন।"
                    }
                }
            }
            .addOnFailureListener {
                message.text = it.message ?: "লগইন ব্যর্থ হয়েছে।"
            }
    }

    private fun showReset() {
        setup("পাসওয়ার্ড রিসেট")

        val input = EditText(this).apply {
            hint = "আপনার ইমেইল"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        root.addView(input)

        addButton("রিসেট লিংক পাঠান") {
            val e = input.text.toString().trim()
            if (e.isBlank()) {
                message.text = "ইমেইল লিখুন।"
                return@addButton
            }

            auth.sendPasswordResetEmail(e)
                .addOnSuccessListener {
                    message.text = "রিসেট লিংক ইমেইলে পাঠানো হয়েছে।"
                }
                .addOnFailureListener {
                    message.text = it.message ?: "লিংক পাঠানো যায়নি।"
                }
        }

        addButton("ফিরে যান") { showLogin() }
    }

    private fun showDashboard() {
        setup("Location Finder")
        message.text = "আপনি লগইন করেছেন।"

        addButton("নতুন আমন্ত্রণ তৈরি করুন") {
            createInvitation()
        }

        addButton("আমন্ত্রণ কোড গ্রহণ করুন") {
            showJoinInvitation()
        }

        addButton("লগআউট") {
            auth.signOut()
            showLogin()
        }
    }

    private fun createInvitation() {
        val user = auth.currentUser ?: return

        setup("আমন্ত্রণ তৈরি")

        val consent = CheckBox(this).apply {
            text = "আমি এই ব্যক্তির সঙ্গে লোকেশন শেয়ার করতে সম্মত।"
            setTextColor(Color.BLACK)
        }
        root.addView(consent)

        addButton("সম্মতি দিয়ে কোড তৈরি করুন") {
            if (!consent.isChecked) {
                message.text = "আগে সম্মতি দিন।"
                return@addButton
            }

            val sessionId = UUID.randomUUID().toString()
            val code = UUID.randomUUID().toString()
                .replace("-", "")
                .uppercase()

            val session = mapOf(
                "userA" to user.uid,
                "userB" to "",
                "consentA" to true,
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

            db.updateChildren(updates)
                .addOnSuccessListener {
                    setup("আমন্ত্রণ তৈরি হয়েছে")
                    message.text =
                        "আপনার আমন্ত্রণ কোড:\n\n$code\n\n" +
                        "এই কোডটি অন্য ব্যক্তিকে দিন। " +
                        "তার সম্মতি ছাড়া লোকেশন শেয়ার হবে না।"

                    addButton("কোড শেয়ার করুন") {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "আমার Location Finder আমন্ত্রণ কোড: $code"
                            )
                        }
                        startActivity(
                            Intent.createChooser(share, "আমন্ত্রণ শেয়ার করুন")
                        )
                    }

                    addButton("হোমে ফিরে যান") { showDashboard() }
                }
                .addOnFailureListener {
                    message.text = "সমস্যা: ${it.message}"
                }
        }

        addButton("ফিরে যান") { showDashboard() }
    }

    private fun showJoinInvitation() {
        setup("আমন্ত্রণ গ্রহণ")

        val codeInput = EditText(this).apply {
            hint = "আমন্ত্রণ কোড লিখুন"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        root.addView(codeInput)

        addButton("কোড যাচাই করুন") {
            val code = codeInput.text.toString().trim().uppercase()
            if (code.isBlank()) {
                message.text = "আমন্ত্রণ কোড লিখুন।"
                return@addButton
            }
            verifyInvitation(code)
        }

        addButton("ফিরে যান") { showDashboard() }
    }

    private fun verifyInvitation(code: String) {
        val user = auth.currentUser ?: return

        db.child("invites").child(code).get()
            .addOnSuccessListener { invite ->
                if (!invite.exists() ||
                    invite.child("status").getValue(String::class.java) != "pending"
                ) {
                    message.text = "কোডটি ভুল অথবা আর কার্যকর নেই।"
                    return@addOnSuccessListener
                }

                val sessionId = invite.child("sessionId")
                    .getValue(String::class.java) ?: return@addOnSuccessListener

                val creator = invite.child("createdBy")
                    .getValue(String::class.java)

                if (creator == user.uid) {
                    message.text = "নিজের আমন্ত্রণ নিজে গ্রহণ করা যাবে না।"
                    return@addOnSuccessListener
                }

                showConsentScreen(code, sessionId)
            }
            .addOnFailureListener {
                message.text = "কোড যাচাই করা যায়নি: ${it.message}"
            }
    }

    private fun showConsentScreen(code: String, sessionId: String) {
        setup("লোকেশন শেয়ারিং সম্মতি")

        val consent = CheckBox(this).apply {
            text = "আমি স্বেচ্ছায় এই ব্যক্তির সঙ্গে আমার লোকেশন শেয়ার করতে সম্মত।"
            setTextColor(Color.BLACK)
        }
        root.addView(consent)

        addButton("সম্মতি দিয়ে যোগ দিন") {
            if (!consent.isChecked) {
                message.text = "যোগ দিতে হলে সম্মতি দিতে হবে।"
                return@addButton
            }

            val user = auth.currentUser ?: return@addButton
            val sessionRef = db.child("sessions").child(sessionId)

            sessionRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {
                    val userA = currentData.child("userA")
                        .getValue(String::class.java)
                    val userB = currentData.child("userB")
                        .getValue(String::class.java)
                    val active = currentData.child("active")
                        .getValue(Boolean::class.java) ?: false
                    val stopped = currentData.child("stopped")
                        .getValue(Boolean::class.java) ?: false

                    if (userA == null || userB != "" || active || stopped) {
                        return Transaction.abort()
                    }

                    currentData.child("userB").value = user.uid
                    currentData.child("consentB").value = true
                    currentData.child("active").value = true
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    if (error != null) {
                        message.text = "সমস্যা: ${error.message}"
                    } else if (committed) {
                        db.child("invites").child(code)
                            .child("status").setValue("accepted")
                        setup("আমন্ত্রণ গ্রহণ সম্পন্ন")
                        message.text =
                            "দুজনের সম্মতি সম্পন্ন হয়েছে। " +
                            "পরবর্তী ধাপে লাইভ লোকেশন চালু হবে।"
                        addButton("হোমে ফিরে যান") { showDashboard() }
                    } else {
                        message.text = "আমন্ত্রণটি আর গ্রহণ করা যাচ্ছে না।"
                    }
                }
            })
        }

        addButton("বাতিল করুন") { showDashboard() }
    }

    private fun stopSharing(sessionId: String) {
        db.child("sessions").child(sessionId).updateChildren(
            mapOf(
                "active" to false,
                "stopped" to true
            )
        ).addOnSuccessListener {
            message.text = "লোকেশন শেয়ারিং বন্ধ হয়েছে।"
        }.addOnFailureListener {
            message.text = "বন্ধ করা যায়নি: ${it.message}"
        }
    }
}
