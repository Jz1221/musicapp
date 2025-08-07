package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.adapter.SongsListAdapter
import com.example.musicapp.databinding.ActivitySongsListBinding
import com.example.musicapp.models.CategoryModel

class SongsListActivity : AppCompatActivity() {

    companion object{
        // Static variable to hold the selected category for this screen
        lateinit var category : CategoryModel
    }

    // ViewBinding object for this activity's layout
    lateinit var binding: ActivitySongsListBinding

    // Adapter for the RecyclerView
    lateinit var songsListAdapter: SongsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate and set the content view using ViewBinding
        binding = ActivitySongsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply padding to avoid overlapping with system UI (e.g., status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set the category name as the title
        binding.nameTextView.text = category.name

        // Load the category image into the ImageView with rounded corners using Glide
        Glide.with(binding.coverImageView).load(category.coverUrl)
            .apply (
                RequestOptions().transform(RoundedCorners(32))
            )
            .into(binding.coverImageView)

        // Set up the RecyclerView with the list of songs
        setupSongsListRecycleView()

        // Handle back button click to return to HomeScreen
        binding.backButton.setOnClickListener {
            val intent = Intent(this, HomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    // Function to configure the RecyclerView that displays songs
    fun setupSongsListRecycleView() {
        // Initialize adapter and provide the song list from selected category
        songsListAdapter = SongsListAdapter(category.songs) {

        }
        // Set layout manager and assign adapter to the RecyclerView
        binding.songsListRecycleView.layoutManager = LinearLayoutManager(this)
        binding.songsListRecycleView.adapter = songsListAdapter
    }
}