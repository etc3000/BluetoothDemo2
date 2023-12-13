package com.example.bluetoothdemo2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private Connectivity connectivity;
    private TextView readTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bluetooth setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Device does not support Bluetooth");
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
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
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }

        // UI setup
        readTextView = findViewById(R.id.received_data_text);

        // Handler for updating UI from background threads
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Connectivity setup
        connectivity = new Connectivity(this, mainHandler);

        // Discover Devices Button
        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(v -> discoverDevices());

        // Connect Button
        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(v -> connectToSelectedDevice());

        // Cancel Button
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> cancelBluetoothOperation());

        // Write Button
        Button writeButton = findViewById(R.id.write_button);
        writeButton.setOnClickListener(v -> writeDataToConnectedDevice());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void discoverDevices() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        showToast("Discovering devices...");
        // Implement Bluetooth device discovery logic
    }

    private void connectToSelectedDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        showToast("Connecting to the selected device...");
        // Implement Bluetooth device connection logic
    }

    private void cancelBluetoothOperation() {
        showToast("Bluetooth operation canceled");
        // Implement Bluetooth operation cancellation logic
    }

    private void writeDataToConnectedDevice() {
        // Example: Write a hardcoded string to the connected device
        String message = "The other device says hello!";
        byte[] data = message.getBytes();
        connectivity.writeData(data);
    }
}
