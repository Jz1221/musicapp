package com.example.musicapp

// Import statements
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.adapter.CategoryAdapter
import com.example.musicapp.adapter.SectionSongListAdapter
import com.example.musicapp.databinding.ActivityHomeScreenBinding
import com.example.musicapp.models.CategoryModel
import com.example.musicapp.models.SongModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects

class HomeScreen : AppCompatActivity() {

    // View binding for the activity layout
    private lateinit var binding: ActivityHomeScreenBinding

    // Adapter to show categories
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load categories and different sections
        getCategories()

        // Load 3 music sections from Firestore
        setupSections("section_1", binding.section1MainLayout, binding.section1Title, binding.section1RecycleView)
        setupSections("section_2", binding.section2MainLayout, binding.section2Title, binding.section2RecycleView)
        setupSections("section_3", binding.section3MainLayout, binding.section3Title, binding.section3RecycleView)

        // Load top 5 mostly played songs
        setupMostlyPlayed("mostly_played", binding.mostlyPlayedMainLayout, binding.mostlyPlayedTitle, binding.mostlyPlayedRecycleView)

        // Adjust padding for system bars (status/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle bottom navigation bar selection
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Update bottom mini-player view if a song is playing
        showPlayerView()
    }

    // Show the mini player at the bottom if a song is currently playing
    fun showPlayerView() {
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        // Check if a song is currently playing in MyExoplayer
        MyExoplayer.getCurrentSong()?.let {
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing: ${it.title}"

            // Load the song cover image with rounded corners
            Glide.with(binding.songCoverImageView)
                .load(it.coverUrl)
                .apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        } ?: run {
            // Hide the mini player if no song is playing
            binding.playerView.visibility = View.GONE
        }
    }

    // Load categories from Firestore and display in horizontal RecyclerView
    private fun getCategories() {
        FirebaseFirestore.getInstance().collection("category")
            .get().addOnSuccessListener {
                val categoryList = it.toObjects(CategoryModel::class.java)
                setupCategoryRecyclerView(categoryList)
            }
    }

    // Initialize category RecyclerView
    private fun setupCategoryRecyclerView(categoryList: List<CategoryModel>) {
        categoryAdapter = CategoryAdapter(categoryList)
        binding.categoriesRecycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecycleView.adapter = categoryAdapter
    }

    // Load specific section (by ID), show its name and songs
    fun setupSections(id: String, mainLayout: RelativeLayout, titleView: TextView, recyclerView: RecyclerView) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                val section = it.toObject(CategoryModel::class.java)
                section?.apply {
                    mainLayout.visibility = View.VISIBLE
                    titleView.text = name
                    recyclerView.layoutManager = LinearLayoutManager(this@HomeScreen, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView.adapter = SectionSongListAdapter(songs)

                    // Navigate to list activity with section data on click
                    mainLayout.setOnClickListener {
                        SongsListActivity.category = section
                        startActivity(Intent(this@HomeScreen, SongsListActivity::class.java))
                    }
                }
            }
    }

    // Display top 5 most played songs
    fun setupMostlyPlayed(id: String, mainLayout: RelativeLayout, titleView: TextView, recyclerView: RecyclerView) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                // Fetch top 5 songs sorted by play count (descending)
                FirebaseFirestore.getInstance().collection("songs")
                    .orderBy("count", Query.Direction.DESCENDING)
                    .limit(5)
                    .get().addOnSuccessListener { songListSnapshot ->
                        val songModelList = songListSnapshot.toObjects<SongModel>()
                        val songsIdList = songModelList.map { it.id } // Get song IDs

                        // Use the section name for display title
                        val section = it.toObject(CategoryModel::class.java)
                        section?.apply {
                            this.songs = songsIdList // Attach songs to the section
                            mainLayout.visibility = View.VISIBLE
                            titleView.text = name
                            recyclerView.layoutManager = LinearLayoutManager(this@HomeScreen, LinearLayoutManager.HORIZONTAL, false)
                            recyclerView.adapter = SectionSongListAdapter(songs)

                            // Open full song list activity on section click
                            mainLayout.setOnClickListener {
                                SongsListActivity.category = section
                                startActivity(Intent(this@HomeScreen, SongsListActivity::class.java))
                            }
                        }
                    }
            }
    }
}
