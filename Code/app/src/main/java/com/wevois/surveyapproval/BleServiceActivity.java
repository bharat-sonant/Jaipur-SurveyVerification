package com.wevois.surveyapproval;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.reader.ble.BU01_Factory;
import com.reader.ble.BU01_Reader;
import com.reader.ble.BU01_Service;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BleServiceActivity extends AppCompatActivity {
    private static final int PERMISSION_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private static final String SHARED_PREFERENCES_FILE = "data";
    private static final String SHARED_PREFERENCES_KEY = "mac";

    private BU01_Service service;
    private Adapter adapter;
    private Handler handler = new Handler();
    private Runnable stopScanRunnable = this::stopScan;
    private Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
            handler.postDelayed(this, 500);
        }
    };
    private BluetoothReceiver bluetoothReceiver;

    @BindView(R.id.swipe)
    SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        initView();
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
        checkBleSwitch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_last_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.last_device:
                String address = getMac();
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                if (TextUtils.isEmpty(address)) {
                    dialog.setMessage(getResources().getString(R.string.text_dialog_no_device));
                    dialog.setPositiveButton(getResources().getString(R.string.text_dialog_btn_confirm), null);
                } else {
                    dialog.setMessage(String.format(getResources().getString(R.string.text_dialog_last_device_mac), address));
                    dialog.setPositiveButton(getResources().getString(R.string.text_dialog_btn_confirm), (dialog1, which) -> {
                        dialog1.dismiss();
                        try {
                            BU01_Reader reader = BU01_Factory.bu01ByAddress(BleServiceActivity.this, address);
                            ((BU01Application) getApplication()).setBleReader(reader);
                            startActivity(new Intent(BleServiceActivity.this, MapActivity.class));
                        } catch (Exception e) {
                            Toast.makeText(BleServiceActivity.this,
                                    getResources().getString(R.string.toast_err_connect_failed), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    });
                    dialog.setNegativeButton(getResources().getString(R.string.text_dialog_btn_cancel), null);
                }
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_COARSE_LOCATION:
                if (grantResults.length > 0) {
                    boolean reject = false;
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            reject = true;
                            break;
                        }
                    }
                    if (reject) {
                        Toast.makeText(BleServiceActivity.this,
                                getResources().getString(R.string.toast_err_location_permission_denied), Toast.LENGTH_SHORT).show();
                    } else {
                        scanForBleReader();
                    }
                } else {
                    Toast.makeText(BleServiceActivity.this,
                            getResources().getString(R.string.toast_err_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        toolbar.setTitle(String.format("%s-v%s", getResources().getString(R.string.app_name), versionName));
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter((adapter = new Adapter()));
        list.getItemAnimator().setAddDuration(0);
        list.getItemAnimator().setChangeDuration(0);
        list.getItemAnimator().setMoveDuration(0);
        list.getItemAnimator().setRemoveDuration(0);

        adapter.setOnItemClickListener(reader -> {
            ((BU01Application) getApplication()).setBleReader(reader);
            saveMac(reader.getAddress());
            startActivity(new Intent(this, MapActivity.class));
        });

        swipe.setColorSchemeResources(R.color.colorAccent);
        swipe.setOnRefreshListener(this::scanForBleReader);
    }

    private void checkBleSwitch() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            BluetoothAdapter bluetoothAdapter = manager.getAdapter();
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    if (checkPermission())
                        bluetoothAdapter.enable();
                } else {
                    initService();
                }
            }
        }
    }

    private boolean isGPSOpen() {
        boolean isOpen = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {


            isOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return isOpen;
    }

    private boolean checkPermission() {


        boolean hasPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED || (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)) {
                hasPermission = false;
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_COARSE_LOCATION);
            }
        }
        return hasPermission;
    }

    private void initService() {
        try {
            service = BU01_Factory.bu01(BleServiceActivity.this, reader -> adapter.addReader(reader));
        } catch (Exception e) {
            Toast.makeText(BleServiceActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        scanForBleReader();
    }

    private void scanForBleReader() {
        if (!checkPermission()) {
            setRefreshing(false);
            return;
        }
        if (service == null) {
            setRefreshing(false);
            checkBleSwitch();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isGPSOpen()) {
                setRefreshing(false);
                CustomDialog.showTurnOnGPS(BleServiceActivity.this);
                return;
            }
        }
        setRefreshing(true);
        adapter.clearReaders();
        service.scanForBU01BleReader();
        handler.postDelayed(stopScanRunnable, 20000);
        handler.postDelayed(notifyRunnable, 500);
    }

    private void stopScan() {
        if (service != null) {
            service.stopScan();
        }
        handler.removeCallbacks(stopScanRunnable);
        handler.removeCallbacks(notifyRunnable);
        setRefreshing(false);
    }

    private void saveMac(String address) {
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE).edit();
        editor.putString(SHARED_PREFERENCES_KEY, address);
        editor.apply();
    }

    private String getMac() {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE);
        return preferences.getString(SHARED_PREFERENCES_KEY, "");
    }

    private void setRefreshing(boolean refreshing) {
        if (refreshing && !swipe.isRefreshing()) {
            swipe.setRefreshing(true);
        } else if (!refreshing && swipe.isRefreshing()) {
            swipe.setRefreshing(false);
        }
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private List<BU01_Reader> readers = new ArrayList<>();
        private OnItemClickListener listener;

        void addReader(BU01_Reader reader) {
            if (readers.contains(reader)) {
                readers.set(readers.indexOf(reader), reader);
            } else {
                readers.add(reader);
            }
//            Collections.sort(readers, (o1, o2) -> o2.getRssi() - o1.getRssi());
        }

        void clearReaders() {
            readers.clear();
            notifyDataSetChanged();
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BU01_Reader reader = readers.get(position);
            holder.name.setText(reader.getName());
            holder.mac.setText(reader.getAddress());
            holder.rssi.setText(reader.getRssi() + "dB");
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(readers.get(holder.getAdapterPosition()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return readers.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.name)
            TextView name;
            @BindView(R.id.mac)
            TextView mac;
            @BindView(R.id.rssi)
            TextView rssi;

            ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

        }

        interface OnItemClickListener {
            void onItemClick(BU01_Reader reader);
        }

    }

    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        initService();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        service = null;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
