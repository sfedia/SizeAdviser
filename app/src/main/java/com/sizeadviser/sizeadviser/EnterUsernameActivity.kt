package com.sizeadviser.sizeadviser

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.sizeadviser.sizeadviser.databinding.ActivityEnterUsernameBinding


class EnterUsernameActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityEnterUsernameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))

        binding = DataBindingUtil.setContentView(this, R.layout.activity_enter_username)

        auth = FirebaseAuth.getInstance()

        binding.saveUsername.setOnClickListener { saveAndGo() }

    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser!!
        if (currentUser.displayName != null) {
            val intent = Intent(this@EnterUsernameActivity, CalibrationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveAndGo() {
        val newName = binding.UserName.text.toString()
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName).build()
        user!!.updateProfile(profileUpdates)
        val intent = Intent(this@EnterUsernameActivity, CalibrationActivity::class.java)
        val b = Bundle()
        b.putString("displayName", newName)
        intent.putExtras(b)
        startActivity(intent)
        finish()
    }
}