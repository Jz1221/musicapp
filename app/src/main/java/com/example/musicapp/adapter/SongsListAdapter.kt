package com.example.musicapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.MyExoplayer
import com.example.musicapp.PlayerActivity
import com.example.musicapp.databinding.SongListItemRecyclerRowBinding
import com.example.musicapp.models.SongModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

// Adapter for displaying a list of songs (favorites) in a RecyclerView
class SongsListAdapter (private val songIdList : List<String>, // List of song document IDs from Firestore
    private val onSongRemoved: () -> Unit) :    // Callback triggered after a song is removed from favorites
    RecyclerView.Adapter<SongsListAdapter.MyViewHolder>() {

    // Inflate item layout and create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = SongListItemRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding, onSongRemoved)
    }

    // Bind data to ViewHolder at given position
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(songIdList[position])
    }

    // Return number of items
    override fun getItemCount(): Int {
        return songIdList.size
    }

    // ViewHolder class to hold and bind views for each song item
    class MyViewHolder(
        private val binding : SongListItemRecyclerRowBinding,
        private val onSongRemoved: () -> Unit   // Callback to refresh UI after removal
    ) : RecyclerView.ViewHolder(binding.root){

        // Bind song data using songId (fetch from Firestore)
        fun bindData(songId : String){

            FirebaseFirestore.getInstance().collection("songs")
                .document(songId).get()
                .addOnSuccessListener {
                    val song = it.toObject(SongModel::class.java)
                    song?.apply {

                        // Set title and subtitle to the UI
                        binding.songTitleTextView.text = title
                        binding.songSubtitleTextView.text = subtitle

                        // Load the cover image using Glide with rounded corners
                        Glide.with(binding.songCoverImageView).load(coverUrl)
                            .apply (
                                RequestOptions().transform(RoundedCorners(32))
                            )
                            .into(binding.songCoverImageView)

                        // Handle short click: play the song and open PlayerActivity
                        binding.root.setOnClickListener {
                            MyExoplayer.startPlaying(binding.root.context, song)
                            it.context.startActivity(Intent(it.context, PlayerActivity::class.java))
                        }

                        // Handle long click: show confirmation to remove song from favorites
                        binding.root.setOnLongClickListener {
                            val context = binding.root.context

                            AlertDialog.Builder(context)
                                .setTitle("Remove Favorite")
                                .setMessage("Are you sure you want to remove this song from your favorites?")
                                .setPositiveButton("Yes") { _, _ ->
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {

                                        // Remove the favorite entry from Realtime Database
                                        FirebaseDatabase.getInstance().getReference("favourites")
                                            .child(songId)
                                            .child(userId)
                                            .removeValue()
                                            .addOnSuccessListener {
                                                onSongRemoved() // Notify the adapter to refresh list
                                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                            true    // consume the long click
                        }

                    }
                }
        }
    }
}