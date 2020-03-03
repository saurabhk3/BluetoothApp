package com.example.bluetoothapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    TextView searchTextView,statusTextView;
    ListView listView;
    Button searchButton;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> devices = new ArrayList<>();
    ArrayList<String> nameList = new ArrayList<>();
    ArrayAdapter<String> adapter ;
    private static final String NAME = "BluetoothApp";
    private static final UUID MY_UUID = UUID.fromString("85539a92-5d8d-11ea-bc55-0242ac130003");

    static final int  STATE_CONNECTED = 1;
    static final int  STATE_CONNECTING = 2;
    static final int  STATE_CONNECTION_FAILED = 3;
    static final int  STATE_LISTENING = 4;
    static final int  STATE_MESSAGE_RECEIVED = 5;

    BroadcastReceiver receiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action::",action);
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                searchTextView.setText("Finished!");
                searchButton.setEnabled(true);
            }
            else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                String name = device.getName();
                String deviceString = name + "    Quality: "+rssi+"dBm";

                 if(!nameList.contains(name)){
                    nameList.add(name);
                    devices.add(deviceString);
                }
                statusTextView.setText("Available Devices :");
            }

          adapter.notifyDataSetChanged();
       }
   };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchTextView = findViewById(R.id.search);
        searchButton = findViewById(R.id.searchButton);
        listView = findViewById(R.id.listView);
        statusTextView = findViewById(R.id.status);
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_activated_1,devices);
        listView.setAdapter(adapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(receiver,filter);

    }

    public void Destroy(){
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public void searchDevice(View view){
        searchTextView.setText("Searching...");
        searchButton.setEnabled(false);


        devices.clear();
        nameList.clear();
        bluetoothAdapter.startDiscovery();
    }
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_CONNECTED:
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    break;
                case STATE_CONNECTING:
                    Toast.makeText(MainActivity.this,"Connecting",Toast.LENGTH_LONG).show();
                    break;
                case STATE_CONNECTION_FAILED:
                    Toast.makeText(MainActivity.this,"Failed!",Toast.LENGTH_SHORT).show();
                    break;
                case STATE_LISTENING:
                    Toast.makeText(MainActivity.this, "Listening", Toast.LENGTH_LONG).show();
                    break;
                case STATE_MESSAGE_RECEIVED:
                    Toast.makeText(MainActivity.this, "Rec!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    });

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME,MY_UUID);
            }catch (Exception e){
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket socket = null;
            while(true){  // keep listening until exception occurs or a socket is returned
                try{
                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTING;
                    handler.sendMessage(msg);
                    socket = mmServerSocket.accept();
                }catch (IOException e){
                    Log.i("Status: ","Socket's accept method failed!");
                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(msg);
                    break;
                }
                if(socket!= null){
                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTED;
                    handler.sendMessage(msg);

                    // send/receive
                    break;
                }
            }
        }
        public void cancel(){
            try{
                mmServerSocket.close();
            } catch (IOException e) {
                Log.i("Status:","Could not close connect socket ");
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            try{
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch(IOException e){
                Log.i("Status: ","Socket's create method failed(Client)");
            }
            mmSocket = tmp;
        }
        public void run(){
            // cancel discovery otherwise it slows down the connection
            bluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            }catch(IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.i("Status: ", "Could not close the client socket");
                }
                return;
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.

        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){
                Log.i("Status :","Could not close the client socket");
            }
        }


    }
}

