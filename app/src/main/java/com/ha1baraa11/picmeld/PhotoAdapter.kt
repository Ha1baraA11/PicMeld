package com.ha1baraa11.picmeld

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ha1baraa11.picmeld.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onRemoveClick: (Uri) -> Unit,
    private val onOrderChanged: (List<Uri>) -> Unit = {}
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private val uris = mutableListOf<Uri>()

    fun submitList(newList: List<Uri>) {
        val diff = DiffUtil.calculateDiff(UriDiffCallback(uris, newList))
        uris.clear()
        uris.addAll(newList)
        diff.dispatchUpdatesTo(this)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val item = uris.removeAt(fromPosition)
        uris.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun onDragFinished() {
        onOrderChanged(uris.toList())
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

    private class UriDiffCallback(
        private val oldList: List<Uri>,
        private val newList: List<Uri>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = oldList[oldPos] == newList[newPos]
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = oldList[oldPos] == newList[newPos]
    }
}
