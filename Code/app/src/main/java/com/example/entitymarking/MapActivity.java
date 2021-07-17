package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    String selectedWard = null, selectedCity, userId, date, cbText;
    double lat, lng;
    int currentLineNumber = 0,                      // use + 1 to get currentLine
            EMPLOYEE_DATEWISE_COUNT = 0,
            EMPLOYEE_DATEWISE_TOTAL_COUNT = 0,
            EMPLOYEE_TOTAL_COUNT = 0,
            WARD_DATEWISE_COUNT = 0,
            WARD_DATEWISE_TOTAL_COUNT = 0,
            WARD_TOTAL_COUNT = 0,
            MARKS_COUNT = 0;
    Spinner houseTypeSpinner;
    TextView currentLineTv, totalMarksTv;
    CheckBox alreadyInstalledCb;
    Bitmap photo;
    GoogleMap mMap;
    LocationCallback locationCallback;
    LatLng lastKnownLatLngForWalkingMan = null;
    Marker manMarker;
    DatabaseReference rootRef;
    JSONObject wardJSONObject;
    List<List<LatLng>> dbColl = new ArrayList<>();
    List<String> houseList = new ArrayList<>();
    HashMap<String, Integer> houseDataHashMap;
    CommonFunctions common = new CommonFunctions();
    boolean isPass = true, captureClickControl = true;
    private static final int MAIN_LOC_REQUEST = 5000;
    private static final int GPS_CODE_FOR_ENTITY = 501;
    private static final int FOCUS_AREA_SIZE = 300;
    private static final int PERMISSION_CODE = 1000;
    Bitmap thumbnail;
    private Camera mCamera;
    private SurfaceView surfaceView;
    Camera.PictureCallback pictureCallback;

    private final static String TAG = MapActivity.class.getSimpleName();

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        inIt();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @SuppressLint("ResourceType")
    private void inIt() {
        currentLineTv = findViewById(R.id.current_line_tv);
        rootRef = common.getDatabaseRef(this);
        houseTypeSpinner = findViewById(R.id.house_type_spinner);
        totalMarksTv = findViewById(R.id.total_marks_tv);
        alreadyInstalledCb = findViewById(R.id.already_installed_cb);
        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        SharedPreferences preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        selectedWard = getIntent().getStringExtra("ward");
        selectedCity = preferences.getString("storagePath", "");
        userId = preferences.getString("userId", "");
        cbText = preferences.getString("alreadyInstalledCheckBoxText", getResources().getString(R.string.already_installed_cb_text));
        alreadyInstalledCb.setText(cbText);
        if (selectedWard != null) {
            common.setProgressDialog("Please Wait", "", MapActivity.this, MapActivity.this);
            setPageTitle();
            runOnUiThread(this::fetchHouseTypes);
            fetchWardJson();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mainCheckLocationForRealTimeRequest();
    }

    private void setPageTitle() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView titleTv = toolbar.findViewById(R.id.toolbar_title);
        titleTv.setText("Ward: " + selectedWard);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> MapActivity.this.onBackPressed());
    }

    private void fetchMarkedSurveyData(Location finalLocation) {
        common.setProgressDialog("Saving", "", MapActivity.this, MapActivity.this);
        rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId + "/").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    EMPLOYEE_DATEWISE_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                }
                rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalCount/").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue() != null) {
                            EMPLOYEE_DATEWISE_TOTAL_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                        }
                        rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.getValue() != null) {
                                    EMPLOYEE_TOTAL_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                                }
                                rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard + "/").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.getValue() != null) {
                                            WARD_DATEWISE_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                                        }
                                        rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalCount/").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.getValue() != null) {
                                                    WARD_DATEWISE_TOTAL_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                                                }
                                                rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.getValue() != null) {
                                                            WARD_TOTAL_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                                                        }
                                                        Log.d(TAG, "onDataChange: "+WARD_TOTAL_COUNT+" "+WARD_DATEWISE_TOTAL_COUNT+" "+WARD_DATEWISE_COUNT+" "+EMPLOYEE_TOTAL_COUNT
                                                                +" "+EMPLOYEE_DATEWISE_TOTAL_COUNT+" "+EMPLOYEE_DATEWISE_COUNT);
                                                        saveMarkedLocationAndUploadPhoto(finalLocation);

                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void fetchHouseTypes() {
        rootRef.child("Defaults/FinalHousesType").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    houseList = new ArrayList<>();
                    houseDataHashMap = new HashMap<>();
                    houseList.add("Select House Type");
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.hasChild("name")) {
                            String[] tempStr = String.valueOf(dataSnapshot.child("name").getValue()).split("\\(");
                            houseDataHashMap.put(String.valueOf(tempStr[0]), Integer.parseInt(Objects.requireNonNull(dataSnapshot.getKey())));
                            houseList.add(tempStr[0]);
                        }
                    }
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(MapActivity.this, android.R.layout.simple_spinner_item, houseList) {
                        @Override
                        public boolean isEnabled(int position) {
                            return !(position == 0);
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                            View view = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) view;
                            tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                            return view;
                        }

                    };
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    houseTypeSpinner.setAdapter(spinnerArrayAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void fetchWardJson() {
        try {
            StorageReference stoRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity + "/WardJson").child(selectedWard + ".json");
            File localFile = File.createTempFile("images", "jpg");
            stoRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)));
                    StringBuilder sb = new StringBuilder();
                    String str;
                    while ((str = br.readLine()) != null) {
                        sb.append(str);
                    }
                    wardJSONObject = new JSONObject(sb.toString());
                    prepareDB();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }).addOnFailureListener(e -> {
                common.closeDialog(this);
                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setMessage("No Data Found").setCancelable(false)
                        .setPositiveButton("Ok", (dialog, id) -> {
                            MapActivity.this.onBackPressed();
                            dialog.cancel();
                        })
                        .setNegativeButton("", (dialog, i) -> {
                            MapActivity.this.onBackPressed();
                            dialog.cancel();
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareDB() {
        Iterator<String> keys = wardJSONObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (wardJSONObject.get(key) instanceof JSONObject) {
                    List<LatLng> tempList = new ArrayList<>();
                    JSONArray latLngPointJSONArray = wardJSONObject.getJSONObject(key).getJSONArray("points");
                    for (int a = 0; a < latLngPointJSONArray.length(); a++) {
                        try {
                            lat = latLngPointJSONArray.getJSONArray(a).getDouble(0);
                            lng = latLngPointJSONArray.getJSONArray(a).getDouble(1);
                            tempList.add(new LatLng(lat, lng));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    dbColl.add(tempList);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        common.closeDialog(this);
        drawLine();
        fetchMarkerForLine(false);
    }

    @SuppressLint("SetTextI18n")
    private void fetchMarkerForLine(boolean isCloseProgressDialog) {
        rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalMarksTv.setText("" + 0);
                if (snapshot.getValue() != null) {
                    if (snapshot.hasChild("marksCount")) {
                        totalMarksTv.setText(String.valueOf(snapshot.child("marksCount").getValue()));
                    }
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.hasChild("latLng")) {
                            String[] tempStr = String.valueOf(dataSnapshot.child("latLng").getValue()).split(",");
                            double lat = Double.parseDouble(tempStr[0]);
                            double lng = Double.parseDouble(tempStr[1]);
                            addMarkerForEntity(new LatLng(lat, lng));
                        }
                    }
                }
                if (isCloseProgressDialog) common.closeDialog(MapActivity.this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void mainCheckLocationForRealTimeRequest() {
        if (common.locationPermission(MapActivity.this)) {
            LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                    .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                    .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                try {
                    task1.getResult(ApiException.class);
                    if (task1.isSuccessful()) {
                        startCaptureLocForWalkingMan();
                    }
                } catch (ApiException e) {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapActivity.this, MAIN_LOC_REQUEST);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void checkGpsForEntity() {
        LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
                if (task1.isSuccessful()){
                    openCam();
                }
            } catch (ApiException e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapActivity.this, GPS_CODE_FOR_ENTITY);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void pickLocForEntity() {
        if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        Location finalLocation = locationResult.getLastLocation();
                        LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(this);
                        if (finalLocation != null) {
                            updateMarksCount(finalLocation);
                        } else {
                            common.closeDialog(MapActivity.this);
                            captureClickControl = true;
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                            builder.setMessage("Please Retry").setCancelable(false)
                                    .setPositiveButton("Retry", (dialog, id) -> {
                                        checkGpsForEntity();
                                        dialog.cancel();
                                    })
                                    .setNegativeButton("No", (dialog, i) -> finish());
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
                    }
                }, Looper.getMainLooper());
    }

    private void updateMarksCount(Location finalLocation) {
        rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("marksCount")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue() != null) {
                            MARKS_COUNT = Integer.parseInt(String.valueOf(snapshot.getValue()));
                            MARKS_COUNT++;
                        } else {
                            MARKS_COUNT = 1;
                        }
                        captureClickControl = true;
                        common.closeDialog(MapActivity.this);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                        builder.setMessage("Data Collected successfully.Proceed to Save.").setCancelable(false)
                                .setPositiveButton("Proceed", (dialog, id) -> {
                                    fetchMarkedSurveyData(finalLocation);
                                    dialog.cancel();
                                })
                                .setNegativeButton("Cancel", (dialog, i) -> dialog.cancel());
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        houseTypeSpinner.setEnabled(true);
                        alreadyInstalledCb.setEnabled(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @SuppressLint("SimpleDateFormat")
    private void saveMarkedLocationAndUploadPhoto(Location loc) {
        if (MARKS_COUNT != 0) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference stoRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity);
            ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 99, toUpload);
            stoRef.child("/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + date + "-" + MARKS_COUNT + ".jpg")
                    .putBytes(toUpload.toByteArray())
                    .addOnCompleteListener(uriMainTask -> {
                        if (uriMainTask.isSuccessful()) {
                            HashMap<String, Object> hM = new HashMap<>();
                            hM.put("latLng", loc.getLatitude() + "," + loc.getLongitude());
                            hM.put("userId", userId);
                            hM.put("alreadyInstalled", alreadyInstalledCb.isChecked());
                            hM.put("image", date + "-" + MARKS_COUNT + ".jpg");
                            hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                            hM.put("houseType", houseDataHashMap.get(houseTypeSpinner.getSelectedItem()));
                            rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT).setValue(hM)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/marksCount").setValue(MARKS_COUNT)
                                                    .addOnCompleteListener(task1 -> {
                                                        if (task1.isSuccessful()) {
                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId).setValue(++EMPLOYEE_DATEWISE_COUNT)
                                                                    .addOnCompleteListener(task2 -> {
                                                                        if (task2.isSuccessful()) {
                                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalCount").setValue(++EMPLOYEE_DATEWISE_TOTAL_COUNT)
                                                                                    .addOnCompleteListener(task3 -> {
                                                                                        if (task3.isSuccessful()) {
                                                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId).setValue(++EMPLOYEE_TOTAL_COUNT)
                                                                                                    .addOnCompleteListener(task31 -> {
                                                                                                        if (task31.isSuccessful()) {
                                                                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard).setValue(++WARD_DATEWISE_COUNT)
                                                                                                                    .addOnCompleteListener(task4 -> {
                                                                                                                        if (task4.isSuccessful()) {
                                                                                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalCount").setValue(++WARD_DATEWISE_TOTAL_COUNT)
                                                                                                                                    .addOnCompleteListener(task5 -> {
                                                                                                                                        if (task5.isSuccessful()) {
                                                                                                                                            rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard).setValue(++WARD_TOTAL_COUNT)
                                                                                                                                                    .addOnCompleteListener(task6 -> {
                                                                                                                                                        if (task6.isSuccessful()) {
                                                                                                                                                            houseTypeSpinner.setSelection(0);
                                                                                                                                                            totalMarksTv.setText("" + MARKS_COUNT);
                                                                                                                                                            MARKS_COUNT = 0;
                                                                                                                                                            alreadyInstalledCb.setChecked(false);
                                                                                                                                                            common.closeDialog(MapActivity.this);
                                                                                                                                                            addMarkerForEntity(new LatLng(loc.getLatitude(), loc.getLongitude()));
                                                                                                                                                        } else {
                                                                                                                                                            common.closeDialog(MapActivity.this);
                                                                                                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                                                                                                        }
                                                                                                                                                    });
                                                                                                                                        } else {
                                                                                                                                            common.closeDialog(MapActivity.this);
                                                                                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                                                                                        }

                                                                                                                                    });

                                                                                                                        } else {
                                                                                                                            common.closeDialog(MapActivity.this);
                                                                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                                                                        }

                                                                                                                    });
                                                                                                        } else {
                                                                                                            common.closeDialog(MapActivity.this);
                                                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                                                        }
                                                                                                    });
                                                                                        } else {
                                                                                            common.closeDialog(MapActivity.this);
                                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            common.closeDialog(MapActivity.this);
                                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                                        }
                                                                    });

                                                        } else {
                                                            common.closeDialog(MapActivity.this);
                                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                                        }
                                                    });
                                        } else {
                                            common.closeDialog(MapActivity.this);
                                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                        }
                                    });
                        } else {
                            common.closeDialog(MapActivity.this);
                            common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                        }
                    });


        } else {
            common.closeDialog(MapActivity.this);
            common.showAlertBox("please Retry", "Ok", "", MapActivity.this);
        }
    }

    private void startCaptureLocForWalkingMan() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                    LatLng currentLatLng = new LatLng(locationResult.getLocations().get(latestLocationIndex).getLatitude(), locationResult.getLocations().get(latestLocationIndex).getLongitude());
                    if (manMarker == null) {
                        addRealtimeMarker(currentLatLng);
                    } else {
                        manMarker.setPosition(currentLatLng);
                    }
                    lastKnownLatLngForWalkingMan = currentLatLng;

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(19));
                }
            }
        };
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void addRealtimeMarker(LatLng latLng) {
        manMarker = mMap.addMarker(new MarkerOptions().position(latLng).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.man)));
    }

    @SuppressLint("SetTextI18n")
    private void drawLine() {
        mMap.clear();
        currentLineTv.setText("" + (currentLineNumber + 1) + " / " + dbColl.size());
        mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(currentLineNumber))
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .color(0xff000000)
                .jointType(JointType.ROUND)
                .width(8));
        if (lastKnownLatLngForWalkingMan != null) {
            addRealtimeMarker(lastKnownLatLngForWalkingMan);
        }
    }

    private void openCam() {
        new Handler().post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (MapActivity.this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
                } else {
                    showAlertDialog();

                }
            }
        });
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                Rect rect = calculateFocusArea(event.getX(), event.getY());
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);
                mCamera.setParameters(parameters);
            }
            mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = (success, camera) -> {
        if (success) {
        } else {
        }
    };

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - focusAreaSize / 2;
            } else {
                result = -1000 + focusAreaSize / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize / 2;
        }
        return result;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void showAlertDialog() {
        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.custom_camera_alertbox, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        AlertDialog dialog = alertDialog.create();
        surfaceView = (SurfaceView) dialogLayout.findViewById(R.id.surfaceViews);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        SurfaceHolder.Callback surfaceViewCallBack = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera = Camera.open();
                    Camera.Parameters parameters = mCamera.getParameters();
                    List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                    parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
                    mCamera.setParameters(parameters);
                    setCameraDisplayOrientation(MapActivity.this, 0, mCamera);
                    mCamera.setPreviewDisplay(surfaceHolder);
                    mCamera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        };
        surfaceHolder.addCallback(surfaceViewCallBack);

        try {
            Button btn = dialogLayout.findViewById(R.id.capture_image_btn);
            btn.setOnClickListener(v -> {
                Log.d(TAG, "showAlertDialog: AAA");
                common.setProgressDialog("Please Wait", "", MapActivity.this, MapActivity.this);
                isPass = true;
                houseTypeSpinner.setEnabled(false);
                alreadyInstalledCb.setEnabled(false);
                if (captureClickControl) {
                    captureClickControl = false;
                    mCamera.takePicture(null, null, null, pictureCallback);
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                dialog.cancel();
                isPass = true;
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                thumbnail = Bitmap.createBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length),
                        0, 0, BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getWidth(),
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getHeight(), matrix, true);
                if (thumbnail != null) {
                    photo = thumbnail;
                    pickLocForEntity();
                } else {
                    common.closeDialog(MapActivity.this);
                    Toast.makeText(this, "Please Retry", Toast.LENGTH_SHORT).show();
                }

                camera.stopPreview();
                if (camera != null) {
                    camera.release();
                    mCamera = null;
                }
                dialog.cancel();
            };

            surfaceView.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    focusOnTouch(motionEvent);
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addMarkerForEntity(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
    }

    @SuppressLint("StaticFieldLeak")
    public void onNextClick(View view) {
        if ((currentLineNumber + 1) >= 0 && (currentLineNumber + 1) < dbColl.size()) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        currentLineNumber++;
                        drawLine();
                        fetchMarkerForLine(true);
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
        }

    }

    @SuppressLint("StaticFieldLeak")
    public void onSaveClick(View view) {
        if (houseTypeSpinner.getSelectedItemId() != 0) {
            if (isPass) {
                isPass = false;
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        return common.network(MapActivity.this);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        common.closeDialog(MapActivity.this);
                        if (result) {
                            checkGpsForEntity();
                        } else {
                            isPass = true;
                            common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                        }
                    }
                }.execute();

            }
        } else {
            common.showAlertBox("Please select house type", "ok", "", MapActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void onPrevClick(View view) {
        if ((currentLineNumber - 1) >= 0 && (currentLineNumber - 1) < dbColl.size()) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        currentLineNumber--;
                        drawLine();
                        fetchMarkerForLine(true);
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
        }

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "onPause: " + e.toString());
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(locationCallback);
        }
    }

    @Override
    @SuppressLint("StaticFieldLeak")
    public void onBackPressed() {
        super.onBackPressed();

        if (locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(locationCallback);
        }
        Intent intent = new Intent(MapActivity.this, SelectWardActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == 500 && grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }

            if (requestCode == PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    showAlertDialog();
                } else {
                    isPass = true;
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == GPS_CODE_FOR_ENTITY) {
                if (resultCode == RESULT_OK) {
                    openCam();
                } else {
                    isPass = true;
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == MAIN_LOC_REQUEST) {
                if (resultCode == RESULT_OK) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "openCam:C " + e.toString());

        }
    }

}