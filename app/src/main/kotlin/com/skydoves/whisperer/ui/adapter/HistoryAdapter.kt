package com.skydoves.whisperer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.HistoryItem

class HistoryAdapter(
  private var items: List<HistoryItem> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

  fun submitList(newList: List<HistoryItem>) {
    items = newList
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
    return HistoryViewHolder(view)
  }

  override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size

  class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val nameView = itemView.findViewById<TextView>(R.id.historyOutfitName)
    private val detailsView = itemView.findViewById<TextView>(R.id.historyOutfitDetails)
    private val scoreView = itemView.findViewById<TextView>(R.id.historyOutfitScore)

    fun bind(item: HistoryItem) {
      nameView.text = item.outfit
      detailsView.text = "${item.day} · ${item.weather}"
      scoreView.text = item.score
      scoreView.visibility = if (item.score.isBlank()) View.GONE else View.VISIBLE
    }
  }
}
