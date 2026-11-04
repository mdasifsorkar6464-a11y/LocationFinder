
package com.asif.locationfinder

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.text.InputType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.UUID

class MainActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val database by lazy {
        FirebaseDatabase.getInstance(
            "https://locationfinder-3df35-default-rtdb.firebaseio.com/"
        ).reference
    }

    private lateinit var layout: LinearLayout
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun setupLayout(titleText: String) {
        layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(36, 50, 36, 30)
        layout.setBackgroundColor(Color.WHITE)

        val title = TextView(this)
        title.text = titleText
        title.textSize = 25f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER
        layout.addView(title)

        message = TextView(this)
        message.textSize = 15f
        message.setTextColor(Color.DKGRAY)
        layout.addView(message)

        setContentView(layout)
    }

    private fun addButton(text: String, action: () -> Unit) {
        val button = Button(this)
        button.text = text
        layout.addView(button)
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

        email = EditText(this)
        email.hint = "ইমেইল ঠিকানা"
        email.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        layout.addView(email)

        password = EditText(this)
        password.hint = "পাসওয়ার্ড"
        password.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(password)

        addButton("লগইন করুন") { loginUser() }
        addButton("নতুন অ্যাকাউন্ট তৈরি করুন") { showRegister() }
        addButton("পাসওয়ার্ড ভুলে গেছেন?") { resetPassword() }
    }

    private fun showRegister() {
        setupLayout("নতুন অ্যাকাউন্ট")

        email = EditText(this)
        email.hint = "আপনার ইমেইল"
        layout.addView(email)

        password = EditText(this)
        password.hint = "কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড"
        password.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
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
                            message.text =
                                "আপনার ইমেইলে ভেরিফিকেশন লিংক পাঠানো হয়েছে।"
                        }
                        ?.addOnFailureListener {
                            message.text = it.message
                        }
                }
                .addOnFailureListener {
                    message.text = it.message
                }
        }

        addButton("ফিরে যান") { showLogin() }
    }

    private fun loginUser() {
        val e = email.text.toString().trim()
        val p = password.text.toString()

        auth.signInWithEmailAndPassword(e, p)
            .addOnSuccessListener { result ->
                val user = result.user

                user?.reload()?.addOnSuccessListener {
                    if (user.isEmailVerified) {
                        showDashboard()
                    } else {
                        auth.signOut()
                        showLogin()
                        message.text =
                            "আগে ইমেইল ভেরিফিকেশন সম্পন্ন করুন।"
                    }
                }
            }
            .addOnFailureListener {
                message.text = it.message
            }
    }

    private fun resetPassword() {
        setupLayout("পাসওয়ার্ড রিসেট")

        email = EditText(this)
        email.hint = "আপনার ইমেইল"
        layout.addView(email)

        addButton("রিসেট লিংক পাঠান") {
            val e = email.text.toString().trim()

            if (e.isBlank()) {
                message.text = "ইমেইল লিখুন।"
                return@addButton
            }

            auth.sendPasswordResetEmail(e)
                .addOnSuccessListener {
                    message.text =
                        "পাসওয়ার্ড রিসেট লিংক ইমেইলে পাঠানো হয়েছে।"
                }
                .addOnFailureListener {
                    message.text = it.message
                }
        }

        addButton("ফিরে যান") { showLogin() }
    }

    private fun showDashboard() {
        setupLayout("Location Finder")

        message.text =
            "আপনি লগইন করেছেন।\n" +
            "লোকেশন শেয়ারিংয়ের জন্য আমন্ত্রণ তৈরি করুন।"

        addButton("আমন্ত্রণ কোড তৈরি করুন") {
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

        val sessionId = UUID.randomUUID().toString()
        val inviteCode = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(12)
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
            "createdAt" to ServerValue.TIMESTAMP,
            "status" to "pending"
        )

        val updates = hashMapOf<String, Any>(
            "sessions/$sessionId" to session,
            "invites/$inviteCode" to invite
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                setupLayout("আমন্ত্রণ তৈরি হয়েছে")
                message.text =
                    "আমন্ত্রণ কোড:\n$inviteCode\n\n" +
                    "এই কোডটি যাকে লোকেশন শেয়ার করতে চান তাকে দিন।\n" +
                    "তার স্পষ্ট সম্মতি ছাড়া লোকেশন শেয়ার হবে না।"
            }
            .addOnFailureListener {
                message.text = "সমস্যা: ${it.message}"
            }
    }

    private fun showJoinInvitation() {
        setupLayout("আমন্ত্রণ গ্রহণ")

        val code = EditText(this)
        code.hint = "আমন্ত্রণ কোড লিখুন"
        code.inputType = InputType.TYPE_CLASS_TEXT
        layout.addView(code)

        addButton("কোড যাচাই করুন") {
            message.text =
                "আমন্ত্রণ গ্রহণের নিরাপদ ব্যবস্থা পরবর্তী ধাপে যুক্ত করা হবে।"
        }

        addButton("ফিরে যান") { showDashboard() }
    }
}
