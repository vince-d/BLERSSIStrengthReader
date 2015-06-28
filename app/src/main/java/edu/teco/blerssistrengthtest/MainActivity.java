package edu.teco.blerssistrengthtest;

// General imports.
import android.animation.Animator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

// Androidplot imports.
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.text.DecimalFormat;



public class MainActivity extends AppCompatActivity {

    private static final String _TAG = "MainActivity";
    private BluetoothGatt mBluetoothGatt;

    private Handler mHandler;
    private Runnable mCircleRevealRunnable;

    private SimpleXYSeries mSeries;
    private XYPlot mPlot = null;
    private int time = 0;
    private int[] rssiArray;

    private boolean screenLockOn;
    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure BLE is available and switched on.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE on this device.", Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager bMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bMan.getAdapter() == null || !bMan.getAdapter().isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first.", Toast.LENGTH_LONG).show();
            finish();
        }

        // Initially set screen brightness lock.
        screenLockOn = true;
        screenLock(true);

        rssiArray = new int[10];

        mHandler = new Handler(Looper.getMainLooper());

        // Set up graph.
        mSeries = new SimpleXYSeries("BLE RSSI");
        mPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        mPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        mPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.argb(43,255,255,255));
        mPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.argb(43, 0, 0, 0));
        mPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.argb(43, 0, 0, 0));
        mPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        mPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
        mPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        mPlot.getGraphWidget().getRangeOriginLinePaint().setStrokeWidth(4);
        mPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        mPlot.getGraphWidget().getDomainOriginLinePaint().setStrokeWidth(4);
        mPlot.getGraphWidget().getCursorLabelBackgroundPaint().setColor(Color.GREEN);
        mPlot.getBorderPaint().setColor(Color.TRANSPARENT);
        mPlot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        mPlot.getLegendWidget().getTextPaint().setColor(Color.BLACK);
        mPlot.setDomainValueFormat(new DecimalFormat("0"));
        mPlot.setRangeValueFormat(new DecimalFormat("0"));
        mPlot.setTicksPerDomainLabel(3);
        mPlot.addSeries(mSeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
        mPlot.setRangeBoundaries(-120, -10, BoundaryMode.FIXED);

        // Get BLE name and address and set views accordingly.
        // Also use address to create device and connect.
        Intent intent = getIntent();
        String BLEAddress = intent.getStringExtra("mac");
        String BLEName = intent.getStringExtra("name");

        BluetoothDevice device = bMan.getAdapter().getRemoteDevice(BLEAddress);
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        TextView v = (TextView) findViewById(R.id.bleDeviceNameView);
        v.setText(BLEName + " - " + BLEAddress);

        // Nifty circle reveal animation.
        mCircleRevealRunnable = new Runnable() {
            @Override
            public void run() {
                View graphView = findViewById(R.id.mySimpleXYPlot);
                // get the center for the clipping circle
                int cx = (graphView.getLeft() + graphView.getRight()) / 2;
                int cy = (graphView.getTop() + graphView.getBottom()) / 2;

                // get the final radius for the clipping circle
                int finalRadius = Math.max(graphView.getWidth(), graphView.getHeight());

                // create the animator for this view (the start radius is zero)
                Animator anim = ViewAnimationUtils.createCircularReveal(graphView, cx, cy, 0, finalRadius);
                anim.setDuration(800);

                // make the view visible and start the animation
                graphView.setVisibility(View.VISIBLE);
                anim.start();
            }
        };

        // Set click listener for FAB.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab2);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove screen brightness lock and pending animation callbacks.
        screenLock(false);
        mHandler.removeCallbacks(mCircleRevealRunnable);

        // Disconnect BLE.
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            // Turn screen brightness lock on or off.
            if (screenLockOn) {
                Toast.makeText(this, "Screen brightness lock turned off", Toast.LENGTH_SHORT).show();
                screenLock(false);
                screenLockOn = false;
            } else {
                Toast.makeText(this, "Screen brightness lock turned on", Toast.LENGTH_SHORT).show();
                screenLock(true);
                screenLockOn = true;
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // GATT callback gets called on connection state change or RSSI update.
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        // Connection state changed.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(_TAG, "Connected to " + gatt.getDevice().getAddress());
                Log.i(_TAG, "Read remote RSSI.");
                gatt.readRemoteRssi();

                // Circle-Reveal graph once we're connected. ~2 seconds before data starts coming in.
                mHandler.postDelayed(mCircleRevealRunnable, 2200);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(_TAG, "Disconnected from " + gatt.getDevice().getAddress());
            }

        }

        // New RSSI received.
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i(_TAG, "RSSI=" + rssi);

            // Only update graph on every 10th RSSI read (average from 10 RSSIs).
            rssiArray[time % 10] = rssi;
            if (time % 10 == 0 && time != 0) {

                int avg = 0;
                for (int aRssiArray : rssiArray) {
                    avg += aRssiArray;
                }

                avg = avg / rssiArray.length;
                mSeries.addLast(time / 10, avg);
                mPlot.redraw();
            }


            // Sleep for 0.1 second => About 10 RSSIs per second.
            time++;
            mHandler.postDelayed(mStartRssiScanRunnable, 100);
        }

        final Runnable mStartRssiScanRunnable = new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.readRemoteRssi();
            }
        };

    };

    /**
     * Set or unset screen brightness lock (wake lock).
     * @param b whether to set or unset.
     */
    private void screenLock(boolean b) {
        if (b) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        } else {
            mWakeLock.release();
        }
    }

}
