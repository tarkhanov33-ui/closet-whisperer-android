package com.skydoves.whisperer.ui.add

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddItemFragment : Fragment() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var nameEditText: EditText
  private lateinit var colorEditText: EditText
  private lateinit var styleSpinner: Spinner
  private lateinit var categorySpinner: Spinner
  private lateinit var insulationSpinner: Spinner

  private lateinit var uploadPromptContainer: View
  private lateinit var previewImageView: ImageView

  private val categories = listOf("Top", "Bottom", "Outerwear", "Shoes", "Full Body")
  private val insulations = listOf("Light", "Medium", "Warm")
  private val styles = listOf("Casual", "Smart Casual", "Elegant", "Sporty", "Boho-chic")

  private var selectedImageBase64: String? = null
  private var selectedImageUri: Uri? = null

  private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      selectedImageUri = it
      previewImageView.setImageURI(it)
      previewImageView.visibility = View.VISIBLE
      uploadPromptContainer.visibility = View.GONE

      // Convert to Base64 and run auto-tagging
      val base64 = getBase64FromUri(it)
      if (base64 != null) {
        selectedImageBase64 = base64
        runAutoTagging(base64)
      } else {
        Toast.makeText(context, "Could not read chosen image", Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_add_item, container, false)

    nameEditText = view.findViewById(R.id.addItemNameEditText)
    colorEditText = view.findViewById(R.id.addItemColorEditText)
    styleSpinner = view.findViewById(R.id.addItemStyleSpinner)
    categorySpinner = view.findViewById(R.id.addItemCategorySpinner)
    insulationSpinner = view.findViewById(R.id.addItemInsulationSpinner)

    uploadPromptContainer = view.findViewById(R.id.uploadPromptContainer)
    previewImageView = view.findViewById(R.id.clothingItemPreviewImage)

    val backButton = view.findViewById<View>(R.id.addItemBackButton)
    backButton.setOnClickListener {
      if (parentFragmentManager.backStackEntryCount > 0) {
        parentFragmentManager.popBackStack()
      } else {
        (activity as? MainActivity)?.selectTab(R.id.navigation_closet)
      }
    }

    val categoryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories).apply {
      setDropDownViewResource(R.layout.spinner_item)
    }
    categorySpinner.adapter = categoryAdapter

    val insulationAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, insulations).apply {
      setDropDownViewResource(R.layout.spinner_item)
    }
    insulationSpinner.adapter = insulationAdapter

    val styleAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, styles).apply {
      setDropDownViewResource(R.layout.spinner_item)
    }
    styleSpinner.adapter = styleAdapter

    val saveButton = view.findViewById<Button>(R.id.addItemSaveButton)
    saveButton.setOnClickListener {
      saveClothingItem()
    }

    val resetButton = view.findViewById<Button>(R.id.addItemResetButton)
    resetButton.setOnClickListener {
      resetForm()
    }

    val uploadZone = view.findViewById<View>(R.id.uploadImageZone)
    uploadZone.setOnClickListener {
      // Launch standard image picker to get a real chosen photo
      pickImageLauncher.launch("image/*")
    }

    return view
  }

  private fun getBase64FromUri(uri: Uri): String? {
    return try {
      val inputStream = requireContext().contentResolver.openInputStream(uri)
      val bytes = inputStream?.readBytes()
      inputStream?.close()
      bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
    } catch (e: Exception) {
      null
    }
  }

  private fun runAutoTagging(base64: String) {
    Toast.makeText(context, "Analyzing image & auto-detecting tags...", Toast.LENGTH_SHORT).show()
    lifecycleScope.launch {
      closetRepository.analyzeImage(base64)
        .catch {
          Toast.makeText(context, "Auto-detection unavailable. Please enter details manually.", Toast.LENGTH_SHORT).show()
        }
        .collect { tags ->
          if (tags.isNotEmpty()) {
            val category = tags["category"]?.toString()
            val subCategory = tags["subCategory"]?.toString()
            val color = tags["color"]?.toString()
            val styleTag = tags["styleTag"]?.toString()
            val thermal = tags["thermalInsulation"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1

            if (!subCategory.isNullOrBlank()) nameEditText.setText(subCategory)
            if (!color.isNullOrBlank()) colorEditText.setText(color)
            if (!styleTag.isNullOrBlank()) {
              val idx = styles.indexOfFirst {
                it.equals(styleTag, true) || it.contains(styleTag, true) || styleTag.contains(it, true)
              }
              if (idx >= 0) styleSpinner.setSelection(idx)
            }

            val matchedCat = when (category?.uppercase()) {
              "TOP" -> "Top"
              "BOTTOM" -> "Bottom"
              "OUTERWEAR" -> "Outerwear"
              "SHOES" -> "Shoes"
              "FULL_BODY" -> "Full Body"
              else -> "Top"
            }
            categorySpinner.setSelection(categories.indexOf(matchedCat))

            val matchedInsulation = when (thermal) {
              2 -> "Medium"
              3 -> "Warm"
              else -> "Light"
            }
            insulationSpinner.setSelection(insulations.indexOf(matchedInsulation))

            Toast.makeText(context, "Clothing tags auto-detected successfully!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Auto-detection completed. Please double-check details.", Toast.LENGTH_SHORT).show()
          }
        }
    }
  }

  private fun saveClothingItem() {
    val name = nameEditText.text.toString().trim()
    val color = colorEditText.text.toString().trim()
    val style = styleSpinner.selectedItem.toString()
    val category = categorySpinner.selectedItem.toString()
    val insulation = insulationSpinner.selectedItem.toString()

    if (name.isEmpty() || color.isEmpty() || style.isEmpty()) {
      Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
      return
    }

    val thermalInt = when (insulation) {
      "Medium" -> 2
      "Warm"   -> 3
      else     -> 1
    }


    val item = ClothingItem(
      category = category,
      subCategory = name,
      status = "Clean",
      color = color,
      styleTag = style,
      thermalInsulation = thermalInt,
      imageUrl = ""
    )

    lifecycleScope.launch {
      closetRepository.createItem(item, selectedImageBase64)
        .catch { err ->
          Toast.makeText(context, "Failed to save item: ${err.message}", Toast.LENGTH_SHORT).show()
        }
        .collect {
          Toast.makeText(context, "Item added to closet successfully!", Toast.LENGTH_SHORT).show()
          resetForm()
          if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
          } else {
            (activity as? MainActivity)?.selectTab(R.id.navigation_closet)
          }
        }
    }
  }

  private fun resetForm() {
    nameEditText.text.clear()
    colorEditText.text.clear()
    styleSpinner.setSelection(0)
    categorySpinner.setSelection(0)
    insulationSpinner.setSelection(0)
    
    selectedImageBase64 = null
    selectedImageUri = null
    previewImageView.setImageURI(null)
    previewImageView.visibility = View.GONE
    uploadPromptContainer.visibility = View.VISIBLE
  }
}
