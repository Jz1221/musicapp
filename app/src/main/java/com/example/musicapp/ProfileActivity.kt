package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import android.widget.EditText
import android.widget.Toast


class ProfileActivity : AppCompatActivity() {

    // ViewBinding reference to access views in layout
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate layout using ViewBinding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inflate layout using ViewBinding
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load user info (username & email)
        showUserInfo()

        // Set current selected item on bottom nav bar
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile

        // Handle navigation item selection
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeScreen::class.java)
                    startActivity(intent)
                    true
                }
                R.id.playlist -> {
                    val intent = Intent(this, UserUploadMusic::class.java)
                    startActivity(intent)
                    true
                }
                R.id.favourites -> {
                    val intent = Intent(this, FavouriteMusicActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> true // Already on Profile
                else -> false
            }
        }

        // Edit Profile button click listener
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // App Info button click
        binding.btnAppInfo.setOnClickListener {
            showMessageBox("App Info", "🎵 Rileyify v1.0\nInspired by Me\nDeveloped by Song Jia Zheng\n 2025 All rights reserved")
        }

        // Terms & Conditions button click
        binding.btnTermsConditions.setOnClickListener {
            showMessageBox(
                "Terms and Conditions",
                """
        Welcome to Rileyify!

        By using this app, you agree to the following terms:

        1. **Respectful Use**
           - You must treat other users and content respectfully.
           - Offensive, inappropriate, or harmful content is not allowed.

        2. **Content Ownership**
           - Do not upload or share music you do not own or have permission to distribute.
           - Rileyify is not responsible for copyright violations by users.

        3. **Account Responsibility**
           - Keep your account credentials secure.
           - You are responsible for all activity under your account.

        4. **Prohibited Activities**
           - Do not attempt to hack, reverse engineer, or misuse the app.
           - Do not upload viruses, malware, or engage in illegal activity.

        5. **Termination**
           - We reserve the right to suspend or terminate accounts that violate these terms without prior notice.

        6. **Limitation of Liability**
           - Rileyify is provided “as-is”.
           - The developer is not liable for data loss, app crashes, or service interruptions.

        7. **Changes to Terms**
           - These terms may be updated at any time.
           - Continued use of the app constitutes acceptance of any changes.

        If you do not agree to these terms, please discontinue use of the app.
        """.trimIndent()
            )
        }

        // Sign Out button click
        binding.btnSignOut.setOnClickListener {
            // Confirmation Message
            AlertDialog.Builder(this)
                .setTitle("Sign Out Confirmation")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes") { dialog, which ->
                    dialog.dismiss()
                    // Yes

                    // Stop music playback if playing
                    MyExoplayer.release()

                    // Sign out from FirebaseAuth
                    FirebaseAuth.getInstance().signOut()

                    // Clear activity stack and return to login screen
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                //No
                .setNegativeButton("No") {dialog, which ->
                    dialog.dismiss()
                }
                .show()
        }

        // Change Password button click
        binding.btnChangePassword.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email

            if (!email.isNullOrEmpty()) {
                // Send password reset email
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showMessageBox("Reset Link Sent","A password reset link has been sent to: $email")
                        } else {
                            showMessageBox("Error", "Failed to send reset email. Try again later.")
                        }
                    }
            } else {
                showMessageBox("Error", "No email associated with this account.")
            }
        }

    }

    // Resume lifecycle - show mini player if song is playing
    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    // Show user info from FirebaseAuth and Realtime Database
    private fun showUserInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val email = user.email ?: "No Email"

            // Set email from FirebaseAuth
            binding.tvEmail.text = "Email: $email"

            // Get username from Firebase Realtime Database
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            database.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value.toString()
                    binding.tvUsername.text = "Username: $name"
                } else {
                    binding.tvUsername.text = "Username: Unknown"
                }
            }.addOnFailureListener {
                binding.tvUsername.text = "Username: Error"
                Toast.makeText(this, "Failed to fetch username", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.tvUsername.text = "Not logged in"
            binding.tvEmail.text = ""
        }
    }


    // Show dialog to edit username
    private fun showEditProfileDialog() {
        val editText = EditText(this)
        editText.hint = "Enter new username"

        AlertDialog.Builder(this)
            .setTitle("Edit Username")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newUsername = editText.text.toString().trim()
                if (newUsername.isNotEmpty()) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val uid = user?.uid
                    val email = user?.email

                    if (uid != null && email != null) {
                        // Update FirebaseAuth display name (optional)
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newUsername)
                            .build()

                        user.updateProfile(profileUpdates)

                        // Update username in Realtime Database
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                        val updatedUser = mapOf(
                            "id" to uid,
                            "name" to newUsername,
                            "email" to email // keep email fixed
                        )

                        database.child(uid).setValue(updatedUser)
                            .addOnSuccessListener {
                                showUserInfo()
                                showMessageBox("Success", "Username updated successfully.")
                            }
                            .addOnFailureListener { e ->
                                showMessageBox("Error", "Failed to update username: ${e.message}")
                            }
                    } else {
                        showMessageBox("Error", "User not authenticated.")
                    }
                } else {
                    showMessageBox("Input Required", "Username cannot be empty.")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    // Show bottom mini player if a song is playing
    private fun showPlayerView() {
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        MyExoplayer.getCurrentSong()?.let {
            // If song is playing, show mini player with song title and image
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing: " + it.title
            Glide.with(binding.songCoverImageView)
                .load(it.coverUrl)
                .apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        } ?: run {
            // Hide player view if no song is playing
            binding.playerView.visibility = View.GONE
        }
    }

    // Display a simple message dialog box
    private fun showMessageBox(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}
