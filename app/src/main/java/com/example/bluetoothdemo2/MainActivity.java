package com.example.bluetoothdemo2;

// Importing the packages we need

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
    // Using a random UUID for Bluetooth communication (standard practice)
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
        deviceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE); // allows user to select a device

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Retrieve the selected device from the list
                BluetoothDevice selectedDevice = discoveredDevices.get(position);
                if (selectedDevice != null) {
                    connectToDevice(selectedDevice);
                } else {
                    Log.e(TAG, "No device selected");
                    showToast("No device selected");
                }
            }
        });
        // Discover Devices Button
        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverDevices();
            }
        });

        // Connect Button
        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothDevice selectedDevice = getSelectedDevice();
                if (selectedDevice != null) {
                    connectToDevice(selectedDevice);
                } else {
                    Log.e(TAG, "No device selected");
                    showToast("No device selected");
                }
            }
        });

        // Write Button
        Button writeButton = findViewById(R.id.write_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectivity != null) {
                    // Hardcoded string to be sent over Bluetooth
                    String message = "The other device says hello!";
                    byte[] bytes = message.getBytes();
                    connectivity.write(bytes);
                } else {
                    Log.e(TAG, "Connectivity is not initialized");
                    showToast("Connectivity is not initialized");
                }
            }
        });

        // Cancel Button
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelConnection();
            }
        });
    }
    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.BLUETOOTH, android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void discoverDevices() {
        // Check if location services are enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Prompt user to enable location services
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        // Check if Bluetooth and location permissions are enabled
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If permissions are not granted, request them
            requestBluetoothPermissions();
        } else {
            // Permissions are already granted, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery() {
        // Clear existing devices in the list
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();

        // Register for discovery events
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // Start discovery
        if (!bluetoothAdapter.isEnabled()) {
            // Request user to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, 1);
        } else {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            boolean startedDiscovery = bluetoothAdapter.startDiscovery();
            if (startedDiscovery) {
                showToast("Discovery started");
            } else {
                showToast("Failed to start discovery");
            }
        }
    }

    private BluetoothDevice getSelectedDevice() {
        // Retrieve the selected device from the device_list_view
        int selectedPosition = deviceListView.getCheckedItemPosition();
        if (selectedPosition != ListView.INVALID_POSITION) {
            return discoveredDevices.get(selectedPosition);
        }
        return null;
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

    // Method for discovering paired Bluetooth devices
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

    // Method for connecting to another Bluetooth Device
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
            // initialize our Connectivity class, start threads for operations
            connectivity = new Connectivity(bluetoothSocket, handler);
            Thread connectivityThread = new Thread((Runnable) connectivity);
            connectivityThread.start();

            // Add a log statement or Toast for successful connection
            Log.d(TAG, "Connected to: " + device.getName());
            showToast("Connected to: " + device.getName());
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
