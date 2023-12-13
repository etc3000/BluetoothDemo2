package com.example.bluetoothdemo2;

//Importing the packages we need

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final UUID MY_UUID = UUID.randomUUID();
    //Using a random UUID for Bluetooth communication (standard practice)
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Connectivity connectivity;

    // UI components
    private TextView receivedDataText;

    private TextView tv1;
    private ListView deviceListView;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private ArrayAdapter<BluetoothDevice> deviceAdapter;

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
        // UI components
        tv1 = findViewById(R.id.tv1);
        deviceListView = findViewById(R.id.device_list_view);

        // Initialize discovered devices list and adapter
        discoveredDevices = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, discoveredDevices);
        deviceListView.setAdapter(deviceAdapter);

        // Discover Devices Button
        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(view -> discoverDevices());

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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void discoverDevices() {
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            // Request user to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Clear existing devices in the list
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();

        // Register for discovery events
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // Start discovery
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!bluetoothAdapter.isDiscovering()) {
            boolean startedDiscovery = bluetoothAdapter.startDiscovery();
            if (startedDiscovery) {
                showToast("Discovery started");
            } else {
                showToast("Failed to start discovery");
            }
        }
    }

    // BroadcastReceiver for handling device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // A Bluetooth device was found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    discoveredDevices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };
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
            showToast("Error connecting to Bluetooth Device");
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
    // Unregister the receiver when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryReceiver);
    }
}
