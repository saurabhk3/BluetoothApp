package com.example.bluetoothapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    TextView searchTextView,statusTextView;
    ListView listView;
    Button searchButton;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> devices = new ArrayList<>();
    ArrayList<String> nameList = new ArrayList<>();
    ArrayAdapter<String> adapter ;
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


    public void searchDevice(View view){
        searchTextView.setText("Searching...");
        searchButton.setEnabled(false);

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        devices.clear();
        nameList.clear();
        bluetoothAdapter.startDiscovery();
    }
}
