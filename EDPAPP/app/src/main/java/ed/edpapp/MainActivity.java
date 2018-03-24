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
import android.widget.TextView;
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

    private int mState = UART_PROFILE_DISCONNECTED;
    private ed.edpapp.UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdaptor = null;
    private Button bluetoothBtn, buzzBtn;
    private ListView list;
    ArrayList<listItem> loclist = new ArrayList<>();
    ArrayAdapter<String> adapter;

    TextView currentLocation;
    TextView nextLocation;
    LocationManager locationManager;
    Location locationer = new Location();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //The UI elements for the test app
        currentLocation = (TextView) findViewById(R.id.currentlocdisp);
        nextLocation = (TextView) findViewById(R.id.nextlocdisp);
        bluetoothBtn = (Button) findViewById(R.id.BluetoothButton);
        mBtAdaptor = BluetoothAdapter.getDefaultAdapter();
        buzzBtn = (Button) findViewById(R.id.buzzer);
        list = (ListView) findViewById(R.id.locationlist);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getLocationNames());
        list.setAdapter(adapter);

        //Gets the chosen item from the list of locations
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Start(i);
            }
        });
        //initiates the location manager with GPS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        //Button that displays a list of BLE devices
        bluetoothBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(!mBtAdaptor.isEnabled()){
                    Log.i(TAG, "onClick = BT not enabled yet");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, REQUEST_SELECT_DEVICE);
                }
            }
        });
        //button to test the buzz feature
        buzzBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                buzz(true);
            }
        });
        //Closes the app if the bluetooth hardware is not present
        if(mBtAdaptor == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG);
            finish();
            return;
        }
        service_init();
        populateDestinations();
    }

    //Sends the command to buzz either the left side or the right side
    public void buzz(boolean left){
        try{
            String command;
            if(left){
                command = "FlashL";
            }else{
                command = "FlashR";
            }
            byte[] value = command.getBytes("UTF-8");
            try{

                mService.writeRXCharacteristic(value);
            }catch(Exception e){
                Log.i(TAG, "No Bluetooth Device");
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    //adds the passed listItem to the displayed list of locations
    private void addlocation(listItem newItem){
        loclist.add(newItem);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getLocationNames());
        list.setAdapter(adapter);
    }

    //a small test list of locations
    private void populateDestinations(){
        listItem localoca = new listItem("Royal Albert Hall", "51.500523", "-0.177341");
        listItem localoca1 = new listItem("Imperial College London", "51.499332", "-0.174527");
        listItem localoca2 = new listItem("Buckingham Palace", "51.501912", "-0.141657");

        addlocation(localoca);
        addlocation(localoca1);
        addlocation(localoca2);

    }

    //starts the UART service for the bluetooth communication
    private void service_init() {
        Intent bindIntent = new Intent(this, ed.edpapp.UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    //bluetooth configuration/setup
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

    //onReceive will be called every time the system sees that something
    //related to bluetooth has happened
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
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }
            if(action.equals(UartService.ACTION_GATT_DISCONNECTED)){
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Log.d(TAG, "UART_DISCONNECT_MSG");

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

                   if(text == "LButton"){
                       compassmode();
                   }
                   if(text == "RButton"){

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
    //code that would contain the setup for the compass mode discussed as a possible addition
    void compassmode(){

    }
    //Following 5 methods are general configuration methods for the bluetooth connection
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
               }else{
                   Log.d(TAG, "pressing the thing didn't work");
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

    //onStart, onDestroy, onPause, onRestart, onStop, onResume, are the methods called when the app
    //closes and opens
    public void onStart() {
        super.onStart();
    }

    public void onDestroy() {

        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        }catch (Exception ignore){
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
    }

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

    //returns all the names contained in loclist
    private String[] getLocationNames(){
        String[] locations = new String[loclist.size()];
        for(int i = 0; i<loclist.size(); i++){
            listItem temp = (listItem) loclist.get(i);
            locations[i] = temp.Location;
        }
        return locations;
    }

    //Starts the GPS service
    void Start(int i){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationer.pollLocation(loclist.get(i).getLat(), loclist.get(i).getLong(), Double.toString(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()),
                    Double.toString(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude()), MainActivity.this);
            displayCurrent(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
    }

    //displayCurrent and displayNext show the current and next locations on the screen
    public void displayCurrent(android.location.Location location){
        currentLocation.setText(location.getLatitude() + ", " + location.getLongitude());
    }
    public void displayNext(stepItem location){
        nextLocation.setText(location.getLat() + ", " + location.getLong());
    }

    //Callback methods which are called when certain GPS activities occur.
    LocationListener locationListener = new LocationListener() {
        @Override
        //called upon a location change, calling appropriate behaviours
        public void onLocationChanged(android.location.Location location) {
            displayCurrent(location);
            if(locationer.isAtLocation(location)){
                Log.i(TAG, "Is At Location");
                buzz(locationer.goLeft());
            }
            displayNext(locationer.nextLocation());

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

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {

    }
}
