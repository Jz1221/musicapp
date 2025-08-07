package com.example.musicapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicapp.SongsListActivity
import com.example.musicapp.databinding.CategoryItemRecycleRowBinding
import com.example.musicapp.models.CategoryModel

// Adapter class to display a list of music categories in a RecyclerView
class CategoryAdapter (private val categoryList : List<CategoryModel>) :
    RecyclerView.Adapter<CategoryAdapter.MyViewHolder>() {

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // Inflate the item layout using ViewBinding
        val binding = CategoryItemRecycleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    // Called to bind data to a ViewHolder at a given position
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(categoryList[position])
    }

    // Called to bind data to a ViewHolder at a given position
    override fun getItemCount(): Int {
        return categoryList.size
    }

    // ViewHolder class that holds and binds views for each category item
    class MyViewHolder(private val binding  : CategoryItemRecycleRowBinding) :
        RecyclerView.ViewHolder(binding.root){
            // Binds a CategoryModel to the UI components
            fun bindData(category : CategoryModel){

                // Set the category name to the TextView
                binding.nameTextView.text = category.name

                // Load the cover image using Glide with rounded corners
                Glide.with(binding.coverImageView).load(category.coverUrl)
                    .apply (
                        RequestOptions().transform(RoundedCorners(32))  // Rounded corners with radius 32
                    )
                    .into(binding.coverImageView)

                // Set click listener to navigate to SongsListActivity
                val context = binding.root.context
                binding.root.setOnClickListener {

                    // Assign the selected category to a static variable in SongsListActivity
                    SongsListActivity.category = category

                    // Start the SongsListActivity to show songs under the selected category
                    context.startActivity(Intent(context, SongsListActivity::class.java))
                }

            }
        }
}