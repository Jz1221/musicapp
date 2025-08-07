package com.example.musicapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.databinding.ItemMusicBinding
import com.example.musicapp.models.MusicModel

// Adapter for displaying and playing local/remote MP3 music items in a RecyclerView
class MusicAdapter(
    private var musicList: MutableList<MusicModel>, // List of music items
    private val onLongClick: (MusicModel) -> Unit   // Callback for long-press event (e.g., delete or options)
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    // ViewHolder class that holds reference to views in each row
    inner class MusicViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    private var mediaPlayer: MediaPlayer? = null    // MediaPlayer instance for playback

    // Inflate the row layout and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    // Return the total number of items
    override fun getItemCount(): Int = musicList.size

    // Bind data to views at a given position
    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
        with(holder.binding) {

            // Set song title and description
            tvTitle.text = music.title
            tvDescription.text = music.description

            // Display filename from audio URL if available
            music.audioUrl?.let { url ->
                val fileName = Uri.parse(url).lastPathSegment ?: "Unknown"
                tvFileName.text = "File $fileName"
            }

            // Handle play button click
            btnPlay.setOnClickListener {
                Toast.makeText(holder.itemView.context, "URL: ${music.audioUrl}", Toast.LENGTH_SHORT).show()
                playMusic(holder.itemView.context, music.audioUrl)
            }

            // Handle pause button click
            btnPause.setOnClickListener {
                pauseMusic(holder.itemView.context)
            }

            // Hide download button if not used
            btnDownload.visibility = View.GONE

            // Handle long press (e.g., for delete or more options)
            root.setOnLongClickListener {
                onLongClick(music)
                true
            }
        }
    }

    // Play music from a given URL
    private fun playMusic(context: Context, url: String?) {
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "No audio URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mediaPlayer?.release()  // Release any existing MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)   // Set data source
                prepare()           // Prepare the MediaPlayer
                start()             // Start playback
            }
            Toast.makeText(context, "Playing music...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Pause the currently playing music
    private fun pauseMusic(context: Context){
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            Toast.makeText(context, "Music Paused", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No music is playing", Toast.LENGTH_SHORT).show()
        }
     }

    // Update the list of music items and refresh the RecyclerView
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: MutableList<MusicModel>) {
        musicList = newList
        notifyDataSetChanged()
    }
}
