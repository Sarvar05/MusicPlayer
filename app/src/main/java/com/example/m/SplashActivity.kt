package com.example.m

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.Timer
import java.util.TimerTask


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val intent = Intent(this@SplashActivity,MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 1000)
    }
}