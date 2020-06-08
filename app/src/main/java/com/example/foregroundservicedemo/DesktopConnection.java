package com.example.foregroundservicedemo;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private static final String SOCKET_NAME = "scrcpy";

    private final LocalSocket videoSocket;
    private final FileDescriptor videoFd;


    private DesktopConnection(LocalSocket videoSocket) throws IOException {
        this.videoSocket = videoSocket;
        videoFd = videoSocket.getFileDescriptor();
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    public static DesktopConnection open(String device, boolean tunnelForward) throws IOException {
        LocalSocket videoSocket;
        LocalServerSocket localServerSocket = new LocalServerSocket(SOCKET_NAME);
        try {
            videoSocket = localServerSocket.accept();
            // send one byte so the client may read() to detect a connection error
            videoSocket.getOutputStream().write(0);
        } finally {
            localServerSocket.close();
        }


        DesktopConnection connection = new DesktopConnection(videoSocket);
        return connection;
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();
    }



    public FileDescriptor getVideoFd() {
        return videoFd;
    }


}