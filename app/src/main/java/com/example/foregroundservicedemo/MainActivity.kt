package com.example.foregroundservicedemo

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.LocalServerSocket
import android.net.LocalSocket
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

        // build connections here.
        serverThread = ServerThreadConnect()
        serverThread!!.start()
        serverThread!!.join()

        Log.d("shiheng", "Connection built")

        //val handlerThread = ServerThreadHanlder()
        //handlerThread!!.start()

    }

    override fun onDestroy() {
        super.onDestroy()
        serverIsLoop = false
    }

    var serverThread: ServerThreadConnect? = null

    inner class ToastMessageHandler: Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.getData().getString("MSG", "Toast"), Toast.LENGTH_SHORT).show()
        }
    }

    inner class ServerThreadConnect : Thread() {
        override fun run() {
            var serverSocket: LocalServerSocket? = null
            try {
                serverSocket = LocalServerSocket("localServer")
                socket = serverSocket.accept()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class ServerThreadHanlder : Thread() {
        val TAG = "ServerThread"
        private val handler: ToastMessageHandler = ToastMessageHandler()

        override fun run() {
            try {
                Log.d(TAG, "connected.")
                val inputStream = DataInputStream(socket.inputStream)
                val outputStream = DataOutputStream(socket.outputStream)


                while (serverIsLoop) {
                    Log.d(TAG, "accept")
                    val msg: String = inputStream.readUTF()
                    val bundle = Bundle().apply { putString("MSG", msg) }
                    val message = Message.obtain().apply { data = bundle }

                    outputStream.writeUTF("Received.")

                    handler.sendMessage(message)
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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