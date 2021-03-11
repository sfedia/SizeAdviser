package com.sizeadviser.sizeadviser

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView

class StartScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))
        setContentView(R.layout.activity_start_screen)
        supportActionBar?.hide()
        findViewById<TextView>(R.id.fullscreen_content2).letterSpacing = 0.3f
    }

    override fun onStart() {
        super.onStart()
        val timer = object: CountDownTimer(2000, 200) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                val intent = Intent(this@StartScreenActivity, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        timer.start()
    }
}