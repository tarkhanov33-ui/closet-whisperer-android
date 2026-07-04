package com.skydoves.whisperer.ui.closet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.ui.adapter.ClothingAdapter
import com.skydoves.whisperer.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClosetFragment : Fragment() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var clothingAdapter: ClothingAdapter
  private lateinit var itemsCountText: TextView
  private lateinit var categoryContainer: LinearLayout
  private lateinit var searchInput: EditText

  private var allItems: List<ClothingItem> = emptyList()
  private var activeCategory = "All"
  private var activeQuery = ""

  private val categories = listOf("All", "Top", "Bottom", "Outerwear", "Shoes", "Full Body")

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_closet, container, false)

    itemsCountText = view.findViewById(R.id.closetItemsCountText)
    categoryContainer = view.findViewById(R.id.closetCategoryChipsContainer)
    searchInput = view.findViewById(R.id.closetSearchInput)

    val addButton = view.findViewById<Button>(R.id.closetAddButton)
    addButton.setOnClickListener {
      (activity as? MainActivity)?.selectTab(R.id.navigation_add)
    }

    val recyclerView = view.findViewById<RecyclerView>(R.id.closetRecyclerView)
    recyclerView.layoutManager = GridLayoutManager(context, 2)
    clothingAdapter = ClothingAdapter(onItemClick = { item ->
      showItemDetailsDialog(item)
    })
    recyclerView.adapter = clothingAdapter

    
    searchInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        activeQuery = s?.toString()?.trim() ?: ""
        filterItems()
      }
      override fun afterTextChanged(s: Editable?) {}
    })

    setupCategoryChips()
    loadClosetItems()

    return view
  }

  private fun setupCategoryChips() {
    categoryContainer.removeAllViews()
    val context = requireContext()
    for (category in categories) {
      val button = Button(context).apply {
        text = category
        isAllCaps = false
        textSize = 12f
        setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
        
        val params = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          marginEnd = dpToPx(8)
        }
        layoutParams = params

        
        updateChipStyle(this, category == activeCategory)

        setOnClickListener {
          activeCategory = category
          setupCategoryChips() 
          filterItems()
        }
      }
      categoryContainer.addView(button)
    }
  }

  private fun updateChipStyle(button: Button, isSelected: Boolean) {
    if (isSelected) {
      button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.brand_primary)
      button.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_inverse))
    } else {
      button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
      button.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_secondary))
    }
  }

  private fun loadClosetItems() {
    lifecycleScope.launch {
      closetRepository.fetchItems()
        .catch { err ->
          Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
        }
        .collect { items ->
          allItems = items
          filterItems()
        }
    }
  }

  private fun filterItems() {
    val filtered = allItems.filter { item ->
      
      if (item.type.isBlank() || item.type.equals("Outfit", ignoreCase = true)) return@filter false
      val matchesCategory = activeCategory == "All" || item.type.equals(activeCategory, ignoreCase = true)
      val matchesSearch = activeQuery.isEmpty() ||
          item.alias.contains(activeQuery, ignoreCase = true) ||
          item.color.contains(activeQuery, ignoreCase = true) ||
          item.style.contains(activeQuery, ignoreCase = true)

      matchesCategory && matchesSearch
    }
    clothingAdapter.submitList(filtered)
    itemsCountText.text = "${filtered.size} pieces"
  }

  private fun showItemDetailsDialog(item: ClothingItem) {
    val context = requireContext()
    val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
    builder.setTitle(item.alias)
    
    val detailsMsg = "Category: ${item.type}\nColor: ${item.color}\nStyle: ${item.style}\nStatus: ${item.status}"
    builder.setMessage(detailsMsg)
    
    
    val toggleText = if (item.status.equals("Clean", ignoreCase = true)) "Mark as Dirty" else "Mark as Clean"
    builder.setNeutralButton(toggleText) { dialog, _ ->
      val newStatus = if (item.status.equals("Clean", ignoreCase = true)) "Dirty" else "Clean"
      val updated = item.copy(status = newStatus)
      lifecycleScope.launch {
        closetRepository.updateItem(updated)
          .catch { err -> Toast.makeText(context, err.message ?: "Server unavailable", Toast.LENGTH_LONG).show() }
          .collect { success ->
            if (success) {
              Toast.makeText(context, "Status changed to $newStatus", Toast.LENGTH_SHORT).show()
              loadClosetItems()
            }
          }
      }
      dialog.dismiss()
    }
    
    
    builder.setPositiveButton("Delete") { dialog, _ ->
      AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
        .setTitle("Delete item?")
        .setMessage("Are you sure you want to delete ${item.alias}?")
        .setPositiveButton("Yes") { confirmDialog, _ ->
          lifecycleScope.launch {
            val systemId = item.id?.systemID ?: "ambient_invisible_intelligence"
            val objectId = item.id?.objectId ?: ""
            closetRepository.deleteItem(systemId, objectId)
              .catch { err -> Toast.makeText(context, err.message ?: "Server unavailable", Toast.LENGTH_LONG).show() }
              .collect { success ->
                if (success) {
                  Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                  loadClosetItems()
                }
              }
          }
          confirmDialog.dismiss()
        }
        .setNegativeButton("No") { confirmDialog, _ -> confirmDialog.dismiss() }
        .show()
      dialog.dismiss()
    }
    
    builder.setNegativeButton("Close") { dialog, _ ->
      dialog.dismiss()
    }
    
    builder.show()
  }

  private fun dpToPx(dp: Int): Int {
    val density = resources.displayMetrics.density
    return (dp * density).toInt()
  }

  private fun Float.sp(): Float = this

  private fun Int.sp(): Float = this.toFloat()
}
