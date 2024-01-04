package com.example.bluetoothdemo2;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final UUID MY_UUID = UUID.randomUUID();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Connectivity connectivity;

    private TextView receivedDataText;
    private TextView tv1;
    private ListView deviceListView;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private ArrayAdapter<BluetoothDevice> deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestAllPermissions();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, 1);
        }

        tv1 = findViewById(R.id.tv1);
        deviceListView = findViewById(R.id.device_list_view);
        receivedDataText = findViewById(R.id.received_data_text);

        discoveredDevices = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, discoveredDevices);
        deviceListView.setAdapter(deviceAdapter);
        deviceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        deviceListView.setOnItemClickListener((adapterView, view, position, id) -> {
            // Retrieve the selected device from the list
            BluetoothDevice selectedDevice = discoveredDevices.get(position);
            if (selectedDevice != null) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Connecting permission not granted");
                    return;
                }
                tv1.setText("Selected Device: " + selectedDevice.getName() + " (" + selectedDevice.getAddress() + ")");
                connectToDevice(selectedDevice);
            } else {
                Log.e(TAG, "No device selected");
                showToast("No device selected");
            }
        });

        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(view -> discoverDevices());

        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(view -> {
            BluetoothDevice selectedDevice = getSelectedDevice();
            if (selectedDevice != null) {
                connectToDevice(selectedDevice);
            } else {
                Log.e(TAG, "No device selected");
                showToast("No device selected");
            }
        });

        Button writeButton = findViewById(R.id.write_button);
        writeButton.setOnClickListener(view -> {
            if (connectivity != null) {
                String message = "The other device says hello!";
                byte[] bytes = message.getBytes();
                connectivity.write(bytes);
            } else {
                Log.e(TAG, "Connectivity is not initialized");
                showToast("Connectivity is not initialized");
            }
        });

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(view -> cancelConnection());
    }

    private void requestAllPermissions() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Bluetooth permissions granted");
                } else {
                    showToast("Bluetooth permissions not granted");
                }
                break;
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void discoverDevices() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
        } else {
            startDiscovery();
        }
    }

    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.BLUETOOTH, android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    private void startDiscovery() {
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Connecting permission not granted");
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
        int selectedPosition = deviceListView.getCheckedItemPosition();
        if (selectedPosition != ListView.INVALID_POSITION) {
            return discoveredDevices.get(selectedPosition);
        }
        return null;
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    discoveredDevices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private BluetoothDevice getDiscoveredDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Connecting permission not granted");
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

    private void connectToDevice(BluetoothDevice device) {
        cancelConnection();

        try {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth permission not granted");
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

            connectivity = new Connectivity(bluetoothSocket, handler);
            Thread connectivityThread = new Thread(() -> connectivity.run());
            connectivityThread.start();

            Log.d(TAG, "Connected to: " + device.getName());
            showToast("Connected to: " + device.getName());
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Bluetooth device", e);
            showToast("Error connecting to Bluetooth Device");
            cancelConnection();
        }
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryReceiver);
    }
}
