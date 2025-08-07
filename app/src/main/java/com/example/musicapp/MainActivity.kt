package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MainActivity : AppCompatActivity() {

    // View binding to access views from activity_main.xml
    lateinit var binding: ActivityMainBinding
    // Firebase Authentication instance
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge UI support (for full screen layout)
        enableEdgeToEdge()

        // Inflate the layout and bind views
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Navigate to SignUpActivity when "Sign up" TextView is clicked
        binding.signupTv.setOnClickListener {
            val intent = Intent(this@MainActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Find and set click listener for "Forgot Password" TextView
        val forgotPasswordText = findViewById<TextView>(R.id.ForgotPassword)

        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        // Handle login button click
        binding.loginbtn.setOnClickListener {
            val email = binding.emailET.text.toString().trim()
            val password = binding.passET.text.toString().trim()

            // Basic input validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            } else {
                // Attempt Firebase Authentication
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            // Access user's data in Realtime Database
                            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid)

                            dbRef.get().addOnSuccessListener { snapshot ->
                                val dbEmail = snapshot.child("email").getValue(String::class.java)

                                // Double-check email from database for security
                                if (dbEmail == email) {
                                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                                    // Navigate to HomeScreen
                                    val intent = Intent(this, HomeScreen::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Force sign out if email does not match database
                                    auth.signOut()
                                    Toast.makeText(this, "Email not found in database!", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                // Handle database fetch failure
                                auth.signOut()
                                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "UID not found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle Firebase login failure with specific error messages
                        val errorMessage = when (val exception = authTask.exception) {
                            is FirebaseAuthInvalidUserException -> "No account found with this email."
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                            else -> exception?.message ?: "Login failed"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
