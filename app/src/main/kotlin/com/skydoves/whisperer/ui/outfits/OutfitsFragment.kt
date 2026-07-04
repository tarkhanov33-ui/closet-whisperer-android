package com.skydoves.whisperer.ui.outfits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.Outfit
import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.ui.adapter.OutfitAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class OutfitsFragment : Fragment() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var outfitAdapter: OutfitAdapter
  private lateinit var outfitsCountText: TextView
  private lateinit var generateBtn: Button

  private var currentItems: List<ClothingItem> = emptyList()
  private var generatedLook: Outfit? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_outfits, container, false)

    outfitsCountText = view.findViewById(R.id.outfitsCountText)

    val recyclerView = view.findViewById<RecyclerView>(R.id.outfitsRecyclerView)
    recyclerView.layoutManager = LinearLayoutManager(context)
    outfitAdapter = OutfitAdapter(
      onOutfitClick = { outfit -> showOutfitDetailsDialog(outfit) },
      onSaveClick = { outfit -> likeOutfit(outfit) }
    )
    recyclerView.adapter = outfitAdapter

    generateBtn = view.findViewById(R.id.outfitsRegenerateButton)
    generateBtn.visibility = View.GONE

    loadItems()
    loadApprovedOutfits()
    return view
  }

  private fun loadItems() {
    lifecycleScope.launch {
      closetRepository.fetchItems()
        .catch { err -> Toast.makeText(context, err.message ?: "Could not reach the server", Toast.LENGTH_LONG).show() }
        .collect { items ->
          currentItems = items.filter { it.type.isNotBlank() && !it.type.equals("Outfit", ignoreCase = true) }
        }
    }
  }

  private fun loadApprovedOutfits() {
    outfitsCountText.text = "Loading approved looks…"
    lifecycleScope.launch {
      closetRepository.fetchOutfitHistory()
        .catch { err ->
          outfitsCountText.text = "Could not load history"
          Toast.makeText(context, err.message ?: "Could not load outfits history", Toast.LENGTH_LONG).show()
        }
        .collect { list ->
          outfitAdapter.submitList(list)
          outfitsCountText.text = if (list.isEmpty()) {
            "No approved looks yet. Like a look on the Dashboard!"
          } else {
            "${list.size} approved looks"
          }
        }
    }
  }

  private fun showOutfitDetailsDialog(outfit: Outfit) {
    val context = requireContext()
    val msg = "Items:\n${outfit.items}\n\n${outfit.reason}"
    AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
      .setTitle(outfit.title)
      .setMessage(msg)
      .setPositiveButton("Wear Out") { d, _ -> wearOutfit(outfit); d.dismiss() }
      .setNeutralButton("Reject…") { d, _ -> showRejectDialog(outfit); d.dismiss() }
      .setNegativeButton("Close") { d, _ -> d.dismiss() }
      .show()
  }

  private fun likeOutfit(outfit: Outfit) {
    val id = outfit.id?.objectId ?: return
    lifecycleScope.launch {
      closetRepository.likeOutfit(id)
        .catch { err -> Toast.makeText(context, err.message ?: "Could not save", Toast.LENGTH_LONG).show() }
        .collect { Toast.makeText(context, "Saved to favorites.", Toast.LENGTH_SHORT).show() }
    }
  }


  private fun showRejectDialog(outfit: Outfit) {
    val context = requireContext()
    val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
    val input = EditText(builder.context).apply {
      hint = "Why doesn't this work for you? (optional)"
      minLines = 2
      setTextColor(ContextCompat.getColor(context, R.color.ink_primary))
      setHintTextColor(ContextCompat.getColor(context, R.color.ink_muted))
    }
    builder
      .setTitle("Reject \"${outfit.title}\"")
      .setView(input)
      .setPositiveButton("Send feedback") { d, _ ->
        val comment = input.text.toString().trim()
        val backendId = outfit.id?.objectId ?: ""
        lifecycleScope.launch {
          closetRepository.rejectOutfit(backendId, outfit.title, outfit.items, comment)
            .catch { err -> Toast.makeText(context, err.message ?: "Could not send feedback", Toast.LENGTH_LONG).show() }
            .collect {
              Toast.makeText(context, "Feedback sent.", Toast.LENGTH_SHORT).show()
              loadApprovedOutfits()
            }
        }
        d.dismiss()
      }
      .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
      .show()
  }


  private fun wearOutfit(outfit: Outfit) {
    val context = requireContext()
    val names = outfit.items.split(",").map { it.trim().lowercase() }
    val ids = currentItems.filter { it.alias.lowercase() in names }.mapNotNull { it.id?.objectId }
    if (ids.isEmpty()) {
      Toast.makeText(context, "Could not match these items in your closet", Toast.LENGTH_SHORT).show()
      return
    }
    lifecycleScope.launch {
      closetRepository.wearOutfit(ids)
        .catch { err -> Toast.makeText(context, err.message ?: "Server unavailable", Toast.LENGTH_LONG).show() }
        .collect {
          Toast.makeText(context, "Worn! Those items are now in the laundry.", Toast.LENGTH_SHORT).show()
          loadItems()
        }
    }
  }
}
