package com.example.musicapp

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.models.SongModel
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

// Singleton object to manage ExoPlayer instance across the app
object MyExoplayer {

    // Single ExoPlayer instance for reuse
    private var exoPlayer : ExoPlayer? = null

    // The currently playing song
    private var currentSong : SongModel? = null

    // Get the currently playing song
    fun getCurrentSong() : SongModel?{
        return currentSong
    }

    // Get the ExoPlayer instance
    fun getInstance() : ExoPlayer?{
        return exoPlayer
    }

    // Start playing a song using ExoPlayer
    fun startPlaying(context : Context, song : SongModel){

        // Initialize player if it's null
        if(exoPlayer==null)
            exoPlayer = ExoPlayer.Builder(context).build()

        // Only play if the song is different from the currently playing one
        if(currentSong!=song) {
            //Its a new song so start playing
            currentSong = song
            updateCount()   // Increment play count in Firestore

            // Get the song URL and set it to the player
            currentSong?.url?.apply {
                val mediaItem = MediaItem.fromUri(this)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }

        }

    }

    // Update play count in Firestore for analytics or popularity
    fun updateCount(){
        currentSong?.id?.let{id->
            FirebaseFirestore.getInstance().collection("songs")
                .document(id)
                .get().addOnSuccessListener {
                    // Retrieve existing count or start at 1
                    var latestCount = it.getLong("count")
                    if (latestCount==null){
                        latestCount = 1L
                    }else{
                        latestCount = latestCount+1
                    }

                    // Update the new count in Firestore
                    FirebaseFirestore.getInstance().collection("songs")
                        .document(id)
                        .update(mapOf("count" to latestCount))
                }
        }
    }

    // Release ExoPlayer resources when not needed
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        currentSong = null
    }
}