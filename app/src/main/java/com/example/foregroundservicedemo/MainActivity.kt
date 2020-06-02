package com.example.foregroundservicedemo

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    // Buttons on GUI
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    // Request Code to get Screen Record Permission
    private val REQUEST_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnStartService = findViewById(R.id.buttonStartService)
        btnStopService = findViewById(R.id.buttonStopService)
        btnStartService.setOnClickListener {
            // Apply for Screen Record Service.
            val mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_CAPTURE
            )
        }

        btnStopService.setOnClickListener {
                stopService()
        }
    }

    private fun startService(resultCode: Int, data: Intent?) {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", "Recording Screen.")
        serviceIntent.putExtra("code", resultCode)
        serviceIntent.putExtra("data", data)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK) {
                startService(resultCode, data)
                Toast.makeText(this, "Capture Screen Service Starts.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Capture Screen Service Fails to start.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}