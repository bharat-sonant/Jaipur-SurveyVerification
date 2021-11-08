package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    SharedPreferences dbPathSP;
    String userId, assignedWard;
    DatabaseReference rootRef;
    CommonFunctions cmn = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbPathSP = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        checkAlreadyLoggedIn();
    }

    private void checkAlreadyLoggedIn() {
        userId = dbPathSP.getString("userId", null);
        if (userId != null) {
            if (!userId.equals("0")) {
                checkInternet();
                return;
            }
        }
        checkWhetherLocationSettingsAreSatisfied();
    }

    @SuppressLint("StaticFieldLeak")
    private void checkInternet() {
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
                    checkIsActive();

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
        switch (city) {
            case "test":
                dbPath = "https://dtdnavigatortesting.firebaseio.com/";
                storagePath = "Test";
                break;
            case "reengus":
                dbPath = "https://dtdreengus.firebaseio.com/";
                storagePath = "Reengus";
                break;
            case "shahpura":
                dbPath = "https://dtdshahpura.firebaseio.com/";
                storagePath = "Shahpura";
                break;
            default:
                dbPath = "https://dtdnavigator.firebaseio.com/";
                storagePath = "Sikar";
                break;
        }
        dbPathSP.edit().putString("dbPath", dbPath).apply();
        dbPathSP.edit().putString("storagePath", storagePath).apply();
        loginIntent();
    }

    private void checkWhetherLocationSettingsAreSatisfied() {
        if (cmn.locationPermission(MainActivity.this)) {
            LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                    .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                    .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                try {
                    task1.getResult(ApiException.class);
                    if (task1.isSuccessful()) {
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
                                        case "shahpura":
                                            setDatabasePath("shahpura");
                                            break;
                                        default:
                                            cmn.showAlertBox("Please Restart Application", "Ok", "", MainActivity.this);
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
                finish();
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
                finish();
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkIsActive() {
        try {
            rootRef = cmn.getDatabaseRef(this);
            rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/isActive/")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                if (Boolean.parseBoolean(String.valueOf(snapshot.getValue()))) {
                                    checkAssignedWard();
                                    return;
                                }
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Access Denied").setCancelable(false)
                                    .setPositiveButton("ok", (dialog, id) -> {
                                        finish();
                                        dialog.cancel();
                                    })
                                    .setNegativeButton("", (dialog, i) -> finish());
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();


                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAssignedWard() {
        try {
            rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard/")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                assignedWard = dbPathSP.getString("assignment", "null");
                                if (assignedWard != null) {
                                    if (!assignedWard.equals(String.valueOf(snapshot.getValue()))) {
                                        dbPathSP.edit().putString("assignment", String.valueOf(snapshot.getValue())).apply();
                                    }
                                    mapIntent();
                                }
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("No work assigned").setCancelable(false)
                                        .setPositiveButton("ok", (dialog, id) -> {
                                            finish();
                                            dialog.cancel();
                                        })
                                        .setNegativeButton("", (dialog, i) -> finish());
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void loginIntent() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void mapIntent() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
        finish();
    }
}