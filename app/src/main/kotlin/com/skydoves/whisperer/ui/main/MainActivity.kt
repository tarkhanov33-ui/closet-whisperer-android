package com.skydoves.whisperer.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.ui.add.AddItemFragment
import com.skydoves.whisperer.ui.closet.ClosetFragment
import com.skydoves.whisperer.ui.dashboard.DashboardFragment
import com.skydoves.whisperer.ui.login.LoginActivity
import com.skydoves.whisperer.ui.outfits.OutfitsFragment
import com.skydoves.whisperer.ui.profile.ProfileFragment
import com.skydoves.whisperer.core.repository.ClosetRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var bottomNavigation: BottomNavigationView
  private lateinit var greetingText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    greetingText = findViewById(R.id.headerUserGreeting)
    val userName = intent.getStringExtra("user_name").orEmpty()
    val firstName = userName.substringBefore(" ").trim()
    greetingText.text = if (firstName.isNotEmpty()) "Hi, $firstName" else "Hi"

    val signOutBtn = findViewById<ImageView>(R.id.signOutButton)
    signOutBtn.setOnClickListener {
      // Clear session cache and sign out (keep a valid system id so the next login path is well-formed)
      closetRepository.initSession("", "", "ambient_invisible_intelligence")
      val intent = Intent(this, LoginActivity::class.java)
      startActivity(intent)
      finish()
    }

    bottomNavigation = findViewById(R.id.bottom_navigation)

    bottomNavigation.setOnNavigationItemSelectedListener { item ->
      val selectedFragment: Fragment = when (item.itemId) {
        R.id.navigation_dashboard -> DashboardFragment()
        R.id.navigation_closet -> ClosetFragment()
        R.id.navigation_add -> AddItemFragment()
        R.id.navigation_outfits -> OutfitsFragment()
        R.id.navigation_profile -> ProfileFragment()
        else -> DashboardFragment()
      }
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, selectedFragment)
        .commit()
      true
    }

    // Default tab
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, DashboardFragment())
        .commit()
    }
  }

  fun selectTab(tabId: Int) {
    bottomNavigation.selectedItemId = tabId
  }
}
