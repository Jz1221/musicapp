package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musicapp.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    // View View Binding for accessing UI elements
    private lateinit var binding: ActivitySignUpBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using view binding
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle "Next" button click (for sign-up)
        binding.btnNext.setOnClickListener {
            val name = binding.nameET.text.toString().trim()
            val email = binding.emailET.text.toString().trim()
            val password = binding.passET.text.toString().trim()
            val confirmPassword = binding.confirmPassET.text.toString().trim()

            // Check for empty fields first
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Missing Information")
                    .setMessage("Please enter all fields.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            // StringBuilder to collect validation error messages
            val errorMessages = StringBuilder()

            // Validate name (only letters and spaces, min 3 characters)
            if (name.length < 3 || !name.matches(Regex("^[a-zA-Z ]+$"))) {
                errorMessages.append("• Enter a valid name (letters only, min 3 characters)\n")
            }

            // Validate email format
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                errorMessages.append("• Enter a valid email address\n")
            }

            // Validate password strength
            if (!isStrongPassword(password)) {
                errorMessages.append("• Password must contain uppercase, lowercase, digit, and symbol (min 6 characters)\n")
            }

            // Check if password and confirmation match
            if (password != confirmPassword) {
                errorMessages.append("• Passwords do not match\n")
            }

            // If there are validation errors, show them in an AlertDialog
            if (errorMessages.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Validation Errors")
                    .setMessage(errorMessages.toString())
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val database = FirebaseDatabase.getInstance().getReference("users")

                        // Create a user data map
                        val user = mapOf(
                            "id" to uid,
                            "name" to name,
                            "email" to email
                        )

                        // Save user data in Firebase Realtime Database
                        uid?.let {
                            database.child(it).setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()

                                    // Navigate to OTP verification screen and pass the email
                                    val intent = Intent(this, OtpActivity::class.java)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    // This runs only if saving to Firebase fails
                                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        // If sign-up failed, show error message
                        Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Handle "Login" text click – navigate to login page (MainActivity)
        binding.login.setOnClickListener {
            val intent = Intent(this@SignUpActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Handle window insets (status/navigation bars) for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Helper function to validate strong passwords
    private fun isStrongPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,}$")
        return passwordPattern.matches(password)
    }
}
