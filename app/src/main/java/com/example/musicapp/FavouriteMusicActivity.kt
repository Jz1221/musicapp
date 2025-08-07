package com.example.musicapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.adapter.SongsListAdapter
import com.example.musicapp.databinding.ActivityFavouriteMusicBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavouriteMusicActivity : AppCompatActivity() {

    // View binding for activity layout
    private lateinit var binding: ActivityFavouriteMusicBinding

    // Adapter for the RecyclerView to show favorite songs
    private lateinit var adapter: SongsListAdapter

    // List to store the IDs of favorite songs
    private val favoriteSongIds = mutableListOf<String>()

    // List to store the IDs of favorite songs
    private val currentUserUid: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate layout using ViewBinding
        binding = ActivityFavouriteMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure layout avoids overlap with system UI (status bar, nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the UI
        setupRecyclerView() // Setup RecyclerView to display favorite songs
        showPlayerView()    // Show current playing song (if any)
        loadFavoriteSongIdsFromRealtimeDatabase()   // Load favorite song IDs from Firebase
        setupBottomNavigation() // Setup bottom nav bar for screen switching
    }

    // Sets up the RecyclerView and assigns the adapter
    private fun setupRecyclerView() {
        adapter = SongsListAdapter(favoriteSongIds) {
            // Callback when a song is removed; refresh list
            loadFavoriteSongIdsFromRealtimeDatabase() // refresh when a song is removed
        }
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.favoritesRecyclerView.adapter = adapter
    }

    // Loads user's favorite song IDs from Firebase Realtime Database
    private fun loadFavoriteSongIdsFromRealtimeDatabase() {
        if (currentUserUid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to 'favourites' node in Firebase Realtime Database
        val dbRef = FirebaseDatabase.getInstance().getReference("favourites")

        // Read data once (single event)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                favoriteSongIds.clear()

                // Iterate over each song node
                for (songSnapshot in snapshot.children) {
                    val songId = songSnapshot.key

                    // Check if current user has marked this song as favorite
                    val isFavoritedByUser =
                        songSnapshot.child(currentUserUid).getValue(Boolean::class.java) == true

                    if (isFavoritedByUser && songId != null) {
                        favoriteSongIds.add(songId)
                    }
                }

                adapter.notifyDataSetChanged() // Update RecyclerView

                // Show message if no favorites found
                if (favoriteSongIds.isEmpty()) {
                    Toast.makeText(
                        this@FavouriteMusicActivity,
                        "No favorites found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {

                // Handle database read error
                Toast.makeText(
                    this@FavouriteMusicActivity,
                    "Failed to load favorites",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Display "Now Playing" mini player if a song is currently playing
    private fun showPlayerView() {

        // If player view is clicked, open PlayerActivity
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        // Get the currently playing song (if any)
        MyExoplayer.getCurrentSong()?.let {

            // Show player and update UI with song title and cover
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing: ${it.title}"
            Glide.with(binding.songCoverImageView)
                .load(it.coverUrl)
                .apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        } ?: run {

            // Hide player view if no song is playing
            binding.playerView.visibility = View.GONE
        }
    }

    // Handle bottom navigation menu item clicks
    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNavigationView
        bottomNav.selectedItemId = R.id.favourites

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeScreen::class.java))
                    true
                }
                R.id.playlist -> {
                    startActivity(Intent(this, UserUploadMusic::class.java))
                    true
                }
                R.id.favourites -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
