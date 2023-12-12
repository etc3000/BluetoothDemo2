package com.example.bluetoothdemo2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Connectivity {
    private static final String TAG = "Connectivity";
    private static final String NAME = "YourBluetoothAppName";
    private static final UUID MY_UUID = UUID.randomUUID(); // Replace with your generated UUID
    private final Context context;

    public Connectivity(Context context) {
        this.context = context;
    }

    public void startServer() {
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void startClient(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothAdapter bluetoothAdapter;
        private BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            try {
                if (hasBluetoothPermission()) {
                    // TODO: Handle Bluetooth permission request
                    return;
                }

                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                return;
            }

            if (socket != null) {
                // Handle the connected socket in the AcceptThread
                handleConnectedSocket(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing server socket", e);
                }
            }
        }

        private void handleConnectedSocket(BluetoothSocket socket) {
            // Implement your logic to handle the connected socket
            ConnectedThread connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter bluetoothAdapter;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            try {
                if (hasBluetoothPermission()) {
                    // TODO: Handle Bluetooth permission request
                    return;
                }
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            manageMyConnectedSocket(mmSocket);
        }

        private void manageMyConnectedSocket(BluetoothSocket socket) {
            // Implement your logic to handle the connected socket
            ConnectedThread connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED;
    }

    public class ConnectedThread extends Thread {
        private static final String TAG = "ConnectedThread";
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private final OutputStream mmOutputStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
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

        public void run() {
            // Code for reading from the InputStream
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    bytes = mmInputStream.read(buffer);

                    // Process the received data (e.g., update UI, trigger events)
                    // Here, you might want to handle the 'buffer' array containing received bytes
                    Log.d(TAG, "Received data: " + new String(buffer, 0, bytes));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when reading from InputStream", e);
            }
        }

        public void write(byte[] bytes) {
            try {
                // Write to the OutputStream
                mmOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when writing to OutputStream", e);
            }
        }

        public void cancel() {
            try {
                // Close the BluetoothSocket and associated streams
                mmSocket.close();
                mmInputStream.close();
                mmOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when closing the ConnectedThread", e);
            }
        }
    }
}
