Setup guide
------
Step 1: Create a New Android Studio Project
Open Android Studio.
Click on "Start a new Android Studio project."
Choose an appropriate project template, e.g., "Empty Activity."
Set the project's name, package name, and other details.
Click "Finish" to create the project.

Step 2: Add Bluetooth Permissions to Manifest
Open the AndroidManifest.xml file.
Add the following permissions inside the <manifest> tag:
xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>


Step 3: Create UI Layout
Open res/layout/activity_main.xml.
Design the UI layout with at least three buttons (Connect, Write, Cancel) and a TextView for displaying received data.

Step 4: Copy and Replace Code
Open MainActivity.java in Android Studio.
Copy the provided MainActivity.java code.
Paste it into your MainActivity.java file, replacing the existing code.
Ensure that the package name and layout references match your project.

Step 5: Copy Connectivity Class
Right-click on your package name in the Project Explorer.
Select "New" -> "Java Class."
Name the class Connectivity.
Copy the provided Connectivity.java code.
Paste it into your Connectivity.java file.

Step 6: Run the App
Connect your Android device or use an emulator.
Click the "Run" button in Android Studio.
Choose your device and click "OK."

Step 7: Pair Devices and Test
Ensure that Bluetooth is enabled on your device.
Pair your device with another Bluetooth-enabled device.
Launch the app on both devices.
Click the Connect button on one device to connect to the other.
Click the Write button to send a hardcoded message.
Observe the received message on the other device.

Step 8: Customize and Add Comments
Explore the code and customize it according to your needs.
Add comments to explain each section of the code.
Modify the UUID in MainActivity.java with your own UUID if needed

Key Files
------
MainActivity.java
Purpose:
Acts as the main activity that users interact with.
Initializes UI components and Bluetooth-related elements.
Provides buttons for connecting to a Bluetooth device, writing data, and canceling the connection.
Key Components:
MY_UUID: A unique identifier for the Bluetooth service.
bluetoothAdapter: Represents the local Bluetooth adapter.
bluetoothSocket: Represents the Bluetooth socket for communication.
connectivity: Manages Bluetooth connection and data transfer.
receivedDataText: TextView to display received data on the UI.
Key Methods:
onCreate(): Initializes components and sets up button click listeners.
getDiscoveredDevice(): Retrieves a paired Bluetooth device for connection.
connectToDevice(): Initiates the Bluetooth connection to the selected device.
cancelConnection(): Cancels the current Bluetooth connection.

Connectivity.java
Purpose:
Represents a separate thread for managing Bluetooth connection and data transfer.
Handles reading from the InputStream, writing to the OutputStream, and canceling the connection.
Communicates with the UI thread through a Handler to update the UI with received data or connection status.
Key Components:
mmSocket: Bluetooth socket for communication.
mmInputStream: Input stream for reading data.
mmOutputStream: Output stream for writing data.
handler: Handles messages between the Connectivity class and the UI.
Key Methods:
run(): Listens to the InputStream for incoming data in a loop.
write(byte[] bytes): Writes data to the OutputStream.
cancel(): Closes the Bluetooth socket and associated streams.
Message Types (used with handler):
MESSAGE_READ: Indicates received data; updates the UI with the received data.
MESSAGE_CONNECTION_CANCELED: Indicates that the connection has been canceled; updates the UI accordingly.
This file encapsulates the logic for Bluetooth communication and ensures that it runs in a separate thread (run() method) to avoid blocking the UI thread. It uses a Handler to communicate with the UI thread and update the user interface based on the received data or connection status.

