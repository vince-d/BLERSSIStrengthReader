package edu.teco.blerssistrengthtest;

import android.app.ActivityOptions;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class SelectBLEDeviceActivity extends AppCompatActivity {

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private ListView mListView;
    private ArrayAdapter<String> mDeviceListAdapter;

    private FloatingActionButton mFab;

    BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean mDownloading = false;
    private boolean mEnable;

    private ArrayList<BluetoothDevice> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_bledevice);


        mDevices = new ArrayList<>();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE on this device.", Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager bMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bMan.getAdapter() == null || !bMan.getAdapter().isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first.", Toast.LENGTH_LONG).show();
            finish();
        }



        mFab = (FloatingActionButton) findViewById(R.id.fab);


        mFab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Snackbar.make(findViewById(R.id.mainLayout),
                        "Started 10 second BLE scan.", Snackbar.LENGTH_LONG).show();
                v.setAlpha(0.3f);
                v.setClickable(false);
                scanLeDevice(true);
            }
        });

        mListView = (ListView) findViewById(R.id.deviceList);
        mDeviceListAdapter = new ArrayAdapter<String>(this, R.layout.list_item, new ArrayList <String>());
        mListView.setAdapter(mDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice dev = mDevices.get(position);
                scanLeDevice(false);


                Intent i = new Intent(SelectBLEDeviceActivity.this,MainActivity.class);
                i.putExtra("mac", dev.getAddress());
                i.putExtra("name", dev.getName());
                SelectBLEDeviceActivity.this.startActivity(i, ActivityOptions.makeSceneTransitionAnimation(SelectBLEDeviceActivity.this,
                        new Pair<View, String>(view, "transition1"), new Pair<View, String>(mFab, "fabTrans")).toBundle());
            }
        });

        mHandler = new Handler(Looper.getMainLooper());

        mBluetoothAdapter = bMan.getAdapter();
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mEnable = false;
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD);

            mDevices.clear();
            mDeviceListAdapter.clear();
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mFab.setAlpha(1.0f);
            mFab.setClickable(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        scanLeDevice(false);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            mDeviceListAdapter.add(device.getName() + " - " + device.getAddress());
            mDevices.add(device);
        }
    };


}
