package com.wevois.surveyapproval;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.reader.ble.impl.EpcReply;
import com.wevois.surveyapproval.repository.Repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapActivity extends BleBaseActivity implements OnMapReadyCallback {

    String selectedWard = null, selectedCity, userId, date, cbText;
    ImageView imageViewForRejectedMarker;
    int currentLineNumber = 0;                      // use + 1 to get currentLine;
    Spinner houseTypeSpinner;
    TextView currentLineTv, totalMarksTv, titleTv, rgHeadingTv, dateTimeTv;
    RadioButton isSurveyedTrue, isSurveyedFalse;
    Bitmap photo;
    GoogleMap mMap;
    LocationCallback locationCallback;
    LatLng lastKnownLatLngForWalkingMan = null;
    DatabaseReference rootRef;
    SharedPreferences preferences;
    List<List<LatLng>> dbColl = new ArrayList<>();
    List<String> houseList;
    HashMap<String, Integer> houseDataHashMap;
    CommonFunctions common = new CommonFunctions();
    CountDownTimer cdTimer;
    private Camera mCamera;
    private SurfaceView surfaceView;
    Camera.PictureCallback pictureCallback;
    ChildEventListener cELOnLine;
    ValueEventListener cELForAssignedWard;
    AlertDialog dialogForModification;
    HashMap<LatLng, MarkersDataModel> mDMMap = new HashMap<>();
    boolean isPass = true,
            captureClickControl = true,
            boolToInstantiateMovingMarker = true,
            enableZoom = true,
            isEdit = false;
    private static final int MAIN_LOC_REQUEST = 5000,
            GPS_CODE_FOR_ENTITY = 501,
            GPS_CODE_FOR_MODIFICATION = 7777,
            FOCUS_AREA_SIZE = 300,
            PERMISSION_CODE = 1000;
    private Handler mHandler;
    private String sn = "";
    private boolean inventorying = false;
    private boolean btnInventorying = false;
    JSONObject scanDataObject;
    LinearLayout bottomLnyrLayout;

    private Runnable inventoryRunnable = new Runnable() {
        @Override
        public void run() {
            reader.singleInventory((int status, List<EpcReply> list) -> {
                if (status == 0) {
                    for (EpcReply epcReply : list) {
                        Log.e("EPC Reply ", ByteUtils.epcBytes2Hex(epcReply.getEpc()));
                        inventorying = true;
                        try {
                            String rfid = ByteUtils.epcBytes2Hex(epcReply.getEpc());
                            if (scanDataObject.has(rfid)) {
                                JSONArray jsonArray = scanDataObject.getJSONArray(rfid);
                                String serialNo = jsonArray.get(0).toString();
                                Log.e("SerialNo", serialNo);
                                getHouseLineDetails(serialNo);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
//                    final int total = mEPCAdapter.getTotal();
                    runOnUiThread(() -> {
//                        tv_total.setText(String.valueOf(total));
                    });
                } else {
                    mHandler.removeCallbacks(inventoryRunnable);
                    inventorying = false;
                    btnInventorying = false;
//                    btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
                    if (reader.isConnected()) {
//                        updateControls(true);
//                        btnRead.setEnabled(true);
                    }
                    if (status == -205) {
                        //inventory tag or other tag operation maybe return -205
                        CustomDialog.showLowPower(MapActivity.this);
                    } else {
                        showToast(String.format(getResources().getString(R.string.toast_err_start_inventory), status));
                    }
                }
                if (inventorying) {
                    mHandler.postDelayed(inventoryRunnable, 400);
                }
            });
        }
    };

    private Runnable btnInventoryRunnable = new Runnable() {
        @Override
        public void run() {
            reader.singleInventory((status, list) -> {
                if (status == 0) {
                    for (EpcReply epcReply : list) {
//                        mEPCAdapter.addEpcRecord(ByteUtils.epcBytes2Hex(epcReply.getEpc()), epcReply.getRssi());
                    }
//                    final int total = mEPCAdapter.getTotal();
                    runOnUiThread(() -> {
//                        tv_total.setText(String.valueOf(total));
                    });
                } else {
                    mHandler.removeCallbacks(btnInventoryRunnable);
                    inventorying = false;
                    btnInventorying = false;
//                    btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
                    if (reader.isConnected()) {
//                        updateControls(true);
//                        btnRead.setEnabled(true);
                    }
                    if (status == -205) {
                        //inventory tag or other tag operation maybe return -205
                        CustomDialog.showLowPower(MapActivity.this);
                    } else {
                        showToast(String.format(getResources().getString(R.string.toast_err_start_inventory), status));
                    }
                }
                if (btnInventorying) {
                    mHandler.postDelayed(btnInventoryRunnable, 200);
                }
            });
        }
    };

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_map);
            inIt();
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getHouseLineDetails(String SerialNo) {
//        preferences.edit().putString(SerialNo,"cardNo").apply();
        common.setProgressDialog("Please Wait..", "",this, this);
        rootRef.child("CardWardMapping/"+SerialNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.hasChild("line")) {
                        Log.e("Line No",dataSnapshot.child("line").getValue().toString());
                        String lineno = dataSnapshot.child("line").getValue().toString();
                        String wardno = dataSnapshot.child("ward").getValue().toString();
                        getHouseDetails(lineno,wardno,SerialNo);
                    }
                }else {
                    common.closeDialog(MapActivity.this);
//                    preferences.edit().putString(SerialNo,"cardNo").apply();
                    preferences.edit().putString("cardNo", "" + SerialNo).commit();
                    Log.e("Seriallllll",preferences.getString("cardNo",""));
                    Intent intent = new Intent(MapActivity.this,FormPageActivity.class);
                    intent.putExtra("from", "map");
                    startActivity(intent);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getHouseDetails(String line,String ward, String SerialNo) {
        common.setProgressDialog("Please Wait..", "",this, this);
        rootRef.child("Houses/"+ward+"/"+line+"/"+SerialNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.hasChild("name")) {
                        common.closeDialog(MapActivity.this);
                        Log.e("Name",dataSnapshot.child("name").getValue().toString());
                        String name = dataSnapshot.child("name").getValue().toString();
                        String address = dataSnapshot.child("address").getValue().toString();
                        String mobile = dataSnapshot.child("mobile").getValue().toString();
                        String type = dataSnapshot.child("cardType").getValue().toString();
                        String htype = dataSnapshot.child("houseType").getValue().toString();
                        String ward = dataSnapshot.child("ward").getValue().toString();
                        Intent intent = new Intent(MapActivity.this, HouseDetailActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("name",name);
                        intent.putExtra("address",address);
                        intent.putExtra("mobile",mobile);
                        intent.putExtra("userid",userId);
                        intent.putExtra("type",type);
                        intent.putExtra("htype",htype);
                        intent.putExtra("ward",ward);
                        intent.putExtra("line",line);
                        intent.putExtra("serail",SerialNo);
                        startActivity(intent);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint({"ResourceType", "SimpleDateFormat"})
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void inIt() {
        currentLineTv = findViewById(R.id.current_line_tv);
        rootRef = common.getDatabaseRef(this);
        houseTypeSpinner = findViewById(R.id.house_type_spinner);
        totalMarksTv = findViewById(R.id.total_marks_tv);
        rgHeadingTv = findViewById(R.id.radio_group_heading_tv);
        isSurveyedTrue = findViewById(R.id.is_surveyed_true_rb);
        isSurveyedFalse = findViewById(R.id.is_surveyed_false_rb);
        dateTimeTv = findViewById(R.id.date_and_time_tv);
        bottomLnyrLayout = findViewById(R.id.bottomLayout);
        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        new Repository().storageFileDownload(MapActivity.this);
        selectedWard = preferences.getString("assignment", null);
        selectedCity = preferences.getString("storagePath", "");
        userId = preferences.getString("userId", "");
        cbText = preferences.getString("alreadyInstalledCbHeading", getResources().getString(R.string.already_installed_cb_text));
        rgHeadingTv.setText(cbText);
        setRB();
        if (selectedWard != null) {
            common.setProgressDialog("Please Wait", "", MapActivity.this, MapActivity.this);
            setPageTitle();
            fHouseTypeFromSto();
            fetchWardJson();
            assignedWardCEL();
            lastScanTimeVEL();
            checkVersionForTheApplication();
        }
        mHandler = new Handler(Looper.getMainLooper());
        findViewById(R.id.dataScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomLnyrLayout.getVisibility() == View.VISIBLE) {
                    bottomLnyrLayout.setVisibility(View.GONE);
                }else {
                    bottomLnyrLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        getFileDownload();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void getFileDownload() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(preferences.getString("storagePath", "") + "/CardScanData/CardScanData" + ".json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
            try {
                String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                /*File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WardJson/" +
                        storagePath + "/" + wardNo);
                if (!root.exists()) {
                    root.mkdirs();
                }
                File wardFile = new File(root, dates + ".json");
                if (wardFile.exists()) {
                    wardFile.delete();
                }
                FileWriter writer = new FileWriter(wardFile, true);
                writer.append(str);
                writer.flush();
                writer.close();
                Log.e("setListeners", "Set Listener");
                readJsonFile();
                Log.e("setListeners", "Set Listener");*/
                preferences.edit().putString("CardScanData.json", str).commit();
                String fileName = "CardScanData.json";
                Log.e("file name", fileName);
                readJsonFile(fileName);
//                setListeners();
                common.closeDialog(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void readJsonFile(String file) {
        try {
            /*File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WardJson/" +
                    storagePath + "/" + preferences.getString("wardNo", "") + "/" + preferences.getString("commonReferenceDate", "") + ".json");
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder result = new StringBuilder();
            String str;
            while ((str = br.readLine()) != null) {
                result.append(str);
            }*/

            Log.e("json file", String.valueOf(preferences.getString(file, "")));
            scanDataObject = new JSONObject(String.valueOf(preferences.getString(file, "")));

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onReaderConnect() {
        super.onReaderConnect();
        reader.getSerialNumber((status, serialNumber) -> {
            if (status == 0) {
                sn = String.format(Locale.getDefault(), "%010d", serialNumber);
                ActionBar actionBar = getSupportActionBar();
                inventorying = true;
                onInventoryAction();
                if (actionBar != null) {
                    actionBar.setTitle(sn);
                }
            }
//            updateControls(true);
//            btnRead.setEnabled(true);
        });
    }

    @Override
    protected void onReaderDisconnect() {
        super.onReaderDisconnect();
//        updateControls(false);
//        btnRead.setEnabled(false);
//        btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
    }

    @Override
    protected void onReaderBtnPress() {
        super.onReaderBtnPress();
        Log.e("onReaderBtnPress", "onReaderBtnPress");
        if (inventorying) {
            mHandler.removeCallbacks(inventoryRunnable);
            inventorying = false;
        }
//        btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
//        updateControls(false);
//        btnRead.setEnabled(false);
        mHandler.postDelayed(btnInventoryRunnable, 200);
        btnInventorying = true;
    }

    @Override
    protected void onReaderBtnRelease() {
        super.onReaderBtnRelease();
        mHandler.removeCallbacks(btnInventoryRunnable);
        btnInventorying = false;
//        btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
//        updateControls(true);
//        btnRead.setEnabled(true);
    }

    private void setRB() {
        isSurveyedFalse.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(true);
            isSurveyedTrue.setChecked(false);
        });
        isSurveyedTrue.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(false);
            isSurveyedTrue.setChecked(true);
        });
    }

    private void setBothRBUnchecked() {
        isSurveyedFalse.setChecked(false);
        isSurveyedTrue.setChecked(false);
    }

    private boolean checkWhichRBisChecked() {
        return isSurveyedTrue.isChecked();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(marker -> {
            try {
                if (mDMMap.containsKey(marker.getPosition())) {
                    MarkersDataModel mDM = mDMMap.get(marker.getPosition());
                    assert mDM != null;
                    if (mDM.isStatus()) {
                        dialogForRejectedMarker(mDM, marker);
                        return false;
                    }
                }
                Toast.makeText(MapActivity.this, "Card Not Rejected", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        mainCheckLocationForRealTimeRequest();
    }

    @SuppressLint("SetTextI18n")
    private void setPageTitle() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        titleTv = toolbar.findViewById(R.id.toolbar_title);
        titleTv.setText("Ward: " + selectedWard);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> MapActivity.this.onBackPressed());
    }

    private void fHouseTypeFromSto() {
        common.getDatabaseStoragePath(MapActivity.this).child("/Defaults/FinalHousesType.json")
                .getMetadata().addOnSuccessListener(storageMetadata -> {
                    long serverUpdation = storageMetadata.getCreationTimeMillis();
                    long localUpdation = common.getDatabaseSp(MapActivity.this).getLong("houseTypeLastUpdate", 0);
                    if (serverUpdation != localUpdation) {
                        common.getDatabaseSp(MapActivity.this).edit().putLong("houseTypeLastUpdate", serverUpdation).apply();
                        try {
                            File local = File.createTempFile("temp", "txt");
                            common.getDatabaseStoragePath(MapActivity.this)
                                    .child("/Defaults/FinalHousesType.json")
                                    .getFile(local).addOnCompleteListener(task -> {
                                        try {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(local)));
                                            StringBuilder sb = new StringBuilder();
                                            String str;
                                            while ((str = br.readLine()) != null) {
                                                sb.append(str);
                                            }
                                            common.getDatabaseSp(MapActivity.this).edit().putString("houseType", sb.toString().trim()).apply();
                                            parseSpinnerData();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {
                        parseSpinnerData();
                    }
                });
    }

    private void parseSpinnerData() {
        try {
            JSONArray arr = new JSONArray(common.getDatabaseSp(MapActivity.this).getString("houseType", ""));
            houseList = new ArrayList<>();
            houseDataHashMap = new HashMap<>();
            houseList.add("Select House Type");
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.get(i).toString().equalsIgnoreCase("null")) {
                    JSONObject o = arr.getJSONObject(i);
                    String[] tempStr = String.valueOf(o.get("name")).split("\\(");
                    houseDataHashMap.put(String.valueOf(tempStr[0]), i);
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
            houseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (houseTypeSpinner.getSelectedItemId() != 0) {
                        onSaveClick(view);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fetchWardJson() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + selectedWard + "/mapUpdateHistoryJson.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong(preferences.getString("storagePath", "") + "" + selectedWard + "mapUpdateHistoryJsonDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + selectedWard + "/mapUpdateHistoryJson.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        preferences.edit().putString(preferences.getString("storagePath", "") + selectedWard + "mapUpdateHistoryJson", str).apply();
                        preferences.edit().putLong(preferences.getString("storagePath", "") + "" + selectedWard + "mapUpdateHistoryJsonDownloadTime", fileCreationTime).apply();
                        checkDate(selectedWard);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                checkDate(selectedWard);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void checkDate(String wardNo) {
        try {
            JSONArray jsonArray = new JSONArray(preferences.getString(preferences.getString("storagePath", "") + wardNo + "mapUpdateHistoryJson", ""));
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date1 = format.parse(format.format(new Date()));
                    Date date2 = format.parse(jsonArray.getString(i));
                    if (date1.after(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)), wardNo);
                        break;
                    } else if (date1.equals(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)), wardNo);
                        break;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fileMetaDownload(String dates, String wardNo) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + wardNo + "/" + dates + ".json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong(preferences.getString("storagePath", "") + wardNo + dates + "DownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + wardNo + "/" + dates + ".json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        common.getDatabaseSp(MapActivity.this).edit().putString("wardJSON", str).apply();
                        preferences.edit().putLong(preferences.getString("storagePath", "") + wardNo + dates + "DownloadTime", fileCreationTime).apply();
                        prepareDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).addOnFailureListener(Ex -> {
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
            } else {
                prepareDB();
            }
        });
    }

    private void prepareDB() {
        try {
            JSONObject wardJSONObject = new JSONObject(common.getDatabaseSp(MapActivity.this).getString("wardJSON", ""));
            Iterator<String> keys = wardJSONObject.keys();
            dbColl = new ArrayList<>();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (wardJSONObject.get(key) instanceof JSONObject) {
                        List<LatLng> tempList = new ArrayList<>();
                        JSONArray latLngPointJSONArray = wardJSONObject.getJSONObject(key).getJSONArray("points");
                        for (int a = 0; a < latLngPointJSONArray.length(); a++) {
                            try {
                                double lat = latLngPointJSONArray.getJSONArray(a).getDouble(0);
                                double lng = latLngPointJSONArray.getJSONArray(a).getDouble(1);
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
            celForLine();
            drawLine();
            fetchMarkerForLine(false);
            common.closeDialog(this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void celForLine() {
        cELOnLine = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.getValue() != null) {
                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                    }
                    if (snapshot.hasChild("latLng")) {
                        String[] tempStr = String.valueOf(snapshot.child("latLng").getValue()).split(",");
                        double lat = Double.parseDouble(tempStr[0]);
                        double lng = Double.parseDouble(tempStr[1]);

                        if (isRejectedMarker(snapshot)) {
                            mDMMap.put(new LatLng(lat, lng), new MarkersDataModel((Boolean) snapshot.child("alreadyInstalled").getValue(),
                                    (Boolean) isRejectedMarker(snapshot),
                                    String.valueOf(snapshot.child("date").getValue()),
                                    (String) snapshot.child("image").getValue(),
                                    Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
                                    Integer.parseInt(String.valueOf(snapshot.getKey()))));

//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
                        } else {
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
                        }
                    }
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (Objects.equals(snapshot.getKey(), "marksCount")) {
                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
    }

    private boolean isRejectedMarker(DataSnapshot snapshot) {
        if (snapshot.hasChild("status")) {
            return String.valueOf(snapshot.child("status").getValue()).equals("Reject");
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    private void fetchMarkerForLine(boolean isCloseProgressDialog) {
        totalMarksTv.setText("" + 0);
        mDMMap = new HashMap<>();
        enableZoom = true;
        rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).addChildEventListener(cELOnLine);
        if (isCloseProgressDialog) {
            if (cELOnLine != null) {
                rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber)).removeEventListener(cELOnLine);
            }
            common.closeDialog(MapActivity.this);
        }
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
                if (task1.isSuccessful()) {
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
        common.closeDialog(MapActivity.this);
        if (lastKnownLatLngForWalkingMan != null) {
            updateMarksCount();
        } else {
            common.showAlertBox("Please Refresh Location", "Ok", "", MapActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void updateMarksCount() {
        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.custom_image_preview, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        AlertDialog dialog = alertDialog.create();

        ImageView imageView = dialogLayout.findViewById(R.id.image_preview);
        imageView.setImageBitmap(photo);

        Button btn = dialogLayout.findViewById(R.id.proceed_preview_image_btn);
        btn.setOnClickListener(v -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("", "checking internet", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    common.closeDialog(MapActivity.this);
                    if (result) {
                        try {
                            saveMarkedLocationAndUploadPhoto();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        isPass = true;
                        houseTypeSpinner.setSelection(0);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
            dialog.dismiss();

        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_preview_image_btn);
        closeBtn.setOnClickListener(v -> {
            houseTypeSpinner.setSelection(0);
            setBothRBUnchecked();
            dialog.dismiss();
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        captureClickControl = true;
        houseTypeSpinner.setEnabled(true);
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void saveMarkedLocationAndUploadPhoto() {
        if (photo != null) {
            common.closeDialog(MapActivity.this);
            common.setProgressDialog("", "Saving data", MapActivity.this, MapActivity.this);
            rootRef.child("SurveyVerifierData/MarkedHousesByVerifier/" + selectedWard + "/" + (currentLineNumber + 1)).child("lastMarkerKey")
                    .runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            if (currentData.getValue() == null) {
                                currentData.setValue(1);
                            } else {
                                currentData.setValue(String.valueOf((Integer.parseInt(currentData.getValue().toString()) + 1)));
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            if (error == null) {
                                try {
                                    assert currentData != null;
                                    int MARKS_COUNT = Integer.parseInt(String.valueOf(currentData.getValue()));
                                    HashMap<String, Object> hM = new HashMap<>();
                                    hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                    hM.put("verifierId", userId);
//                                    hM.put("alreadyInstalled", checkWhichRBisChecked());
                                    hM.put("image", MARKS_COUNT + ".jpg");
                                    hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                    hM.put("entityType", houseDataHashMap.get(houseTypeSpinner.getSelectedItem()));

                                    rootRef.child("SurveyVerifierData/MarkedHousesByVerifier/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT).setValue(hM);
                                    /*rootRef.child("EntityMarkingData/LastScanTime/Surveyor").child(userId).setValue(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));
                                    rootRef.child("EntityMarkingData/LastScanTime/Ward").child(selectedWard).setValue(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId + "/marked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("marksCount"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/" + selectedWard + "/marked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard + "/marked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/totalMarked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalMarked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalMarked"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/marked"));
                                    if (checkWhichRBisChecked()) {
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled"));
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("alreadyInstalledCount"));
                                    }*/
                                    houseTypeSpinner.setSelection(0);
                                    bottomLnyrLayout.setVisibility(View.GONE);
//                                    setBothRBUnchecked();
//                                    dateTimeTv.setText(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));


                                    ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                                    Bitmap.createScaledBitmap(photo, 400, 600, false)
                                            .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                                    FirebaseStorage.getInstance().getReferenceFromUrl(""+common.getDatabaseStoragePath(MapActivity.this))
                                            .child("SurveyVerifierData/MarkedHousesByVerifierImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT + ".jpg")
                                            .putBytes(toUpload.toByteArray())
                                            .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                                if (taskSnapshot.getTask().isSuccessful()) {
                                                    photo = null;
                                                    enableZoom = true;
                                                    common.closeDialog(MapActivity.this);
                                                }
                                            }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));

                                } catch (Exception e) {
                                    photo = null;
                                    enableZoom = true;
                                    common.closeDialog(MapActivity.this);
                                    e.printStackTrace();
                                }


                            } else {
                                houseTypeSpinner.setSelection(0);
                                common.closeDialog(MapActivity.this);
                                common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                            }
                        }
                    });
        } else {
            Toast.makeText(MapActivity.this, "Please Click Picture Again", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCaptureLocForWalkingMan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;

                    lastKnownLatLngForWalkingMan = new LatLng(locationResult.getLocations().get(latestLocationIndex).getLatitude(),
                            locationResult.getLocations().get(latestLocationIndex).getLongitude());

                    if (boolToInstantiateMovingMarker) {
                        boolToInstantiateMovingMarker = false;
                        common.setMovingMarker(mMap, lastKnownLatLngForWalkingMan, MapActivity.this);
                    }

                    if (cdTimer == null) {
                        timerForWalkingMan();
                    }

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(lastKnownLatLngForWalkingMan);
                    if (dbColl.size() > 0) {
                        for (LatLng ll : dbColl.get(currentLineNumber)) {
                            builder.include(ll);
                        }
                    }
                    if (enableZoom) {
                        enableZoom = false;
                        LatLngBounds bounds = builder.build();
                        int padding = 200;
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.animateCamera(cu);
                    }
                }
            }
        };
        LocationRequest locationRequest = new LocationRequest().setInterval(1000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void timerForWalkingMan() {
        cdTimer = new CountDownTimer(2000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                common.currentLocationShow(lastKnownLatLngForWalkingMan);
                timerForWalkingMan();
            }
        }.start();
    }

    @SuppressLint("SetTextI18n")
    private void drawLine() {
        mMap.clear();
        boolToInstantiateMovingMarker = true;
        currentLineTv.setText("" + (currentLineNumber + 1) + " / " + dbColl.size());
        preferences.edit().putString("lineno", "" + (currentLineNumber + 1)).commit();
        preferences.edit().putString("wardno",selectedWard).commit();
        String line = preferences.getString("lineno", "");
        String ward = preferences.getString("wardno", "");
        Log.e("ward_line",ward+" "+line);
        drawAllLine();
        mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(currentLineNumber))
                .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.upper60), 30))
                .startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start50), 30))
                .color(0xff000000)
                .jointType(JointType.ROUND)
                .width(8));
    }

    private void drawAllLine() {
        for (int i = 0; i < dbColl.size(); i++) {
            if (currentLineNumber != i) {
                mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(i))
                        .startCap(new RoundCap())
                        .endCap(new RoundCap())
                        .color(Color.parseColor("#5abcff"))
                        .jointType(JointType.ROUND)
                        .width(8));
            }
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

    private void focusOnTouch(MotionEvent event) throws Exception {
        if (mCamera != null) {
            try {

                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    Rect rect = calculateFocusArea(event.getX(), event.getY());
                    List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                    meteringAreas.add(new Camera.Area(rect, 800));
                    parameters.setFocusAreas(meteringAreas);
                    mCamera.setParameters(parameters);
                }
                mCamera.autoFocus((success, camera) -> {

                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue());
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue());

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + MapActivity.FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - MapActivity.FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + MapActivity.FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - MapActivity.FOCUS_AREA_SIZE / 2;
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
                common.setProgressDialog("", "Please Wait", MapActivity.this, MapActivity.this);
                isPass = true;
                houseTypeSpinner.setEnabled(false);
                if (captureClickControl) {
                    captureClickControl = false;
                    mCamera.takePicture(null, null, null, pictureCallback);
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                isEdit = false;
                houseTypeSpinner.setSelection(0);
                setBothRBUnchecked();
                dialog.cancel();
                isPass = true;
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                photo = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

                if (photo != null) {
                    if (isEdit) {
                        isPass = true;
                        isEdit = false;
                        captureClickControl = true;
                        houseTypeSpinner.setEnabled(true);
                        common.closeDialog(MapActivity.this);
                        imageViewForRejectedMarker.setImageBitmap(photo);
                    } else {
                        pickLocForEntity();
                    }
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
                    try {
                        focusOnTouch(motionEvent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        } else {
            common.showAlertBox("Line does not exist", "Ok", "", MapActivity.this);
        }
    }

    public void onSaveClick(View view) {
        if (isPass) {
            isPass = false;
            checkGpsForEntity();

            /*if (isSurveyedTrue.isChecked() || isSurveyedFalse.isChecked()) {
                checkGpsForEntity();
            } else {
                isPass = true;
                setBothRBUnchecked();
                houseTypeSpinner.setSelection(0);
                common.showAlertBox("Please Select yes or no option", "ok", "", MapActivity.this);
            }*/
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
        } else {
            common.showAlertBox("Line does not exist", "Ok", "", MapActivity.this);
        }
    }

    @Override
    protected void onPause() {
        try {
            super.onPause();
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();

        if (reader.isConnected()) {
            if (inventorying) {
                mHandler.removeCallbacks(inventoryRunnable);
                inventorying = false;
            }
            if (btnInventorying) {
                mHandler.removeCallbacks(btnInventoryRunnable);
                btnInventorying = false;
            }
        }
        if (reader.isConnected()) {
            reader.disconnect();
        }
    }

    private void removeListeners() {
        if (locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(locationCallback);
        }
        if (cELOnLine != null) {
            rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).removeEventListener(cELOnLine);
        }
        if (cELForAssignedWard != null) {
            rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard").removeEventListener(cELForAssignedWard);
        }
    }

    @Override
    @SuppressLint("StaticFieldLeak")
    public void onBackPressed() {
        super.onBackPressed();
        removeListeners();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logOut) {
            preferences.edit().clear().apply();
            startActivity(new Intent(MapActivity.this, MainActivity.class));
            finish();
        } else if (item.getItemId() == R.id.move_to_line) {
            dialogForMoveToLine();
        }
        return super.onOptionsItemSelected(item);

    }

    @SuppressLint("StaticFieldLeak")
    private void dialogForMoveToLine() {
        try {
            View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.move_to_line_view, null);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
            AlertDialog dialog = alertDialog.create();
            EditText lineNumberEt = dialogLayout.findViewById(R.id.move_to_line_et);
            Button btn = dialogLayout.findViewById(R.id.move_to_line_confirm);
            btn.setOnClickListener(v -> {
                try {
                    if (lineNumberEt != null && lineNumberEt.getText().toString().trim().length() > 0) {
                        int lineNumber = Integer.parseInt(lineNumberEt.getText().toString());
                        if ((currentLineNumber + 1) != lineNumber) {
                            if ((lineNumber - 1) >= 0 && lineNumber <= dbColl.size()) {
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
                                            dialog.cancel();
                                            currentLineNumber = lineNumber - 1;
                                            drawLine();
                                            fetchMarkerForLine(true);
                                        } else {
                                            common.closeDialog(MapActivity.this);
                                            common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                                        }
                                    }
                                }.execute();
                            } else {
                                common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                                lineNumberEt.setText("");
                            }
                        } else {
                            common.showAlertBox("Already on same line", "Ok", "", MapActivity.this);
                            lineNumberEt.setText("");
                        }
                    } else {
                        common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                        assert lineNumberEt != null;
                        lineNumberEt.setText("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                    assert lineNumberEt != null;
                    lineNumberEt.setText("");
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.move_to_line_cancel);
            closeBtn.setOnClickListener(v -> dialog.cancel());
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void dialogForRejectedMarker(MarkersDataModel mdm, Marker marker) {
        try {
            if (dialogForModification != null) {
                dialogForModification.dismiss();
            }
            View diaLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.form_dialog_for_rejected_marker, null);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(diaLayout).setCancelable(false);
            dialogForModification = alertDialog.create();
            Spinner spinner = diaLayout.findViewById(R.id.house_type_spinner_for_rejected_marker);
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
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setSelection(mdm.getHouseType());
            TextView textView = diaLayout.findViewById(R.id.radio_group_heading_tv_for_rejected_marker);
            RadioButton yesRb = diaLayout.findViewById((R.id.is_surveyed_true_rb_for_rejected_marker));
            RadioButton noRb = diaLayout.findViewById((R.id.is_surveyed_false_rb_for_rejected_marker));
            imageViewForRejectedMarker = diaLayout.findViewById(R.id.rejected_marker_image_preview);
            Button imageButton = diaLayout.findViewById(R.id.re_click_picture);
            textView.setText(cbText);

            yesRb.setOnClickListener(view -> {
                yesRb.setChecked(true);
                noRb.setChecked(false);
            });

            noRb.setOnClickListener(view -> {
                yesRb.setChecked(false);
                noRb.setChecked(true);
            });

            if ((mdm.isAlreadyInstalled())) {
                yesRb.setChecked(true);
            } else {
                noRb.setChecked(true);
            }

            imageButton.setOnClickListener(view -> {
                if (isPass) {
                    isPass = false;
                    isEdit = true;
                    openCam();
                }
            });

            try {
                CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(MapActivity.this);
                circularProgressDrawable.setStrokeWidth(5f);
                circularProgressDrawable.setCenterRadius(30f);
                circularProgressDrawable.start();
                FirebaseStorage.getInstance()
                        .getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity + "/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getImageName())
                        .getDownloadUrl()
                        .addOnSuccessListener(uri -> Glide.with(MapActivity.this)
                                .load(uri)
                                .placeholder(circularProgressDrawable)
                                .into(imageViewForRejectedMarker))
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                try {
                                    Toast.makeText(MapActivity.this, "Image Not Available ", Toast.LENGTH_SHORT).show();
                                    imageViewForRejectedMarker.setImageResource(R.drawable.img_not_available);
                                } catch (Exception exc) {
                                    exc.printStackTrace();
                                }

                            }
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }

            Button btn = diaLayout.findViewById(R.id.form_dialog_confirm);
            btn.setOnClickListener(v -> {
                LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                        .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                        .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                    try {
                        task1.getResult(ApiException.class);
                        if (task1.isSuccessful()) {
                            if (lastKnownLatLngForWalkingMan != null) {
                                if (photo != null) {
                                    common.setProgressDialog("", "Please Wait", MapActivity.this, MapActivity.this);
                                    HashMap<String, Object> mapTemp = new HashMap<>();
                                    mapTemp.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                    mapTemp.put("modifiedBy", userId);
                                    mapTemp.put("alreadyInstalled", yesRb.isChecked());
                                    mapTemp.put("image", mdm.getImageName());
                                    mapTemp.put("modifiedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                    mapTemp.put("status", "Re-marked");
                                    mapTemp.put("houseType", houseDataHashMap.get(spinner.getSelectedItem()));
                                    rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getMarkerNumber()).updateChildren(mapTemp);

                                    int temp = Boolean.compare(mdm.isAlreadyInstalled(), yesRb.isChecked());
                                    if (temp == 0) {
                                    } else if (temp > 0) {
                                        common.decCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled"));
                                        common.decCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("alreadyInstalledCount"));
                                    } else {
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("alreadyInstalledCount"));
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled"));
                                    }

                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/totalModified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalModified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalModified"));

                                    try {
                                        ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                                        Bitmap.createScaledBitmap(photo, 400, 600, false)
                                                .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                                        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity)
                                                .child("/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getImageName())
                                                .putBytes(toUpload.toByteArray())
                                                .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                                    if (taskSnapshot.getTask().isSuccessful()) {
                                                        dialogForModification.dismiss();
                                                        photo = null;
                                                        enableZoom = true;
                                                        common.closeDialog(MapActivity.this);
                                                        marker.setIcon(common.BitmapFromVector(MapActivity.this, R.drawable.gharicon));
                                                        marker.setPosition(lastKnownLatLngForWalkingMan);
                                                        mDMMap.remove(marker.getPosition());
                                                    }
                                                }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        dialogForModification.dismiss();
                                        photo = null;
                                        enableZoom = true;
                                        common.closeDialog(MapActivity.this);
                                        marker.setIcon(common.BitmapFromVector(MapActivity.this, R.drawable.gharicon));
                                        marker.setPosition(lastKnownLatLngForWalkingMan);
                                        mDMMap.remove(marker.getPosition());
                                    }
                                } else {
                                    Toast.makeText(MapActivity.this, "Please Click New Image", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (ApiException e) {
                        if (e instanceof ResolvableApiException) {
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(MapActivity.this, GPS_CODE_FOR_MODIFICATION);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            });
            Button closeBtn = diaLayout.findViewById(R.id.form_dialog_cancel);
            closeBtn.setOnClickListener(v -> dialogForModification.cancel());
            dialogForModification.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                    houseTypeSpinner.setSelection(0);
                    setBothRBUnchecked();
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
                    houseTypeSpinner.setSelection(0);
                    setBothRBUnchecked();
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == MAIN_LOC_REQUEST) {
                if (resultCode == RESULT_OK) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
                }
            } else if (requestCode == GPS_CODE_FOR_MODIFICATION) {
                if (resultCode == RESULT_OK) {

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressLint({"CommitPrefEdits", "SetTextI18n"})
    private void assignedWardCEL() {
        cELForAssignedWard = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!String.valueOf(snapshot.getValue()).equals(selectedWard)) {
                    try {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                        builder.setMessage("आपका असाइन किया गया वार्ड बदल गया है").setCancelable(false)
                                .setPositiveButton("Ok", (dialog, id) -> {
                                    mMap.clear();
                                    preferences.edit().putString("assignment", String.valueOf(snapshot.getValue())).apply();
                                    selectedWard = String.valueOf(snapshot.getValue());
                                    currentLineNumber = 0;
                                    titleTv.setText("Ward: " + selectedWard);
                                    common.getDatabaseSp(MapActivity.this).edit().remove("wardJSONLastUpdate").apply();
                                    common.getDatabaseSp(MapActivity.this).edit().remove("wardJSON").apply();
                                    fetchWardJson();
                                    mainCheckLocationForRealTimeRequest();
                                    dialog.cancel();
                                })
                                .setNegativeButton("", (dialog, i) -> dialog.cancel());
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard/").addValueEventListener(cELForAssignedWard);
    }

    private void lastScanTimeVEL() {
        rootRef.child("EntityMarkingData/LastScanTime/Surveyor/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    dateTimeTv.setText(snapshot.getValue().toString());
                } else {
                    dateTimeTv.setText("---");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkVersionForTheApplication() {
        rootRef.child("Settings/LatestVersions/entityMarking").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    try {
                        String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                        if (!version.equalsIgnoreCase(snapshot.getValue().toString())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                            builder.setMessage("Version Expired").setCancelable(false)
                                    .setPositiveButton("Ok", (dialog, id) -> {
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
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                    builder.setMessage("Version Expired").setCancelable(false)
                            .setPositiveButton("Ok", (dialog, id) -> {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void onInventoryAction() {
        if (inventorying) {
            mHandler.post(inventoryRunnable);
            inventorying = true;
//            btnRead.setText(getResources().getString(R.string.text_btn_inventory_stop));
//            updateControls(false);
        } else {
            mHandler.removeCallbacks(inventoryRunnable);
            inventorying = false;
//            btnRead.setText(getResources().getString(R.string.text_btn_inventory_start));
//            updateControls(true);
        }
    }
}