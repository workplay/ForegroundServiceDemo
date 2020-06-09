package com.example.foregroundservicedemo

import android.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.*
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer


class ForegroundService : Service() {

    private var mBufferInfo: MediaCodec.BufferInfo? = null
    private var workHanlder: Handler? = null

    private var mMediaProjection: MediaProjection? = null

    private var mResultCode = 0
    private var mResultData: Intent? = null

    private fun startScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
        }
        val surface = createSurface()
        mMediaProjection!!.createVirtualDisplay("ScreenCapture",
            width, height, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            surface, null, null
        )
    }

    // TODO: Read paramters from device.
    private val width = 480
    private val height = 720

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val CHANNEL_ID = "ForegroundServiceChannel"  // Notification Channel ID.
        val input = intent.getStringExtra("inputExtra")
        mResultCode = intent.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra<Intent>("data")

        if (mResultCode == 0 || mResultData == null) {
            Log.e("ForgroundService", "Error: Screen record permission is rejected by user")
        }

        createEncoderThread()
        createNotificationChannel(CHANNEL_ID)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .build()
        // Create a notification on task bar.
        startForeground(1, notification)


        startScreenCapture()

        return START_STICKY
    }





    private fun createNotificationChannel(CHANNEL_ID: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }


    inner class FrameCallback {
        val LOG_TAG: String = "FrameCallback."

        fun render(
            info: MediaCodec.BufferInfo?,
            outputBuffer: ByteBuffer?
        ) {
            Log.d(LOG_TAG, info.toString())
            Log.d(LOG_TAG,outputBuffer.toString())


            IO.writeFully(MainActivity.getFileDescriptor(), outputBuffer)

        }

        fun formatChange(mediaFormat: MediaFormat?) {
            Log.d("Shiheng", "Format changed.")
        }
    }

    private fun createSurface(): Surface? {
        // Parameters and constants
        val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        val FRAME_RATE = 15 // 30fps
        val IFRAME_INTERVAL = 5 // 5 seconds between I-frames
        val BIT_RATE = 800000 // 5 seconds between I-frames


        mBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

        try {
            val mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            val mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();

            workHanlder!!.postDelayed({
                doExtract(mEncoder, FrameCallback())
            }, 1000)
            return mInputSurface!!

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun createEncoderThread() {
        val encoder = HandlerThread("Encoder")
        encoder.start()
        val looper: Looper = encoder.looper
        workHanlder = Handler(looper)
    }


    private fun doExtract(
        encoder: MediaCodec,
        frameCallback: FrameCallback?
    ) {
        val LOG_TAG = "Foreground Service."
        val VERBOSE = false
        val TIMEOUT_USEC = 10000
        var outputDone = false
        while (!outputDone) {
            val decoderStatus = encoder.dequeueOutputBuffer(mBufferInfo!!, TIMEOUT_USEC.toLong())
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // TODO: Handle this case.

            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                if (VERBOSE) Log.d(
                    LOG_TAG,
                    "decoder output format changed: $newFormat"
                )
                frameCallback?.formatChange(newFormat)
            } else if (decoderStatus < 0) {
                throw RuntimeException(
                    "unexpected result from decoder.dequeueOutputBuffer: " +
                            decoderStatus
                )
            } else { // decoderStatus >= 0
                if (VERBOSE) Log.d(
                    LOG_TAG, "surface decoder given buffer " + decoderStatus +
                            " (size=" + mBufferInfo!!.size + ")"
                )
                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (VERBOSE) Log.d(LOG_TAG, "output EOS")
                    outputDone = true
                }
                val doRender = mBufferInfo!!.size != 0
                if (doRender && frameCallback != null) {
                    val outputBuffer: ByteBuffer? = encoder.getOutputBuffer(decoderStatus)
                    frameCallback.render(mBufferInfo, outputBuffer)
                }
                encoder.releaseOutputBuffer(decoderStatus, doRender)
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
