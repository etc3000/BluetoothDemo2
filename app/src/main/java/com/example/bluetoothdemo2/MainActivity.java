package com.example.bluetoothdemo2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

/**
 * MainActivity class that extends AppCompatActivity.
 * This class is responsible for managing the Bluetooth connection and user interface.
 */
public class MainActivity extends AppCompatActivity {

    // Constants for request codes
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;

    // Bluetooth related variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private Connectivity connectivity;
    private UUID myUUID;

    // UI elements
    private TextView receivedDataText;
    private TextView tv1;
    private ListView deviceListView;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private ArrayAdapter<BluetoothDevice> deviceAdapter;

    // Media player for playing sound
    private MediaPlayer mediaPlayer;

    // Handler for managing messages from the Connectivity class
    private final Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case Connectivity.MESSAGE_READ:
                byte[] readBuffer = (byte[]) msg.obj;
                String readMessage = new String(readBuffer, 0, msg.arg1);
                receivedDataText.setText(readMessage);
                break;
            case Connectivity.MESSAGE_CONNECTION_CANCELED:
                cancelConnection();
                break;
        }
        return true;
    });

    // BroadcastReceiver for managing discovered devices
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    /**
     * Called when the activity is starting.
     * This is where most initialization should go.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        requestPermissions();

        // Initialize Bluetooth adapter and UUID
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        myUUID = getSerialPortProfileUUID();

        // Initialize UI elements
        tv1 = findViewById(R.id.tv1);
        deviceListView = findViewById(R.id.device_list_view);
        receivedDataText = findViewById(R.id.received_data_text);

        // Initialize discovered devices list and adapter
        discoveredDevices = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, discoveredDevices);
        deviceListView.setAdapter(deviceAdapter);

        // Initialize media player
        mediaPlayer = MediaPlayer.create(this, R.raw.tafd);

        // Set on item click listener for device list view
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = deviceAdapter.getItem(position);
            if (selectedDevice != null) {
                connectToDevice(selectedDevice);
            } else {
                Log.e(TAG, "No device selected");
                showToast("No device selected");
            }
        });

        // Initialize and set on click listener for play sound button
        Button playSoundButton = findViewById(R.id.play_sound_button);
        playSoundButton.setOnClickListener(view -> {
            if (connectivity != null) {
                mediaPlayer.start();
            } else {
                Log.e(TAG, "Devices are not connected");
                showToast("Devices are not connected");
            }
        });

        // Initialize and set on click listener for discover button
        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(view -> discoverDevices());

        // Initialize and set on click listener for connect button
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

        // Initialize and set on click listener for write button
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

        // Initialize and set on click listener for cancel button
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(view -> cancelConnection());
    }

    /**
     * Returns the UUID for the Serial Port Profile (SPP).
     */
    private UUID getSerialPortProfileUUID() {
        return UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    /**
     * Requests necessary permissions for the app.
     */
    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    /**
     * Requests Bluetooth permissions.
     */
    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN},
                REQUEST_LOCATION_PERMISSION);
    }

    /**
     * Callback for the result from requesting permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverDevices();
            } else {
                showToast("Location permission denied. App may not function properly.");
                finish();
            }
        }
    }

    /**
     * Shows a toast message and logs it.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    /**
     * Starts the device discovery process.
     */
    private void discoverDevices() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
        } else {
            startDiscovery();
        }
    }

    /**
     * Starts the Bluetooth discovery process.
     */
    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
            return;
        }
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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

    /**
     * Returns the selected device from the list view.
     */
    private BluetoothDevice getSelectedDevice() {
        int selectedPosition = deviceAdapter.getPosition(bluetoothDevice);
        if (selectedPosition != ListView.INVALID_POSITION) {
            tv1.setText("Device Selected!");
            return discoveredDevices.get(selectedPosition);
        }
        return null;
    }

    /**
     * Connects to the selected Bluetooth device.
     */
    private void connectToDevice(BluetoothDevice device) {
        //cancelConnection();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_ENABLE_BT);
            return;
        }
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            try {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, "Error creating bond", e);
                showToast("Error creating bond");
            }
        } else {
        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(myUUID);
            socket.connect();

            connectivity = new Connectivity(socket, handler);
            new Thread(connectivity::run).start();

            Log.d(TAG, "Connected to: " + device.getName());
            showToast("Connected to: " + device.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to Bluetooth device", e);
            showToast("Error connecting to Bluetooth Device");
            cancelConnection();
        }
        }
    }

    /**
     * Cancels the current connection.
     */
    private void cancelConnection() {
        if (connectivity != null) {
            connectivity.cancel();
            connectivity = null;
        }
    }

    /**
     * Called when the activity is destroyed.
     * Unregisters the discovery receiver.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryReceiver);
    }
}