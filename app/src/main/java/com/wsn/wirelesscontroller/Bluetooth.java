package com.wsn.wirelesscontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

// Interface for the UI, so that I can change UI elements in the Connect and Connected threads
interface bluetoothInterface{
    void connected();
    void disconnected();
}

public class Bluetooth implements bluetoothInterface {

    private static final    String              TAG                 = "Bluetooth";
    private static final    String              BT_RETROPIE_NAME    = "retropie";

    private                 Activity            activity;
    private                 BluetoothAdapter    mBluetoothAdapter;


    private static final    int                 REQUEST_ENABLE_BT   = 10;
    private static          boolean             retropie_found      = false;

    private static final    UUID                MY_UUID_INSECURE    = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // thread to open a socket to the pi
    private                 ConnectThread       mConnectThread;

    // thread for sending data to the pi through thr open socket
    private                 ConnectedThread     mConnectedThread;

    private                 bluetoothInterface    bt_interface      = null;

    // These are basically placeholders, they will be implemented
    // when the interface is passed into the Bluetooth class
    public void connected() {}
    public void disconnected() {}

    public Bluetooth(Activity activity, bluetoothInterface bt_interface){

        this.activity = activity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null){
            Toast.makeText(activity, "Device does not support bluetooth.", Toast.LENGTH_SHORT).show();
        }else{
            // register the broadcast receiver for when bluetooth discovery starts
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            activity.registerReceiver(mReceiver, filter);
        }

        this.bt_interface = bt_interface;
    }

    /**
     * broadcast receiver for found devices after calling mBluetoothAdapter.startDiscovery()
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG,"discovery started...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"discovery finished.");
                if(!retropie_found){
                    Toast.makeText(activity, "Retropie not found.", Toast.LENGTH_LONG).show();
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG,"FOUND   deviceName: "+deviceName+"    MAC: "+deviceHardwareAddress);
                if (device.getName().equalsIgnoreCase(BT_RETROPIE_NAME)){
                    retropie_found = true;
                    connectRetroPie(device);
                }
            }
        }
    };

    public void enable(){
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(activity, "Bluetooth enabled.", Toast.LENGTH_LONG).show();
        }
    }
    public void disable(){
        if(mBluetoothAdapter.isEnabled()){
            this.closeConnection();
            mBluetoothAdapter.disable();
            Toast.makeText(activity, "Bluetooth disabled.", Toast.LENGTH_LONG).show();
        }
    }
    public boolean isEnabled(){
        return mBluetoothAdapter.isEnabled();
    }

    public void findRetroPie(){
        retropie_found = false;
        this.getBondedDevices();
    }

    /**
     * Get the list of paired devices
     */
    private void getBondedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            Log.d(TAG,"BONDED DEVICES:");
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG,"\t\tdeviceName: "+deviceName+"    MAC: "+deviceHardwareAddress);

                if(deviceName.equalsIgnoreCase(BT_RETROPIE_NAME)){
                    retropie_found = true;
                    connectRetroPie(device);
                }
            }
        }

        if(!retropie_found){discover();}
    }

    /**
     * Start scanning for bluetooth devices
     */
    private void discover(){
        if(mBluetoothAdapter.isEnabled()) {
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
        }
    }

    /**
     * Found retropie, now start thread to connect to it.
     * @param device
     */
    public void connectRetroPie(BluetoothDevice device){
        Toast.makeText(activity, "Found retropie, now connecting...", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Found retropie, now connecting...");
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            Log.d(TAG, "socket open to retropie.");

            if(mConnectedThread != null){
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                bt_interface.disconnected();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /**
     Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            bt_interface.connected();

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
                bt_interface.disconnected();
            } catch (IOException e) { }
        }
    }


    /**
     * Allow bluetooth class to write to connected device
     * @param msg
     */
    public void write(String msg){
        if(mConnectedThread != null){
            mConnectedThread.write(msg.getBytes());
        }
    }

    public void closeConnection(){
        if(mConnectedThread != null){
            //send 'q' to quit the python bluetooth server socket
            mConnectedThread.write("q".getBytes());
            mConnectedThread.cancel();
        }
    }

    /**
     * make sure bt discovery is stopped
     */
    public void release(){
        mBluetoothAdapter.cancelDiscovery();
        closeConnection();
        activity.unregisterReceiver(mReceiver);
    }

}
