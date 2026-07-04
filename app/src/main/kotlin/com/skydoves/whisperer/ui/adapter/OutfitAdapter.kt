package com.skydoves.whisperer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.Outfit

class OutfitAdapter(
  private var outfits: List<Outfit> = emptyList(),
  private val onOutfitClick: (Outfit) -> Unit = {},
  private val onSaveClick: (Outfit) -> Unit = {}
) : RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

  fun submitList(newList: List<Outfit>) {
    outfits = newList
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
    return OutfitViewHolder(view)
  }

  override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
    val outfit = outfits[position]
    holder.bind(outfit)
    holder.itemView.setOnClickListener { onOutfitClick(outfit) }
    holder.heartIcon.setOnClickListener {
      holder.heartIcon.setImageResource(android.R.drawable.btn_star_big_on)
      onSaveClick(outfit)
    }
  }

  override fun getItemCount(): Int = outfits.size

  class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleView = itemView.findViewById<TextView>(R.id.outfitItemTitle)
    private val dateView = itemView.findViewById<TextView>(R.id.outfitItemDate)
    private val detailsView = itemView.findViewById<TextView>(R.id.outfitItemDetails)
    private val scoreView = itemView.findViewById<TextView>(R.id.outfitItemScore)
    private val reasonView = itemView.findViewById<TextView>(R.id.outfitItemReason)
    val heartIcon: ImageView = itemView.findViewById(R.id.outfitSaveHeart)

    fun bind(outfit: Outfit) {
      titleView.text = outfit.title
      dateView.text = outfit.date
      detailsView.text = outfit.items
      scoreView.text = "★ ${outfit.score}"
      reasonView.text = outfit.reason
      heartIcon.setImageResource(android.R.drawable.btn_star_big_off)
    }
  }
}
