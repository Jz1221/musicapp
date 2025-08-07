package com.example.musicapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.MyExoplayer
import com.example.musicapp.PlayerActivity
import com.example.musicapp.databinding.SectionSongListRecycleRowBinding
import com.example.musicapp.models.SongModel
import com.google.firebase.firestore.FirebaseFirestore

// Adapter to display a list of songs using their Firestore IDs in a RecyclerView
class SectionSongListAdapter (private val songIdList : List<String>) :
    RecyclerView.Adapter<SectionSongListAdapter.MyViewHolder>() {

    // Inflates the item layout and returns a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = SectionSongListRecycleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    // Binds the data (songId) to the ViewHolder
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(songIdList[position])
    }

    // Returns the total number of items
    override fun getItemCount(): Int {
        return songIdList.size
    }

    // ViewHolder class to bind each song item
    class MyViewHolder(private val binding : SectionSongListRecycleRowBinding) : RecyclerView.ViewHolder(binding.root){
        //bind data with view
        fun bindData(songId : String){

            // Fetches and binds song data based on songId
            FirebaseFirestore.getInstance().collection("songs")
                .document(songId).get()
                .addOnSuccessListener {

                    // Convert the Firestore document to a SongModel object
                    val song = it.toObject(SongModel::class.java)

                    // If song is successfully retrieved, bind its data to the UI
                    song?.apply {

                        // Set the song title and subtitle to TextViews
                        binding.songTitleTextView.text = title
                        binding.songSubtitleTextView.text = subtitle

                        // Load the song cover image with rounded corners using Glide
                        Glide.with(binding.songCoverImageView).load(coverUrl)
                            .apply (
                                RequestOptions().transform(RoundedCorners(32))
                            )
                            .into(binding.songCoverImageView)

                        // Set click listener to play the song and open PlayerActivity
                        binding.root.setOnClickListener {
                            MyExoplayer.startPlaying(binding.root.context, song)
                            it.context.startActivity(Intent(it.context, PlayerActivity::class.java))
                        }

                    }
                }
        }
    }
}