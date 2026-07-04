package com.skydoves.whisperer.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.ClothingItem

class ClothingAdapter(
  private var items: List<ClothingItem> = emptyList(),
  private val onItemClick: (ClothingItem) -> Unit = {}
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

  fun submitList(newList: List<ClothingItem>) {
    items = newList
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clothing, parent, false)
    return ClothingViewHolder(view, onItemClick)
  }

  override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size

  class ClothingViewHolder(
    itemView: View,
    private val onItemClick: (ClothingItem) -> Unit
  ) : RecyclerView.ViewHolder(itemView) {
    private val imageView = itemView.findViewById<ImageView>(R.id.clothingItemImage)
    private val nameView = itemView.findViewById<TextView>(R.id.clothingItemName)
    private val detailsView = itemView.findViewById<TextView>(R.id.clothingItemDetails)
    private val statusBadge = itemView.findViewById<TextView>(R.id.clothingItemStatusBadge)

    fun bind(item: ClothingItem) {
      itemView.setOnClickListener { onItemClick(item) }
      nameView.text = item.alias
      detailsView.text = "${item.type} · ${item.color} · ${item.style}"
      
      val base64 = item.imageBase64
      when {
        !base64.isNullOrBlank() -> {
          val pure = base64.substringAfter("base64,", base64)
          val bytes = try {
            android.util.Base64.decode(pure, android.util.Base64.DEFAULT)
          } catch (e: Exception) {
            null
          }
          if (bytes != null) {
            Glide.with(itemView.context)
              .asBitmap()
              .load(bytes)
              .placeholder(android.R.drawable.ic_menu_gallery)
              .into(imageView)
          } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
          }
        }
        item.image.isNotEmpty() -> {
          Glide.with(itemView.context)
            .load(item.image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(imageView)
        }
        else -> imageView.setImageResource(android.R.drawable.ic_menu_gallery)
      }

      statusBadge.text = item.status
      val context = itemView.context
      if (item.status.equals("Clean", ignoreCase = true)) {
        statusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_good))
        statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_good_text))
      } else {
        statusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning))
        statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning_text))
      }
    }
  }
}
