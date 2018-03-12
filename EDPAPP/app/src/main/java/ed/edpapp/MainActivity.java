/*
Application developed as a first prototype for bluetooth communication between the app and an
adafruit bluefriend BLE. This version has 2 buttons, one to connect to a single bluetooth module,
and one to send a flash command to it.

This is developed based off the Nordic example app for UART TX/RX bluetooth communication.
 */



package ed.edpapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final String TAG = "mainActivity";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    Location locationer;
    LocationManager locationManager;
    private int mState = UART_PROFILE_DISCONNECTED;
    private ed.edpapp.UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdaptor = null;
    private Button btnConnectDisconnectLeft, btnConnectDisconnectRight, btnFlashL, btnFlashR, btnDisconnect;
    private ListView list;
    ArrayList<listItem> loclist = new ArrayList<>();
    ArrayAdapter<String> adapter;


    boolean triedLeft = false;
    boolean triedRight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationer = new Location();
        mBtAdaptor = BluetoothAdapter.getDefaultAdapter();


        list = (ListView) findViewById(R.id.listoflocations);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getLocationNames());
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String currentLat = "";
                String currentLong = " ";
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    currentLat = Double.toString(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());
                    currentLong = Double.toString(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
                }
                locationer.pollLocation(loclist.get(i).LocationLat, loclist.get(i).LocationLong, currentLat, currentLong, getApplicationContext());
            }
        });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        if(mBtAdaptor == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG);
            finish();
            return;
        }
        btnConnectDisconnectLeft = (Button) findViewById(R.id.connnectButtonLeft);
        btnConnectDisconnectRight = (Button) findViewById(R.id.connnectButtonRight);
        btnFlashR = (Button) findViewById(R.id.flashButtonRight);
        btnFlashL = (Button) findViewById(R.id.flashButtonLeft);


        service_init();



        btnConnectDisconnectLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!mBtAdaptor.isEnabled()){
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else{
                    if(btnConnectDisconnectLeft.isEnabled()){
                        Intent newIntent = new Intent(MainActivity.this, ed.edpapp.DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        triedLeft = true;
                    }
                }
            }
        });


        btnConnectDisconnectRight.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!mBtAdaptor.isEnabled()){
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else{
                    if(btnConnectDisconnectRight.isEnabled()) {
                        Intent newIntent = new Intent(MainActivity.this, ed.edpapp.DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        triedRight = true;
                    }
                }
            }
        });

        btnFlashL.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    String command = "FlashL";
                    byte[] value = command.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        btnFlashR.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    String command = "FlashR";
                    byte[] value = command.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        populateDestinations();
    }

    public void buzz(boolean left){
        try{
            String command;
            if(left){
                command = "Flash:";
            }else{
                command = "FlashR";
            }
            byte[] value = command.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void addlocation(listItem newItem){
        loclist.add(newItem);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getLocationNames());
        list.setAdapter(adapter);
    }

    private void populateDestinations(){
        listItem localoca = new listItem("Royal Albert Hall", "51.500523", "-0.177341");
        listItem localoca1 = new listItem("Imperial College London", "51.499332", "-0.174527");
        listItem localoca2 = new listItem("Buckingham Palace", "51.501912", "-0.141657");

        addlocation(localoca);
        addlocation(localoca1);
        addlocation(localoca2);

    }

    private void service_init() {
        Intent bindIntent = new Intent(this, ed.edpapp.UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((ed.edpapp.UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if(!mService.initialize()){
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast Recieved");
            Log.d(TAG, action);
            if(action.equals(UartService.ACTION_GATT_CONNECTED)){
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.i(TAG, "UART_CONNECT_MSG");
                        if(triedLeft){
                            btnFlashL.setEnabled(true);
                            btnConnectDisconnectLeft.setEnabled(false);
                        }
                        if(triedRight){
                            btnFlashR.setEnabled(true);
                            btnConnectDisconnectRight.setEnabled(false);
                        }
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }
            if(action.equals(UartService.ACTION_GATT_DISCONNECTED)){
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Log.d(TAG, "UART_DISCONNECT_MSG");
                       btnFlashL.setEnabled(false);
                       btnFlashR.setEnabled(false);
                       mState = UART_PROFILE_DISCONNECTED;
                       mService.close();
                   }
               });
            }
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

           if(action.equals(UartService.ACTION_DATA_AVAILABLE)){
               final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
               try{
                   String text = new String(txValue, "UTF-8");

                   if(text == "Button"){
                       // leftButtonPressed.setText("Left Button is pressed");
                   }
                   if(text == "noButton"){
                       //leftButtonPressed.setText("Left Button is not pressed");
                   }
               } catch (UnsupportedEncodingException e) {
                   Log.e(TAG, e.toString());
               }
           }

           if(action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
               showMessage("Device doesn't support UARt. Disconnecting");
               mService.disconnect();
           }
       }
   };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public void onActivityResult (int requestCode, int resultCode, Intent data){
       switch (requestCode){
           case REQUEST_SELECT_DEVICE:
               if(resultCode == Activity.RESULT_OK && data!=null){
                   String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                   mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                   Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                   mService.connect(deviceAddress);
               }
               break;

           case REQUEST_ENABLE_BT:
               if(resultCode == Activity.RESULT_OK){
                   Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
               }else{
                   Log.d(TAG, "BT not enabled");
                   Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                   finish();
               }
               break;
           default:
               Log.e(TAG, "wrong request code");
               break;
       }
   }
    private void showMessage(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){

        }
    };


    public void onStart() {
        super.onStart();
    }

    public void onDestroy() {

        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        locationManager.removeUpdates(locationListener);
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        }catch (Exception ignore){
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            locationer.updateLatLong(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    protected void onRestart(){
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    protected void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();
    }

    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
    }

    private String[] getLocationNames(){
        String[] locations = new String[loclist.size()];
        for(int i = 0; i<loclist.size(); i++){
            listItem temp = (listItem) loclist.get(i);
            locations[i] = temp.Location;
        }
        return locations;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {

    }
}
