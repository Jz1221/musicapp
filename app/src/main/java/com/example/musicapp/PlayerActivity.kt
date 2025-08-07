package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.musicapp.databinding.ActivityPlayerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer

    // Listener to detect play/pause changes and update UI
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            showGif(isPlaying)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Makes activity edge-to-edge (status/nav bar areas)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle insets for fullscreen display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get current song and player instance
        val song = MyExoplayer.getCurrentSong()
        val player = MyExoplayer.getInstance()

        // If both are available, initialize player view
        if (song != null && player != null) {
            exoPlayer = player
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // Repeat current song

            // Display song title and subtitle
            binding.songTitleTextView.text = song.title
            binding.songSubtitleTextView.text = song.subtitle

            // Fetch song description from Firestore and show it
            val db = FirebaseFirestore.getInstance()
            val songId = song.id

            db.collection("songs").document(songId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val description = document.getString("description")
                        binding.songDescriptionTextView.text = description ?: "No description available."
                    } else {
                        binding.songDescriptionTextView.text = "No description found."
                    }
                }
                .addOnFailureListener {
                    binding.songDescriptionTextView.text = "Failed to load description."
                }


            // Load circular song cover image using Glide
            Glide.with(binding.songCoverImageView)
                .load(song.coverUrl)
                .circleCrop()
                .into(binding.songCoverImageView)

            // Load animated GIF to indicate song is playing
            Glide.with(binding.songGifImageView)
                .load(R.drawable.media_playing)
                .circleCrop()
                .into(binding.songGifImageView)

            // Attach ExoPlayer to Media3 PlayerView
            binding.playerView.player = exoPlayer
            binding.playerView.showController() // Show play/pause buttons

            // Start observing playback state
            exoPlayer.addListener(playerListener)

            // Show or hide animated gif based on playing state
            showGif(exoPlayer.isPlaying)
        } else {
            // Exit if song or player is null
            finish()
        }

        // Back button: navigate to home screen
        binding.backButton.setOnClickListener {
            val intent = Intent(this, HomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Save current song to user's favourites in Firebase Realtime Database
        binding.saveToFavouritesButton.setOnClickListener {
            val song = MyExoplayer.getCurrentSong()

            if (song != null) {
                // Get the currently logged-in user's UID from Firebase Authentication
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener // Exit if user is not logged in

                // Get a reference to the root of the Firebase Realtime Database
                val databaseRef = FirebaseDatabase.getInstance().reference

                // Add this song under "favourites/{songId}/{userId}" = true
                databaseRef.child("favourites").child(song.id).child(userId).setValue(true)

                Toast.makeText(this, "Saved to Favorites!", Toast.LENGTH_SHORT).show() // Show success message
            } else {
                Toast.makeText(this, "Song data not found", Toast.LENGTH_SHORT).show() // Error Message
            }
        }


    }

    // Remove the listener when activity is destroyed to prevent memory leaks
    override fun onDestroy() {
        super.onDestroy()
        if (::exoPlayer.isInitialized) {
            exoPlayer.removeListener(playerListener)
        }
    }

    // Show or hide the animated GIF depending on playback state
    private fun showGif(show: Boolean) {
        binding.songGifImageView.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }
}
