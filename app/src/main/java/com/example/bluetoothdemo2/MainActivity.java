package com.example.bluetoothdemo2;

//Importing the packages we need
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final UUID MY_UUID =  UUID.randomUUID();
    //Using a random UUID for Bluetooth communication (standard practice)
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Connectivity connectivity;

    // UI components
    private TextView receivedDataText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            finish();
        }

        // Reference UI components
        receivedDataText = findViewById(R.id.received_data_text);
        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(view -> {
            BluetoothDevice discoveredDevice = getDiscoveredDevice();
            if (discoveredDevice != null) {
                connectToDevice(discoveredDevice);
            } else {
                Log.e(TAG, "No discovered device available");
            }
        });

        Button writeButton = findViewById(R.id.write_button);
        writeButton.setOnClickListener(view -> {
            if (connectivity != null) {
                // This is a hardcoded string to be sent over Bluetooth
                // via the write button (simple implementation)
                String message = "The other device says hello!";
                byte[] bytes = message.getBytes();
                connectivity.write(bytes);
            } else {
                Log.e(TAG, "Connectivity is not initialized");
            }
        });

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(view -> cancelConnection());
    }
    //Method for discovering paired Bluetooth devices
    private BluetoothDevice getDiscoveredDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                return device;
            }
        }
        return null;
    }
    //Method for connecting to another Bluetooth Device
    private void connectToDevice(BluetoothDevice device) {
        cancelConnection();

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();

            Handler handler = new Handler(message -> {
                switch (message.what) {
                    case Connectivity.MESSAGE_READ:
                        byte[] readBuf = (byte[]) message.obj;
                        String receivedData = new String(readBuf, 0, message.arg1);
                        receivedDataText.setText(receivedData);
                        break;

                    case Connectivity.MESSAGE_CONNECTION_CANCELED:
                        receivedDataText.setText("Connection canceled");
                        break;
                }
                return true;
            });
            //initialize our Connectivity class, start threads for operations
            connectivity = new Connectivity(bluetoothSocket, handler);
            Thread connectivityThread = new Thread((Runnable) connectivity);
            connectivityThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Bluetooth device", e);
        }
    }
    // Button / Method to cancelConnection
    private void cancelConnection() {
        if (connectivity != null) {
            connectivity.cancel();
            connectivity = null;
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Bluetooth socket", e);
            }
            bluetoothSocket = null;
        }
    }
}
