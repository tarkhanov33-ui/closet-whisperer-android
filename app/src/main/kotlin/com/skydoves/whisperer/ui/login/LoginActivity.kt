package com.skydoves.whisperer.ui.login

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private var isLoginMode = true
  private var selectedGender = "female"
  private var dateOfBirth = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    val nameInputLayout = findViewById<View>(R.id.nameInputLayout)
    val nameEditText = findViewById<EditText>(R.id.nameEditText)
    val emailEditText = findViewById<EditText>(R.id.emailEditText)
    val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
    val genderLayout = findViewById<View>(R.id.genderLayout)
    val genderRadioGroup = findViewById<RadioGroup>(R.id.genderRadioGroup)
    val dobInputLayout = findViewById<View>(R.id.dobInputLayout)
    val dobEditText = findViewById<EditText>(R.id.dobEditText)
    val submitButton = findViewById<Button>(R.id.submitButton)

    val authTitleText = findViewById<TextView>(R.id.authTitleText)
    val authSubtitleText = findViewById<TextView>(R.id.authSubtitleText)
    val toggleModeLabel = findViewById<TextView>(R.id.toggleModeLabel)
    val toggleModeButton = findViewById<TextView>(R.id.toggleModeButton)

    genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
      selectedGender = if (checkedId == R.id.genderMaleRadio) "male" else "female"
    }


    val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
    val registerHeightEditText = findViewById<EditText>(R.id.registerHeightEditText)
    val registerWeightEditText = findViewById<EditText>(R.id.registerWeightEditText)
    val thermalSpinner = findViewById<Spinner>(R.id.registerThermalSpinner)
    val styleSpinner = findViewById<Spinner>(R.id.registerStyleSpinner)
    val skinSpinner = findViewById<Spinner>(R.id.registerSkinSpinner)
    val hairSpinner = findViewById<Spinner>(R.id.registerHairSpinner)

    val thermalLabels = listOf("Balanced", "Feels cold easily", "Feels hot easily")
    val thermalValues = listOf("NEUTRAL", "COLD", "HEAT")
    fun bindSpinner(sp: Spinner, opts: List<String>) {
      sp.adapter = ArrayAdapter(this, R.layout.spinner_item, opts).apply {
        setDropDownViewResource(R.layout.spinner_item)
      }
    }
    bindSpinner(thermalSpinner, thermalLabels)
    bindSpinner(styleSpinner, listOf("Casual", "Smart Casual", "Elegant", "Sporty", "Boho-chic"))
    bindSpinner(skinSpinner, listOf("Fair", "Light", "Medium", "Olive", "Tan", "Deep"))
    bindSpinner(hairSpinner, listOf("Black", "Brown", "Blonde", "Red", "Gray", "Other"))


    fun numberPickerField(target: EditText, title: String, min: Int, max: Int, default: Int) {
      target.isFocusable = false
      target.isClickable = true
      target.setOnClickListener {
        // Light-themed context so the picker digits are dark and visible.
        val themed = ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog)
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
    numberPickerField(registerHeightEditText, "Height (cm)", 120, 220, 170)
    numberPickerField(registerWeightEditText, "Weight (kg)", 30, 200, 70)

    passwordEditText.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
      override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
        passwordInputLayout.error =
          if (isLoginMode || isStrongPassword(s?.toString() ?: "")) null
          else PASSWORD_HINT
      }
      override fun afterTextChanged(s: Editable?) {}
    })

    dobEditText.setOnClickListener {
      val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(dobEditText.windowToken, 0)
      val cal = Calendar.getInstance()
      DatePickerDialog(
        this,
        android.R.style.Theme_Holo_Light_Dialog,
        { _, year, month, day ->
          dateOfBirth = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
          dobEditText.setText(dateOfBirth)
        },
        cal.get(Calendar.YEAR) - 25,
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
      ).apply {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      }.show()
    }

    
    val registerBiometricsLayout = findViewById<View>(R.id.registerBiometricsLayout)

    toggleModeButton.setOnClickListener {
      isLoginMode = !isLoginMode
      if (isLoginMode) {
        authTitleText.setText(R.string.login_title)
        authSubtitleText.setText(R.string.login_subtitle)
        submitButton.text = "Login to dashboard"
        toggleModeLabel.text = "Don't have an account? "
        toggleModeButton.text = "Register"
        nameInputLayout.visibility = View.GONE
        genderLayout.visibility = View.GONE
        dobInputLayout.visibility = View.GONE
        registerBiometricsLayout.visibility = View.GONE
      } else {
        authTitleText.setText(R.string.register_title)
        authSubtitleText.setText(R.string.register_subtitle)
        submitButton.text = "Create my closet"
        toggleModeLabel.text = "Already have an account? "
        toggleModeButton.text = "Login"
        nameInputLayout.visibility = View.VISIBLE
        genderLayout.visibility = View.VISIBLE
        dobInputLayout.visibility = View.VISIBLE
        registerBiometricsLayout.visibility = View.VISIBLE
      }
    }

    
    submitButton.setOnClickListener {
      val email = emailEditText.text.toString().trim()
      val password = passwordEditText.text.toString().trim()

      if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      if (!isLoginMode) {
        if (!isStrongPassword(password)) {
          passwordInputLayout.error = PASSWORD_HINT
          passwordEditText.requestFocus()
          return@setOnClickListener
        }
        val h = registerHeightEditText.text.toString().trim().toDoubleOrNull()
        if (h == null || h < 50 || h > 250) {
          registerHeightEditText.error = "Enter a realistic height (50–250 cm)"
          registerHeightEditText.requestFocus()
          return@setOnClickListener
        }
        val w = registerWeightEditText.text.toString().trim().toDoubleOrNull()
        if (w == null || w < 20 || w > 300) {
          registerWeightEditText.error = "Enter a realistic weight (20–300 kg)"
          registerWeightEditText.requestFocus()
          return@setOnClickListener
        }
      }

      submitButton.isEnabled = false
      submitButton.text = "Working…"

      lifecycleScope.launch {
        if (isLoginMode) {
          closetRepository.login(email, password)
            .catch { err ->
              Toast.makeText(this@LoginActivity, err.message ?: "Login failed", Toast.LENGTH_LONG).show()
              submitButton.isEnabled = true
              submitButton.text = "Login to dashboard"
            }
            .collect { user ->
              navigateToMain(user.username, user.userId.email, user.role)
            }
        } else {
          val name = nameEditText.text.toString().trim()
          if (name.isEmpty()) {
            Toast.makeText(this@LoginActivity, "Please enter your name", Toast.LENGTH_SHORT).show()
            submitButton.isEnabled = true
            submitButton.text = "Create my closet"
            return@launch
          }

          val heightVal = registerHeightEditText.text.toString().trim().toDoubleOrNull() ?: 0.0
          val weightVal = registerWeightEditText.text.toString().trim().toDoubleOrNull() ?: 0.0
          val thermalVal = thermalValues[thermalSpinner.selectedItemPosition]
          val styleVal = styleSpinner.selectedItem.toString()
          val skinVal = skinSpinner.selectedItem.toString()
          val hairVal = hairSpinner.selectedItem.toString()

          closetRepository.register(email, name, password, selectedGender, dateOfBirth)
            .catch { err ->
              Toast.makeText(this@LoginActivity, err.message ?: "Registration failed", Toast.LENGTH_LONG).show()
              submitButton.isEnabled = true
              submitButton.text = "Create my closet"
            }
            .collect { user ->

              closetRepository.initSession(user.userId.email, password, "ambient_invisible_intelligence")
              
              try {
                if (heightVal > 0 || weightVal > 0 || skinVal.isNotEmpty() || hairVal.isNotEmpty()) {
                  closetRepository.updateBiometrics(heightVal, weightVal, "Standard", skinVal, hairVal).collect {}
                }
                if (styleVal.isNotEmpty() || thermalVal.isNotEmpty()) {
                  closetRepository.updatePreferences(styleVal, thermalVal).collect {}
                }
              } catch (_: Exception) {}

              navigateToMain(user.username, user.userId.email, user.role)
            }
        }
      }
    }
  }


  private fun isStrongPassword(pw: String): Boolean =
    pw.length >= 5 && pw.any { it.isDigit() } && pw.any { !it.isLetterOrDigit() }

  private fun navigateToMain(name: String, email: String, role: String = "END_USER") {
    val passwordInput = findViewById<EditText>(R.id.passwordEditText)?.text?.toString()?.trim() ?: ""
    closetRepository.initSession(email, passwordInput.ifEmpty { "admin" }, "ambient_invisible_intelligence")

    val intent = Intent(this, MainActivity::class.java).apply {
      putExtra("user_name", name)
      putExtra("user_email", email)
      putExtra("user_role", role)
      putExtra("user_gender", selectedGender)
      putExtra("user_dob", dateOfBirth)
    }
    startActivity(intent)
    finish()
  }

  companion object {
    private const val PASSWORD_HINT =
      "Use 5+ characters with a digit and a symbol (e.g. Passw0rd!)"
  }
}
