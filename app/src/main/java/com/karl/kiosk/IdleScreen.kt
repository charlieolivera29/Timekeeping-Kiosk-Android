package com.karl.kiosk

import android.annotation.SuppressLint
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_splash_screen.*

class IdleScreen : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }
}
