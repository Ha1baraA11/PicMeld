package com.zetazero.picmeld

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zetazero.picmeld.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var uris = listOf<Uri>()

    fun submitList(newList: List<Uri>) {
        uris = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = uris[position]
        holder.binding.ivThumbnail.setImageURI(uri)
        holder.binding.btnRemove.setOnClickListener {
            onRemoveClick(uri)
        }
    }

    override fun getItemCount(): Int = uris.size

    class PhotoViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)
}
