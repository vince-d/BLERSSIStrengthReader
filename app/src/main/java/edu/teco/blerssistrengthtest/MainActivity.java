package edu.teco.blerssistrengthtest;

import android.animation.Animator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends Activity {

    BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean mDownloading = false;
    private boolean mEnable;
    private Button mStartButton;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice;
    private ProgressDialog mProgressDialog;

    private String mBLEAddress;

    private SimpleXYSeries mSeries;
    private SimpleXYSeries mDownloadSeries;

    private XYPlot mPlot = null;

    private int time = 0;

    private int[] rssiArray;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);


            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("RSSI", "Connected to " + gatt.getDevice().getAddress());
                Log.i("RSSI", "Read remote RSSI.");
                gatt.readRemoteRssi();

                // Circle-Reveal graph once we're connected.
                mHandler.postDelayed(new Runnable() {
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

                        // make the view visible and start the animation
                        graphView.setVisibility(View.VISIBLE);
                        anim.start();
                    }
                }, 3100);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("RSSI", "Disconnected from " + gatt.getDevice().getAddress());
            }

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i("RSSI", "RSSI=" + rssi);



            int bla = time % 10;
            rssiArray[bla] = rssi;
            int bla2 = time / 10;

            if (bla == 0 && time != 0) {

                int avg = 0;
                for (int i = 0; i < rssiArray.length; i++) {
                    avg += rssiArray[i];
                }
                avg = avg / rssiArray.length;

                mSeries.addLast(bla2, avg);

                if (mDownloading) {
                    mDownloadSeries.addLast(bla2, avg);
                }

                mPlot.redraw();
            }

            time++;

            // Read RSSI
            mHandler.postDelayed(mStartRssiScanRunnable, 100);
        }

        final Runnable mStartRssiScanRunnable = new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.readRemoteRssi();
            }
        };

    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE on this device.", Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager bMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bMan.getAdapter() == null || !bMan.getAdapter().isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first.", Toast.LENGTH_LONG).show();
            finish();
        }

        mBluetoothAdapter = bMan.getAdapter();

        mEnable = true;
        //mStartButton = (Button) findViewById(R.id.startButton);

        mHandler = new Handler(Looper.getMainLooper());

        // Download stuff.
        
        // instantiate it within the onCreate method
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Now downloading...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        mPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        mSeries = new SimpleXYSeries("Only BLE");
        mDownloadSeries = new SimpleXYSeries("BLE+WiFi");
        mPlot.addSeries(mSeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
        mPlot.addSeries(mDownloadSeries, new LineAndPointFormatter(Color.rgb(20, 20, 20), null, null, null));
        //mPlot.setDomainLeftMax(0);
        //mPlot.setDomainLeftMax(-120);
        mPlot.setRangeBoundaries(-120, 0, BoundaryMode.FIXED);

        rssiArray = new int[10];

        Intent intent = getIntent();
        mBLEAddress = intent.getStringExtra("mac");

        mDevice = mBluetoothAdapter.getRemoteDevice(mBLEAddress);
        mBluetoothGatt = mDevice.connectGatt(this, false, mGattCallback);



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }


    public void downloadButtonClicked(View view) {

        // execute this when the downloader must be fired
        final DownloadTask downloadTask = new DownloadTask(this);
        downloadTask.execute("http://www.mirror.internode.on.net/pub/test/100meg.test");

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
                mDownloading = false;
            }
        });

    }


    // usually, subclasses of AsyncTask are declared inside the activity class.
// that way, you can easily modify the UI thread from here
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mDownloading = false;
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            mDownloading = true;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/dev/null/");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }
}
