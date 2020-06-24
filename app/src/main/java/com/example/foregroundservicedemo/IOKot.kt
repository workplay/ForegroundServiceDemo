package com.example.foregroundservicedemo

import android.media.MediaCodec
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object IOKot {
    @Throws(IOException::class)
    fun writeFully(fd: FileDescriptor?, from: ByteBuffer) {
        // ByteBuffer position is not updated as expected by Os.write() on old Android versions, so
        // count the remaining bytes manually.
        // See <https://github.com/Genymobile/scrcpy/issues/291>.
        var remaining = from.remaining()
        while (remaining > 0) {
            try {
                val w = Os.write(fd, from)
                if (BuildConfig.DEBUG && w < 0) {
                    // w should not be negative, since an exception is thrown on error
                    throw AssertionError("Os.write() returned a negative value ($w)")
                }
                remaining -= w
            } catch (e: ErrnoException) {
                if (e.errno != OsConstants.EINTR) {
                    throw IOException(e)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun writeFully(
        fd: FileDescriptor?,
        buffer: ByteArray?,
        offset: Int,
        len: Int
    ) {
        writeFully(fd, ByteBuffer.wrap(buffer, offset, len))
    }

    @Throws(IOException::class)
    fun sendDeviceMeta(
        fd: FileDescriptor?,
        deviceName: String,
        width: Int,
        height: Int
    ) {
        val DEVICE_NAME_FIELD_LENGTH = 64
        val buffer = ByteArray(DEVICE_NAME_FIELD_LENGTH + 4)
        val deviceNameBytes =
            deviceName.toByteArray(StandardCharsets.UTF_8)
        val len = StringUtils.getUtf8TruncationIndex(
            deviceNameBytes,
            DEVICE_NAME_FIELD_LENGTH - 1
        )
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len)
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly
        buffer[DEVICE_NAME_FIELD_LENGTH] = (width shr 8).toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 1] = width.toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 2] = (height shr 8).toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 3] = height.toByte()
        writeFully(fd, buffer, 0, buffer.size)
    }

    private val headerBuffer = ByteBuffer.allocate(12)
    private const val NO_PTS = -1
    private var ptsOrigin: Long = 0

    @Throws(IOException::class)
    fun writeFrameMeta(
        fd: FileDescriptor?,
        bufferInfo: MediaCodec.BufferInfo,
        packetSize: Int
    ) {
        val pts: Long
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            pts = NO_PTS.toLong() // non-media data packet
        } else {
            if (ptsOrigin == 0L) {
                ptsOrigin = bufferInfo.presentationTimeUs
            }
            pts = bufferInfo.presentationTimeUs - ptsOrigin
        }
        headerBuffer.apply {
            clear()
            putLong(pts)
            putInt(packetSize)
            flip()
        }

        writeFully(fd, headerBuffer)
    }
}