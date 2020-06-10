package com.example.foregroundservicedemo

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer


class ForegroundService : Service() {

    private var mBufferInfo: MediaCodec.BufferInfo? = null

    private var mMediaProjection: MediaProjection? = null

    private var mResultCode = 0
    private var mResultData: Intent? = null

    // TODO: Read paramters from device.
    private val width = 480
    private val height = 720

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {


        mResultCode = intent.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra<Intent>("data")

        if (mResultCode == 0 || mResultData == null) {
            Log.e("ForgroundService", "Error: Screen record permission is rejected by user")
        }

        createNotification()

        Thread {
            startScreenProjection()
        }.start()


        return START_STICKY
    }



    private fun createNotification() {
        val CHANNEL_ID = "ForegroundServiceChannel"  // Notification Channel ID.
        val input = "Screen Projection Started."
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
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(input)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }


    private fun createModiCodec(): MediaCodec {
        // Parameters and constants
        val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        val FRAME_RATE = 60 // 30fps
        val IFRAME_INTERVAL = 10 // 5 seconds between I-frames
        val BIT_RATE = 8000000 // 5 seconds between I-frames

        mBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)

        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);

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
        val mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return mEncoder
    }

    private fun startScreenProjection() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
        }



        try {
            val mEncoder = createModiCodec()
            val mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();

            mMediaProjection!!.createVirtualDisplay("ScreenCapture",
                width, height, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mInputSurface, null, null
            )

            doExtract(mEncoder)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    private fun doExtract(encoder: MediaCodec) {
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
                Log.d(LOG_TAG, "decoder output format changed: $newFormat")

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
                if (doRender ) {
                    val outputBuffer: ByteBuffer? = encoder.getOutputBuffer(decoderStatus)
                    IO.writeFrameMeta(MainActivity.getFileDescriptor(), mBufferInfo!!, outputBuffer!!.remaining())
                    //Log.d(LOG_TAG, info.toString())
                    //Log.d(LOG_TAG,outputBuffer.toString())
                    IO.writeFully(MainActivity.getFileDescriptor(), outputBuffer)
                }
                encoder.releaseOutputBuffer(decoderStatus, doRender)
            }
        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
