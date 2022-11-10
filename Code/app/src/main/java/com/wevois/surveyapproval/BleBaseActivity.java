package com.wevois.surveyapproval;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.reader.ble.BU01_Reader;


public class BleBaseActivity extends AppCompatActivity {
    protected BU01_Reader reader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reader = ((BU01Application) getApplication()).getBleReader();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reader != null) {
            reader.setBtnCallback(new BU01_Reader.OnBtnCallback() {
                @Override
                public void onBtnPress() {
                    onReaderBtnPress();
                }

                @Override
                public void onBtnRelease() {
                    onReaderBtnRelease();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reader != null) {
            reader.setBtnCallback(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (reader != null){
            getMenuInflater().inflate(R.menu.ble_reader, menu);
            MenuItem connectItem = menu.findItem(R.id.connect);
            if (reader.isConnecting()) {
                connectItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                connectItem.setActionView(null);
            }
            if (reader.isConnected()) {
                connectItem.setTitle(getString(R.string.disconnect));
            } else {
                connectItem.setTitle(getString(R.string.connect));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.connect:
                if (item.getTitle().equals(getString(R.string.connect))) {
                    connect();
                } else {
                    disconnect();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void connect() {
        ActivityCollector.invalidateOptionsMenu();
        if (reader != null){
            reader.connect(BleBaseActivity.this, new BU01_Reader.Callback() {
                @Override
                public void onConnect() {
                    ActivityCollector.invalidateOptionsMenu();
                    ActivityCollector.onReaderConnect();
                }

                @Override
                public void onDisconnect() {
                    ActivityCollector.invalidateOptionsMenu();
                    ActivityCollector.onReaderDisconnect();

                    if (!isFinishing()) {
                        Toast.makeText(BleBaseActivity.this, getResources().getString(R.string.toast_err_disconnect), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    protected void disconnect() {
        if (reader != null){
            reader.disconnect();
        }
    }

    protected void showToast(String message){
        Toast.makeText(BleBaseActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    protected void onReaderConnect(){

    }

    protected void onReaderDisconnect(){

    }

    protected void onReaderBtnPress(){

    }

    protected void onReaderBtnRelease(){

    }
}
