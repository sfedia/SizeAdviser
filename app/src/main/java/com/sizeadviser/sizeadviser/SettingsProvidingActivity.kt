package com.sizeadviser.sizeadviser
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.marginBottom
import androidx.preference.PreferenceManager
import com.facebook.login.LoginManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.sizeadviser.sizeadviser.databinding.ActivityCalibrationBinding
import com.sizeadviser.sizeadviser.databinding.ActivityEmailPasswordBinding
import com.sizeadviser.sizeadviser.databinding.ActivityMyCollectionBinding


open class SettingsProvidingActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var auth: FirebaseAuth
    lateinit var sharedPreferences: SharedPreferences
    var hRatio: Double = 0.0
    var wRatio: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))
        auth = FirebaseAuth.getInstance()
        setupSharedPreferences()
    }

    fun setupSharedPreferences(): SharedPreferences {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        return sharedPreferences
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.authorized_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sign_out -> {
                signOut()
            }
            R.id.menu_settings -> {
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun signOut() {
        auth.signOut()
        LoginManager.getInstance().logOut()
        val intent = Intent(applicationContext, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "profile_user_name" -> {
                val user = auth.currentUser!!
                val newName: String = sharedPreferences?.getString(
                    "profile_user_name", user.displayName!!
                ).toString()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName).build()
                user.updateProfile(profileUpdates).addOnCompleteListener {
                    val api = SizeAdviserApi()
                    if (sharedPreferences != null) {
                        api.registerCurrentUser(sharedPreferences, update = true)
                    }
                }
            }
            "profile_gender" -> {
                val api = SizeAdviserApi()
                if (sharedPreferences != null) {
                    api.registerCurrentUser(sharedPreferences, update = true)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}