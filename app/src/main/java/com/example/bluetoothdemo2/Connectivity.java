package com.example.bluetoothdemo2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//public Class Constructor, holds all of our inner classes
// Inner classes are our run/read/write/cancel operations etc.
// thread-safe by making each process run on its own thread.
public class Connectivity {
    static final String TAG = "Connectivity";
    static final int MESSAGE_READ = 1;
    static final int MESSAGE_CONNECTION_CANCELED = 2;

    private final BluetoothSocket mmSocket;
    private final InputStream mmInputStream;
    private final OutputStream mmOutputStream;
    private final Handler handler;

    public Connectivity(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        this.handler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating InputStream or OutputStream", e);
        }

        mmInputStream = tmpIn;
        mmOutputStream = tmpOut;
    }

    //reading data from inputStream (hardcoded string sent from other device)
    public void run() {
        // Code for reading from the InputStream
        byte[] buffer = new byte[1024];
        int bytes;

        try {
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                bytes = mmInputStream.read(buffer);

                // Process the received data
                // Send the obtained bytes to the UI activity
                handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when reading from InputStream", e);
            // Notify the UI activity about the connection cancellation
            handler.obtainMessage(MESSAGE_CONNECTION_CANCELED).sendToTarget();
        }
    }

    // Method to write data to the OutputStream
    public void write(byte[] bytes) {
        try {
            mmOutputStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when writing to OutputStream", e);
        }
    }

    // Method to cancel the connection
    public void cancel() {
        try {
            mmSocket.close();
            mmInputStream.close();
            mmOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when closing the ConnectedThread", e);
        }

        // Notify the UI activity about the connection cancellation
        handler.obtainMessage(MESSAGE_CONNECTION_CANCELED).sendToTarget();
    }
}
