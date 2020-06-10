package com.example.foregroundservicedemo

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.IOException
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity() {
    // Buttons on GUI
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    // Request Code to get Screen Record Permission
    private val REQUEST_CAPTURE = 1



    companion object{
        private lateinit var serverSocket: LocalServerSocket
        lateinit var socket: LocalSocket

        fun  getFileDescriptor(): FileDescriptor{
            return socket.fileDescriptor
        }
    }


    private var serverIsLoop = true

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


        // val thread = ClientThreadConnect()
        val thread = ServerThreadConnect()

        thread!!.start()
        thread!!.join()

        Log.d("shiheng", "Connection built")
    }

    override fun onDestroy() {
        super.onDestroy()
        serverIsLoop = false
    }

    inner class ServerThreadConnect : Thread() {
        override fun run() {
            var serverSocket: LocalServerSocket? = null
            try {
                serverSocket = LocalServerSocket("scrcpy")
                socket = serverSocket.accept()
                socket.getOutputStream().write(0);

            } catch (e: Exception) {
                e.printStackTrace()
            }
            IO.sendDeviceMeta(getFileDescriptor(), "Shiheng Device",480, 720)
        }
    }

    inner class ClientThreadConnect : Thread() {
        override  fun run() {
            socket = LocalSocket()
            socket.connect(LocalSocketAddress("scrcpy"))
            try {
                val controlSocket = LocalSocket()
                controlSocket.connect(LocalSocketAddress("scrcpy"))
            } catch (e: Exception) {
                socket.close()
            }
            IO.sendDeviceMeta(getFileDescriptor(), "Shiheng Device",480, 720)
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