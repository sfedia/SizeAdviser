package com.sizeadviser.sizeadviser

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.app.NavUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.sizeadviser.sizeadviser.databinding.ActivityFittingRoomBinding
import com.sizeadviser.sizeadviser.databinding.ActivitySettingsBinding
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            //NavUtils.navigateUpFromSameTask(this)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}