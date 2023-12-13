package com.example.bluetoothdemo2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Connectivity {
    private static final String TAG = "Connectivity";
    private static final String NAME = "YourBluetoothAppName";
    private static final UUID MY_UUID = UUID.fromString("your-uuid-string"); // Replace with your generated UUID
    private BluetoothServerSocket mmServerSocket;
    private final Context context;
    private final Handler mainHandler;
    private ConnectedThread connectedThread;

    public Connectivity(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
    }

    public void startServer() {
        if (hasBluetoothPermission()) {
            showToast("Bluetooth permissions not granted");
            return;
        }
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void startClient(BluetoothDevice device) {
        if (hasBluetoothPermission()) {
            showToast("Bluetooth permissions not granted");
            return;
        }
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void writeData(byte[] data) {
        if (connectedThread != null) {
            connectedThread.write(data);
        } else {
            showToast("Not connected to any device");
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothAdapter bluetoothAdapter;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            try {
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
                handleConnectedSocket(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing server socket", e);
                }
            }
        }

        private void handleConnectedSocket(BluetoothSocket socket) {
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }

        private void showToast(String message) {
            mainHandler.post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket = null;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter bluetoothAdapter;

        public ConnectThread(BluetoothDevice device) {

            BluetoothSocket tmp = null;
            mmDevice = device;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

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

            handleConnectedSocket(mmSocket);
        }

        private void handleConnectedSocket(BluetoothSocket socket) {
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }

        private void showToast(String message) {
            mainHandler.post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
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
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                while (true) {
                    bytes = mmInputStream.read(buffer);
                    String receivedData = new String(buffer, 0, bytes);
                    Log.d(TAG, "Received data: " + receivedData);

                    mainHandler.post(() ->
                            updateUIWithReceivedData(receivedData));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when reading from InputStream", e);
            }
        }

        private void updateUIWithReceivedData(String receivedData) {
            // Implement UI update logic here
            // For example, update a TextView
            // received_data_text.setText(receivedData);
        }

        public void write(byte[] bytes) {
            try {
                mmOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when writing to OutputStream", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                mmInputStream.close();
                mmOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when closing the ConnectedThread", e);
            }
        }
    }

    private void showToast(String message) {
        mainHandler.post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
