package com.sizeadviser.sizeadviser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.sizeadviser.sizeadviser.databinding.ActivityCalibrationBinding
import com.sizeadviser.sizeadviser.DatabaseHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser

class CalibrationActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityCalibrationBinding
    var genderSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))

        binding = DataBindingUtil.setContentView(this, R.layout.activity_calibration)

        auth = FirebaseAuth.getInstance()

        binding.maleButton.setOnClickListener {
            it.setBackgroundColor(applicationContext.getColor(R.color.paletteBlue))
            binding.femaleButton.background = null
            saveGender(0)
        }
        binding.femaleButton.setOnClickListener {
            it.setBackgroundColor(applicationContext.getColor(R.color.palettePink))
            binding.maleButton.background = null
            saveGender(1)
        }
        binding.calibrationOK.setOnClickListener { proceedToProfile() }

    }

    public override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser!!

        val db = DatabaseHandler(this, null)
        try {
            db.createGITable()
        } catch (e: android.database.sqlite.SQLiteException) {}

        var dName: String? = null
        dName = if (currentUser.displayName == null) {
            val gotExtra = intent.getStringExtra("displayName")
            gotExtra
        } else {
            currentUser.displayName
        }

        if (dName != null) {
            binding.welcomeHeader.text = applicationContext.getString(
                R.string.calibration_welcome_header_name,
                dName.split(" ")[0]
            )
        }
        else {
            binding.welcomeHeader.visibility = View.INVISIBLE
            binding.welcomeHeader2.text = applicationContext.getString(
                R.string.calibration_welcome_header_noname
            )
        }


        val gender: Int = db.getGenderIntOf(currentUser.uid)

        if (gender != -1) {
            proceedToProfile(force = true)
        }
    }

    private fun saveGender(gender: Int) {
        genderSelected = true
        val db = DatabaseHandler(this, null)
        val user = auth.currentUser!!.uid
        try {
            db.createGITable()
        } catch (e: android.database.sqlite.SQLiteException) {}
        db.addGenderInfo(user, gender)
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("profile_gender", gender.toString())
        editor.commit()
    }

    private fun proceedToProfile(force: Boolean = false) {
        if (genderSelected || force) {
            val intent = Intent(this@CalibrationActivity, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(baseContext, "Select your gender before you proceed to the profile",
                Toast.LENGTH_SHORT).show()
        }
    }
}