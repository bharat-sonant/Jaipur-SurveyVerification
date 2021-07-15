package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    SharedPreferences dbPathSP;
    CommonFunctions cmn = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbPathSP = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        checkInternet();
    }

    @SuppressLint("StaticFieldLeak")
    private void checkInternet(){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return cmn.network(MainActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    checkWhetherLocationSettingsAreSatisfied();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Please Connect to internet").setCancelable(false)
                            .setPositiveButton("Ok", (dialog, id) -> {
                                Toast.makeText(MainActivity.this, "Turn On Internet", Toast.LENGTH_SHORT).show();
                                finish();
                                dialog.cancel();
                            })
                            .setNegativeButton("", (dialog, i) -> {
                                dialog.cancel();
                                finish();
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        }.execute();
    }

    private void setDatabasePath(String city) {
        String dbPath, storagePath;
        if (city.equals("test")) {
            dbPath = "https://dtdnavigatortesting.firebaseio.com/";
            storagePath = "Test";
        } else if (city.equals("reengus")) {
            dbPath = "https://dtdreengus.firebaseio.com/";
            storagePath = "Reengus";
        } else {
            dbPath = "https://dtdnavigator.firebaseio.com/";
            storagePath = "Sikar";
        }

        dbPathSP.edit().putString("dbPath", dbPath).apply();
        dbPathSP.edit().putString("storagePath", storagePath).apply();
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        cmn.closeDialog(MainActivity.this);
        startActivity(i);
        finish();
    }

    private void checkWhetherLocationSettingsAreSatisfied() {
        if (cmn.locationPermission(MainActivity.this)) {
            LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                    .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                    .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                try {
                    task1.getResult(ApiException.class);
                    if (task1.isSuccessful()){
                        getLocation();
                    }
                } catch (ApiException e) {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this, 501);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        Location finalLocation = locationResult.getLastLocation();
                        LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);
                        if (finalLocation != null) {
                            try {
                                String address = String.valueOf(new Geocoder(MainActivity.this, Locale.getDefault())
                                        .getFromLocation(finalLocation.getLatitude(), finalLocation.getLongitude(), 5)
                                        .get(0)
                                        .getLocality());

                                Log.d("TAG", "onLocationResult: "+address);
                                if (address != null) {
                                    switch (address.toLowerCase()) {
                                        case "jaipur":
                                            setDatabasePath("jaipur");
                                            break;
                                        case "sikar":
                                            setDatabasePath("sikar");
                                            break;
                                        case "reengus":
                                            setDatabasePath("reengus");
                                            break;
                                        default:
                                            cmn.showAlertBox("Please Restart Application","Ok","",MainActivity.this);
                                            break;
                                    }

                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("Please Retry").setCancelable(false)
                                            .setPositiveButton("Retry", (dialog, id) -> {
                                                checkWhetherLocationSettingsAreSatisfied();
                                                dialog.cancel();
                                            })
                                            .setNegativeButton("No", (dialog, i) -> finish());
                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 500 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MainActivity.this.checkWhetherLocationSettingsAreSatisfied();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 501) {
            if (resultCode == RESULT_OK) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }

    }
}