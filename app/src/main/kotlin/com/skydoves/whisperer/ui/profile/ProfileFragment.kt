package com.skydoves.whisperer.ui.profile

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.repository.ClosetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var personaImageView: ImageView

  private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      personaImageView.setImageURI(it)
      Toast.makeText(context, "Persona photo updated successfully!", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_profile, container, false)

    val subtitleText = view.findViewById<TextView>(R.id.profileSubtitleText)
    val tabLayout = view.findViewById<TabLayout>(R.id.profileTabLayout)
    
    val personalForm = view.findViewById<View>(R.id.personalInfoForm)
    val styleForm = view.findViewById<View>(R.id.styleBodyForm)

    val nameEditText = view.findViewById<EditText>(R.id.profileFullName)
    val emailEditText = view.findViewById<EditText>(R.id.profileEmail)
    val usernameEditText = view.findViewById<EditText>(R.id.profileUsername)
    val dobEditText = view.findViewById<EditText>(R.id.profileDob)
    
    personaImageView = view.findViewById(R.id.profilePersonaImage)

    val userName = activity?.intent?.getStringExtra("user_name").orEmpty()
    val userEmail = activity?.intent?.getStringExtra("user_email").orEmpty()
    val userDob = activity?.intent?.getStringExtra("user_dob").orEmpty()

    nameEditText.setText(userName)
    emailEditText.setText(userEmail)
    usernameEditText.setText(userName.substringBefore(" ").lowercase())
    if (userDob.isNotEmpty()) dobEditText.setText(userDob)

    // Style & Body Inputs
    val heightEditText = view.findViewById<EditText>(R.id.profileHeight)
    val weightEditText = view.findViewById<EditText>(R.id.profileWeight)
    val thermalSpinner = view.findViewById<Spinner>(R.id.profileThermal)
    val styleSpinner = view.findViewById<Spinner>(R.id.profileStyle)
    val skinSpinner = view.findViewById<Spinner>(R.id.profileSkin)
    val hairSpinner = view.findViewById<Spinner>(R.id.profileHair)

    // Dropdown options. Thermal labels map to the backend's COLD/HEAT/NEUTRAL enum.
    val thermalLabels = listOf("Balanced", "Feels cold easily", "Feels hot easily")
    val thermalValues = listOf("NEUTRAL", "COLD", "HEAT")
    val styleOptions = listOf("Casual", "Smart Casual", "Elegant", "Sporty", "Boho-chic")
    val skinOptions = listOf("Fair", "Light", "Medium", "Olive", "Tan", "Deep")
    val hairOptions = listOf("Black", "Brown", "Blonde", "Red", "Gray", "Other")

    fun bind(spinner: Spinner, options: List<String>) {
      spinner.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, options).apply {
        setDropDownViewResource(R.layout.spinner_item)
      }
    }
    bind(thermalSpinner, thermalLabels)
    bind(styleSpinner, styleOptions)
    bind(skinSpinner, skinOptions)
    bind(hairSpinner, hairOptions)

    // Height / weight are picked from a bounded wheel so only realistic values are possible.
    fun numberPickerField(target: EditText, title: String, min: Int, max: Int, default: Int) {
      target.isFocusable = false
      target.isClickable = true
      target.setOnClickListener {
        // Light-themed context so the picker digits are dark and visible.
        val themed = ContextThemeWrapper(requireContext(), android.R.style.Theme_Holo_Light_Dialog)
        val picker = NumberPicker(themed).apply {
          minValue = min
          maxValue = max
          wrapSelectorWheel = false
          value = target.text.toString().trim().toIntOrNull()?.coerceIn(min, max) ?: default
        }
        val container = FrameLayout(themed).apply {
          setPadding(0, 24, 0, 0)
          addView(
            picker,
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.CENTER
            )
          )
        }
        AlertDialog.Builder(themed)
          .setTitle(title)
          .setView(container)
          .setPositiveButton("OK") { d, _ -> target.setText(picker.value.toString()); d.dismiss() }
          .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
          .show()
      }
    }
    numberPickerField(heightEditText, "Height (cm)", 120, 220, 170)
    numberPickerField(weightEditText, "Weight (kg)", 30, 200, 70)


    lifecycleScope.launch {
      closetRepository.fetchProfile()
        .catch { /* stay on defaults if the profile can't be loaded */ }
        .collect { profile ->
          val bp = profile.bodyParameters
          if (bp != null) {
            bp.height?.let { if (it > 0) heightEditText.setText(it.toInt().toString()) }
            bp.weight?.let { if (it > 0) weightEditText.setText(it.toInt().toString()) }
            skinOptions.indexOfFirst { it.equals(bp.skinTone, true) }.takeIf { it >= 0 }?.let { skinSpinner.setSelection(it) }
            hairOptions.indexOfFirst { it.equals(bp.hairColor, true) }.takeIf { it >= 0 }?.let { hairSpinner.setSelection(it) }
          }
          styleOptions.indexOfFirst { it.equals(profile.stylePreference, true) }.takeIf { it >= 0 }?.let { styleSpinner.setSelection(it) }
          thermalValues.indexOfFirst { it.equals(profile.thermalSensitivity, true) }.takeIf { it >= 0 }?.let { thermalSpinner.setSelection(it) }
        }
    }

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab?) {
        if (tab?.position == 0) {
          subtitleText.text = "Update your account details."
          personalForm.visibility = View.VISIBLE
          styleForm.visibility = View.GONE
        } else {
          subtitleText.text = "Tune the inputs your stylist uses."
          personalForm.visibility = View.GONE
          styleForm.visibility = View.VISIBLE
        }
      }
      override fun onTabUnselected(tab: TabLayout.Tab?) {}
      override fun onTabReselected(tab: TabLayout.Tab?) {}
    })

    val savePersonalBtn = view.findViewById<Button>(R.id.profileSavePersonalBtn)
    savePersonalBtn.setOnClickListener {
      val newUsername = usernameEditText.text.toString().trim()
        .ifEmpty { nameEditText.text.toString().trim() }
      val newDob = dobEditText.text.toString().trim()
      savePersonalBtn.isEnabled = false
      savePersonalBtn.text = "Saving…"
      lifecycleScope.launch {
        closetRepository.updateUser(newUsername, "", newDob)
          .catch { err ->
            Toast.makeText(context, err.message ?: "Could not save details", Toast.LENGTH_LONG).show()
            savePersonalBtn.isEnabled = true
            savePersonalBtn.text = "Save personal info"
          }
          .collect {
            Toast.makeText(context, "Personal information saved!", Toast.LENGTH_SHORT).show()
            savePersonalBtn.isEnabled = true
            savePersonalBtn.text = "Save personal info"
          }
      }
    }

    val saveStyleBtn = view.findViewById<Button>(R.id.profileSaveStyleBtn)
    saveStyleBtn.setOnClickListener {
      // Validate realistic human ranges before sending
      val height = heightEditText.text.toString().trim().toDoubleOrNull()
      if (height == null || height < 50 || height > 250) {
        heightEditText.error = "Enter a realistic height (50–250 cm)"
        heightEditText.requestFocus()
        return@setOnClickListener
      }
      val weight = weightEditText.text.toString().trim().toDoubleOrNull()
      if (weight == null || weight < 20 || weight > 300) {
        weightEditText.error = "Enter a realistic weight (20–300 kg)"
        weightEditText.requestFocus()
        return@setOnClickListener
      }

      val proportions = "Standard"
      val skinTone = skinSpinner.selectedItem.toString()
      val hairColor = hairSpinner.selectedItem.toString()
      val stylePreference = styleSpinner.selectedItem.toString()
      val thermalSensitivity = thermalValues[thermalSpinner.selectedItemPosition]

      saveStyleBtn.isEnabled = false
      saveStyleBtn.text = "Saving…"

      lifecycleScope.launch {
        combine(
          closetRepository.updateBiometrics(height, weight, proportions, skinTone, hairColor),
          closetRepository.updatePreferences(stylePreference, thermalSensitivity)
        ) { b, p -> Pair(b, p) }
          .catch { err ->
            Toast.makeText(context, "Failed to save profile: ${err.message}", Toast.LENGTH_SHORT).show()
            saveStyleBtn.isEnabled = true
            saveStyleBtn.text = "Save changes"
          }
          .collect {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            saveStyleBtn.isEnabled = true
            saveStyleBtn.text = "Save changes"
          }
      }
    }

    val uploadPhotoBtn = view.findViewById<Button>(R.id.profileUploadPhotoBtn)
    uploadPhotoBtn.setOnClickListener {
      pickImageLauncher.launch("image/*")
    }

    return view
  }
}
