package com.example.musicapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.adapter.MusicAdapter
import com.example.musicapp.databinding.ActivityUserUploadMusicBinding
import com.example.musicapp.databinding.DialogUploadMusicBinding
import com.example.musicapp.models.MusicModel
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UserUploadMusic : AppCompatActivity() {

    private lateinit var binding: ActivityUserUploadMusicBinding
    private lateinit var adapter: MusicAdapter
    private lateinit var musicList: MutableList<MusicModel>
    private lateinit var database: DatabaseReference
    private var audioUri: Uri? = null   // To hold selected MP3 file URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserUploadMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView and fetch music from Firebase
        setupRecyclerView()
        fetchMusicList()

        // Add button to upload new music
        binding.add.setOnClickListener {
            showUploadDialog()
        }

        // Bottom navigation listener
        binding.bottomNavigationView.selectedItemId = R.id.playlist
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeScreen::class.java))
                    true
                }
                R.id.playlist -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.favourites -> {
                    val intent = Intent(this, FavouriteMusicActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // Setup the RecyclerView with adapter and layout
    private fun setupRecyclerView() {
        musicList = mutableListOf()
        adapter = MusicAdapter(musicList) { music -> showOptionsDialog(music) }
        binding.musicRecycleView.layoutManager = LinearLayoutManager(this)
        binding.musicRecycleView.adapter = adapter
    }

    // Fetch list of uploaded music from Firebase Realtime Database
    private fun fetchMusicList() {
        database = FirebaseDatabase.getInstance().getReference("Music")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Save current scroll position
                val layoutManager = binding.musicRecycleView.layoutManager as LinearLayoutManager
                val scrollPosition = layoutManager.findFirstVisibleItemPosition()

                musicList.clear()
                for (data in snapshot.children) {
                    val model = data.getValue(MusicModel::class.java)
                    model?.let { musicList.add(it) }
                }

                adapter.updateList(musicList)

                // Restore previous scroll position
                layoutManager.scrollToPosition(scrollPosition)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserUploadMusic, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Show dialog to upload or update a music item
    private fun showUploadDialog(existingMusic: MusicModel? = null) {
        val dialogBinding = DialogUploadMusicBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Pre-fill fields if editing existing music
        dialogBinding.etTitle.setText(existingMusic?.title ?: "")
        dialogBinding.etDescription.setText(existingMusic?.description ?: "")
        dialogBinding.etAudioUrl.setText("")

        // MP3 file picker
        dialogBinding.btnSelectMp3.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        // Handle upload or update action
        dialogBinding.btnUpload.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim()
            val desc = dialogBinding.etDescription.text.toString().trim()
            val audioUrl = dialogBinding.etAudioUrl.text.toString().trim()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Please fill in title and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (audioUrl.isEmpty() && audioUri == null && existingMusic == null) {
                Toast.makeText(this, "Please select an MP3 or enter an audio URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If audio URL is provided manually
            when {
                audioUrl.isNotEmpty() -> {
                    val id = existingMusic?.id ?: database.push().key!!
                    val music = MusicModel(id, title, desc, audioUrl)
                    database.child(id).setValue(music)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Uploaded via URL", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            audioUri = null
                        }
                }

                // If user picked an MP3 from device
                audioUri != null -> {
                    uploadAudioToFirebase(title, desc, existingMusic?.id, dialog)
                }

                // If only title/desc updated
                existingMusic != null -> {
                    val update = mapOf("title" to title, "description" to desc)
                    database.child(existingMusic.id!!).updateChildren(update)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            audioUri = null
                        }
                }

                else -> {
                    Toast.makeText(this, "No upload method selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // Upload MP3 file to Firebase Storage and save metadata to Realtime Database
    private fun uploadAudioToFirebase(
        title: String,
        description: String,
        musicId: String? = null,
        dialog: AlertDialog
    ) {
        val progress = ProgressDialog(this)
        progress.setMessage("Uploading...")
        progress.setCancelable(false)
        progress.show()

        val fileName = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("Music").child("$fileName.mp3")

        audioUri?.let { uri ->
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    val id = musicId ?: database.push().key!!
                    val music = MusicModel(id, title, description, url.toString())
                    database.child(id).setValue(music)
                        .addOnSuccessListener {
                            progress.dismiss()
                            Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            audioUri = null
                        }
                }
            }.addOnFailureListener {
                progress.dismiss()
                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle MP3 selection from device
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            audioUri = uri
        }
    }

    // Show update/delete options for a selected music item
    private fun showOptionsDialog(music: MusicModel) {
        val options = arrayOf("Update", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showUploadDialog(music)
                    1 -> deleteMusic(music)
                }
            }
            .show()
    }

    // Delete a music entry from Firebase
    private fun deleteMusic(music: MusicModel) {
        val audioUrl = music.audioUrl ?: return
        val isFirebaseStorage = audioUrl.contains("firebasestorage.googleapis.com")

        if (isFirebaseStorage) {
            // If uploaded to Firebase, delete from storage first
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(audioUrl)
            storageRef.delete()
                .addOnSuccessListener {
                    // Then delete from Realtime Database
                    deleteFromDatabase(music.id!!)
                }
                .addOnFailureListener { storageError ->
                    Toast.makeText(this, "Failed to delete from storage: ${storageError.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Just delete from Realtime Database for external URLs
            deleteFromDatabase(music.id!!)
        }
    }

    // Remove music data from Firebase Realtime Database
    private fun deleteFromDatabase(musicId: String) {
        database.child(musicId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Music deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { dbError ->
                Toast.makeText(this, "Failed to delete from database: ${dbError.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Resume player view when activity is in foreground
    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    // Display mini-player UI with current song info
    private fun showPlayerView() {
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        MyExoplayer.getCurrentSong()?.let {
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing: ${it.title}"
            Glide.with(binding.songCoverImageView)
                .load(it.coverUrl)
                .apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        } ?: run {
            binding.playerView.visibility = View.GONE
        }
    }
}
