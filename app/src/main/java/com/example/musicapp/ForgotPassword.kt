package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {

    // FirebaseAuth instance for sending password reset email
    private lateinit var mAuth: FirebaseAuth

    // UI components
    private lateinit var emailField: EditText
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Apply padding for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Authentication instance
        mAuth = FirebaseAuth.getInstance()

        // Bind views from XML layout
        emailField = findViewById(R.id.et_email)
        btnReset = findViewById(R.id.btn_reset_password)
        val rememberPasswordText = findViewById<TextView>(R.id.tv_remember_password)

        // "Remember Password?" TextView click: return to login screen
        rememberPasswordText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Handle "Reset Password" button click
        btnReset.setOnClickListener {
            val email = emailField.text.toString().trim()

            // Check if email is entered
            if (email.isNotEmpty()) {
                resetPassword(email)
            } else {
                emailField.error = "Email field can't be empty"
            }
        }
    }

    // Function to handle password reset
    private fun resetPassword(email: String) {

        // Reference to the "users" node in Firebase Realtime Database
        val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")

        // Check if the email exists in the Realtime Database
        database.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {

                        // If email found in database, send password reset link
                        mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(this@ForgotPassword, "Password reset link has been sent to your email", Toast.LENGTH_LONG).show()

                                // Navigate back to login screen
                                startActivity(Intent(this@ForgotPassword, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Handle error in sending reset email
                                Toast.makeText(this@ForgotPassword, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Email not found in Firebase database
                        Toast.makeText(this@ForgotPassword, "Email not found in our records", Toast.LENGTH_SHORT).show()
                    }
                }

                // Handle Firebase database read error
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Toast.makeText(this@ForgotPassword, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}
