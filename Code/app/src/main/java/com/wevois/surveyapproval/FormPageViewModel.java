package com.wevois.surveyapproval;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.wevois.surveyapproval.adapter.ParisarAdapter;
import com.wevois.surveyapproval.repository.Repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class FormPageViewModel extends ViewModel {
    Activity activity;
    Context context;
    FormPageActivity formPageActivity;
    SharedPreferences preferences;
    ParisarAdapter parisarAdapter;
    ParisarAdapter adapter;
    CommonFunctions common = CommonFunctions.getInstance();
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    List<String> houseTypeList = new ArrayList<>(), houseTypeListRevisit = new ArrayList<>(), revisitTypeList = new ArrayList<>();
    JSONArray jsonArrayHouseType = new JSONArray(), jsonArrayHouseTypeRevisit = new JSONArray();
    JSONObject dataObject = new JSONObject(), jsonObject = new JSONObject(), jsonObjectWard = new JSONObject();
    Spinner spinnerHouseType, spinnerRevisitReason, spinnerRevisitHouseType;
    AlertDialog customTimerAlertBox, customTimerAlertBoxForImage, saveAlertBox;
    public final ObservableField<String> userTv = new ObservableField<>("");
    public final ObservableField<String> mobileTv = new ObservableField<>("");
    public final ObservableField<String> totalHousesTv = new ObservableField<>("");
    public final ObservableField<String> addressTv = new ObservableField<>("");
    public final ObservableField<String> revisitNameTv = new ObservableField<>("");
    public ObservableField<Boolean> isVisible = new ObservableField<>(false);
    public ObservableField<Boolean> isMoreBtnVisible = new ObservableField<>(false);
    public ObservableField<Boolean> isChecked = new ObservableField<>(false);
    public ObservableField<Boolean> isCheckedAwasiye = new ObservableField<>(true);
    public ObservableField<Boolean> isVisibleBtnRevisit = new ObservableField<>(true);
    public ObservableField<Boolean> isVisibleTvRevisitNote = new ObservableField<>(true);
    public ObservableField<Boolean> isVisibleBtnRevisitSave = new ObservableField<>(false);
    public ObservableField<Boolean> isVisibleRevisitName = new ObservableField<>(false);
    public ObservableField<Boolean> isVisibleReasonRevisit = new ObservableField<>(false);
    public ObservableField<Boolean> isVisibleRadioRevisit = new ObservableField<>(false);
    public ObservableField<Boolean> isVisibleCardRevisit = new ObservableField<>(false);
//    public ObservableField<Boolean> addBtnVisibility = new ObservableField<>(false);

    ArrayList<String> oldMobiles = new ArrayList<>(), newMobiles = new ArrayList<>();
    String mobileNumber = "", hT = "", markingKey = "", currentDate, countCheck = "2",
            currentCardNumber, from = "";
    boolean isMoved = true, isValid = true, isDelete = false;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    RecyclerView rcy_parisar_data;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    File myPath = null;
    File myHousePath = null;
    EditText tv_totalhouse;
    ImageView addMoreRow;
    Bitmap identityBitmap = null;
    Bitmap houseImage = null;
    ImageView imgHouse, imgcard;
    ImageView btnMore;
    String house_type;
    ArrayList<SubHouseModel> list = new ArrayList<>();
    ArrayList<SubHouseModel> entitie_list = new ArrayList<>();
    ArrayList<Bitmap> bitmaps = new ArrayList<>();
//    ParisarAdapter parisarAdapter;
//    public ArrayList<String> list = new ArrayList<>();

    public void init(ArrayList<SubHouseModel> list, ArrayList<Bitmap> bitmaps) {
        this.list = list;
        this.bitmaps = bitmaps;
    }

    public void init(ParisarAdapter parisarAdapter) {
        this.parisarAdapter = parisarAdapter;
    }

    public void init(FormPageActivity activity, FormPageActivity formPageActivity, EditText etTotalHouse, ImageView addMoreRow, RecyclerView rcyParisarData, ImageView imgCard, ImageView imgHouse, Spinner spnrHouseType, String froms, Spinner spnrHouseTypeCardRevisit, Spinner spnrReason) {
        this.activity = activity;
        this.imgcard = imgCard;
        this.imgHouse = imgHouse;
        context = activity;
        this.formPageActivity = formPageActivity;
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        currentCardNumber = preferences.getString("cardNo", "");
        from = froms;
        tv_totalhouse = etTotalHouse;
        this.addMoreRow = addMoreRow;
        spinnerHouseType = spnrHouseType;
        spinnerRevisitHouseType = spnrHouseTypeCardRevisit;
        spinnerRevisitReason = spnrReason;
        rcy_parisar_data = rcyParisarData;
        hT = preferences.getString("houseType", "");
        markingKey = preferences.getString("markingKey", "");
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        currentDate = dateFormat1.format(new Date());
        awasiyeButtonClick();
        getHouseTypesCardRevisit(false);
//        getCardRevisitReasons();
/*
        if (Integer.parseInt(hT) != 1 && Integer.parseInt(hT) != 19) {
            commercialButtonClick();
        }
*/
        try {
            for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                if (jsonArrayHouseType.get(i).toString().equals(hT)) {
                    spinnerHouseType.setSelection(i + 1);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        rcy_parisar_data.setLayoutManager(linearLayoutManager);
        adapter = new ParisarAdapter(context, new ParisarAdapter.SubFormClick() {
            @Override
            public void onClickForm(int pos) {
                Log.e("paris adapterar", " adapter " + pos);
            }
        });
        parisarAdapter = adapter;
        rcy_parisar_data.setAdapter(adapter);

        /*if (!from.equalsIgnoreCase("map")) {
            new Repository().checkSurveyDetailsIfAlreadyExists(activity, currentCardNumber).observeForever(dataSnapshot -> {
                if (dataSnapshot != null) {
                    if (dataSnapshot.getValue() != null) {
                        int count=0;
                        entitie_list.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                            Log.e("DataSnapshot",""+count);
                            for (DataSnapshot snapshot1 : snapshot.child("Entities").getChildren()){
                                Log.e("Sub Houses value",""+snapshot.child("Entities").child(snapshot1.getKey()).child("name").getValue());
//                                Log.e("Sub Houses value", "" + snapshot.child(snapshot1.getKey()).child("name").getValue());
                                count++;
                                SubHouseModel subHouseModel = new SubHouseModel();
                                subHouseModel.setName(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("name").getValue()));
                                subHouseModel.setMobile(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("mobile").getValue()));
                                subHouseModel.setImg(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("house image").getValue()));
                                entitie_list.add(subHouseModel);
                            }

                            if (entitie_list.size() > 0){
                                isMoreBtnVisible.set(false);
                                totalHousesTv.set(String.valueOf(count));
                                adapter = new ParisarAdapter(context,entitie_list, new ParisarAdapter.SubFormClick() {
                                    @Override
                                    public void onClickForm(int pos) {
                                        Intent intent = new Intent(context, SubFormPageActivity.class);
                                        intent.putExtra("pos",pos);
                                        intent.putExtra("name",entitie_list.get(pos).getName());
                                        intent.putExtra("type","edit");
                                        intent.putExtra("mobile",entitie_list.get(pos).getMobile());
                                        context.startActivity(intent);
                                    }
                                });
                                parisarAdapter = adapter;
                                rcy_parisar_data.setAdapter(adapter);
                            }
                            if (snapshot.child("Entities").child("1") != null){
                                Log.e("Sub Houses value",""+snapshot.child("Entities").child("1").child("name").getValue());
                            }
                            if (snapshot.child("name").getValue() != null && snapshot.child("name").getValue().toString().length() > 0) {
                                userTv.set(snapshot.child("name").getValue().toString());
                            }
                            if (snapshot.child("houseType").getValue() != null && snapshot.child("houseType").getValue().toString().length() > 0) {
                                if (Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 1 && Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 19) {
                                    commercialButtonClick();
                                } else {
                                    awasiyeButtonClick();
                                }
                                try {
                                    for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                                        if (jsonArrayHouseType.get(i).toString().equals(snapshot.child("houseType").getValue().toString())) {
                                            spinnerHouseType.setSelection(i + 1);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (snapshot.child("address").getValue() != null && snapshot.child("address").getValue().toString().length() > 0) {
                                addressTv.set(snapshot.child("address").getValue().toString());
                            }
                            if (snapshot.child("servingCount").getValue() != null && snapshot.child("servingCount").getValue().toString().length() > 0) {
                                totalHousesTv.set(snapshot.child("servingCount").getValue().toString());
                            }
                            if (snapshot.child("mobile").getValue() != null && snapshot.child("mobile").getValue().toString().length() > 0) {
                                mobileNumber = snapshot.child("mobile").getValue().toString();
                                if (mobileNumber.contains(",")) {
                                    String[] mobile = mobileNumber.trim().split(",");
                                    for (int i = 0; i < mobile.length; i++) {
                                        oldMobiles.add(mobile[i].trim());
                                    }
                                } else {
                                    oldMobiles.add(mobileNumber);
                                }
                                mobileTv.set(mobileNumber);
                            }
                        }
                    }
                }
            });
            new Repository().checkRfidAlreadyExists(activity, preferences.getString("ward", ""), preferences.getString("line", ""), preferences.getString("rfid", "")).observeForever(dataSnapshot -> {
                if (dataSnapshot != null) {
                    if (dataSnapshot.getValue() != null) {
                        countCheck = "1";
                    } else {
                        countCheck = "2";
                    }
                }
            });
        }*/
    }

    public void addMoreRow() {
        Log.e("Enter", "add more row");
//        parisarAdapter.notifyDataSetChanged();
    }


    public void saveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                showAlertDialog(true);
            }
        }
    }

    public void saveImageHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                showAlertDialog(false);
            }
        }
    }

    public void saveRevisitImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                showAlertDialog(false);
            }
        }
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
        if (Math.abs(touchCoordinateInCameraReper) + FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - FOCUS_AREA_SIZE / 2;
        }
        return result;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
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

    @SuppressLint({"StaticFieldLeak", "ClickableViewAccessibility"})
    public void showAlertDialog(boolean isHouses) {
        try {
            if (customTimerAlertBox != null) {
                customTimerAlertBox.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View dialogLayout = inflater.inflate(R.layout.custom_camera_alertbox, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        customTimerAlertBox = alertDialog.create();
        surfaceView = (SurfaceView) dialogLayout.findViewById(R.id.surfaceViews);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        surfaceViewCallBack = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera = Camera.open();
                } catch (RuntimeException e) {
                }
                Camera.Parameters parameters;
                parameters = mCamera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
                mCamera.setParameters(parameters);
                setCameraDisplayOrientation(activity, 0, mCamera);
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                    mCamera.startPreview();
                } catch (Exception e) {
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
        Button btn = dialogLayout.findViewById(R.id.capture_image_btn);
        btn.setOnClickListener(v -> {
            common.setProgressBar("Processing...", activity, activity);
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    mCamera.takePicture(null, null, null, pictureCallback);
                    return null;
                }
            }.execute();
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
        closeBtn.setOnClickListener(v -> {
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
        });
        if (!activity.isFinishing()) {
            customTimerAlertBox.show();
        }
        pictureCallback = (bytes, camera) -> {
            Matrix matrix = new Matrix();
            matrix.postRotate(90F);
            Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmaps = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
            Bitmap bitmap = Bitmap.createScaledBitmap(bitmaps, 400, 600, false);

            camera.stopPreview();
            if (camera != null) {
                camera.release();
                mCamera = null;
            }
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            showAlertBoxForImage(bitmap, isHouses);
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
    }


    private void showAlertBoxForImage(Bitmap i, boolean isHouses) {
        try {
            common.closeDialog(activity);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View dialogLayout = inflater.inflate(R.layout.image_view_layout, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
        if (i != null) {
            markerImage.setImageBitmap(i);
        }
        dialogLayout.findViewById(R.id.okeyBtn).setOnClickListener(view1 -> {
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }

            if (isHouses) {
                common.setProgressBar("Processing...", activity, activity);
                identityBitmap = i;
                imgcard.setImageBitmap(identityBitmap);
                setOnLocal();
            } else {
                common.setProgressBar("Processing...", activity, activity);
                houseImage = i;
                imgHouse.setImageBitmap(houseImage);
                setOnLocalHouse();
            }
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_view_btn);
        closeBtn.setOnClickListener(view1 -> {
            common.closeDialog(activity);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
            isMoved = true;
        });
        customTimerAlertBoxForImage = alertDialog.create();
        if (!activity.isFinishing()) {
            customTimerAlertBoxForImage.show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void setOnLocalHouse() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                File root = new File(Environment.getExternalStorageDirectory(), "SurveyHouseImage");
                if (!root.exists()) {
                    root.mkdirs();
                }
                myHousePath = new File(root, currentCardNumber + "House" + ".jpg");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myPath);
                    houseImage.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fos.close();
                    } catch (Exception ignored) {
                        ignored.getMessage();
                    }
                }
                common.closeDialog(activity);
                return null;
            }
        }.execute();

    }

    @SuppressLint("StaticFieldLeak")
    private void setOnLocal() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                File root = new File(Environment.getExternalStorageDirectory(), "SurveyCardImage");
                if (!root.exists()) {
                    root.mkdirs();
                }
                myPath = new File(root, currentCardNumber + ".jpg");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myPath);
                    identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fos.close();
                    } catch (Exception ignored) {
                        ignored.getMessage();
                    }
                }
                common.closeDialog(activity);
                return null;
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public void saveData() {
        if (isMoved) {
            isMoved = false;
            common.setProgressBar("Please Wait...", activity, activity);
            new Repository().checkNetWork(activity).observeForever(response -> {
                int i;
                if (response) {
                    i = 1;
                } else {
                    i = 2;
                }
                if (validateSurveyForm()) {
                    if (from.equalsIgnoreCase("map")) {
                        if (i == 1) {
                            saveRfidImageData();
                        } else {
                            isMoved = true;
                            common.showAlertBox("No internet connection.", false, activity,activity);
                        }
                    } else {
                        String mobile = mobileTv.get();
                        String rfID = preferences.getString("rfid", "");
                        saveOfflineData(mobile, rfID, i);
                    }
                } else {
                    isMoved = true;
                }
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void saveRfidImageData() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStoragePath(activity) + "/SurveyRfidNotFoundCardImage");
                StorageReference mountainImagesRef = storageRef.child(preferences.getString("rfid", "") + ".jpg");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] data = baos.toByteArray();
                UploadTask uploadTask = mountainImagesRef.putBytes(data);
                uploadTask.addOnFailureListener(exception -> {
                    common.closeDialog(activity);
                }).addOnSuccessListener(taskSnapshot -> {
                    try {
                        if (myPath != null) {
                            saveRfidHomeImageData();
                        } else {
                            myPath.delete();
                        }

                    } catch (Exception e) {
                    }
                    setRfidNotFoundData();
                });
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void saveRfidHomeImageData() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStoragePath(activity) + "/SurveyRfidNotFoundCardImage");
                StorageReference mountainImagesRef = storageRef.child(preferences.getString("rfid", "") + ".jpg");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                houseImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] data = baos.toByteArray();
                UploadTask uploadTask = mountainImagesRef.putBytes(data);
                uploadTask.addOnFailureListener(exception -> {
                    common.closeDialog(activity);
                }).addOnSuccessListener(taskSnapshot -> {
                    try {
                        if (myPath != null) {

                            myPath.delete();
                        }

                    } catch (Exception e) {
                    }
                    setRfidNotFoundData();
                });
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
            }
        }.execute();

    }

    private void setRfidNotFoundData() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String mobile = mobileTv.get();
        String lastCharacterOfNumber = mobile.substring(mobile.length() - 1);
        if (lastCharacterOfNumber.equalsIgnoreCase(",")) {
            mobile = mobile.substring(0, mobile.length() - 1);
        }
        HashMap<String, Object> housesMap = new HashMap<>();
        housesMap.put("address", addressTv.get());
        housesMap.put("cardNo", "");
        housesMap.put("phaseNo", "2");
        housesMap.put("createdDate", timeFormat.format(new Date()));
        housesMap.put("surveyorId", preferences.getString("userId", ""));
        try {
            housesMap.put("houseType", jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
        } catch (JSONException e) {
        }
        housesMap.put("latLng", "(" + preferences.getString("lat", "") + "," + preferences.getString("lng", "") + ")");
        housesMap.put("line", preferences.getString("line", ""));
        housesMap.put("name", userTv.get());
        housesMap.put("mobile", mobile);
        if (isCheckedAwasiye.get()) {
            housesMap.put("cardType", "आवासीय");
        } else {
            housesMap.put("cardType", "व्यावसायिक");
        }
        housesMap.put("rfid", preferences.getString("rfid", ""));
        housesMap.put("ward", preferences.getString("ward", ""));
        housesMap.put("cardImage", preferences.getString("rfid", "") + ".jpg");
        housesMap.put("houseImage", preferences.getString("rfid", "") + ".jpg");
        if (isVisible.get()) {
            housesMap.put("servingCount", tv_totalhouse.getText().toString());
        }
        new Repository().saveRfidNotFoundData(activity, housesMap).observeForever(dataSnapshot -> {
            showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true, "rfid", preferences.getString("rfid", ""));
        });
    }

    private boolean validateSurveyForm() {
        if (userTv.get().length() == 0) {
            common.showAlertBox("Wrong assignment!\nPlease enter valid name.", false, activity,activity);
            isValid = false;
        } else if (spinnerHouseType.getSelectedItem().toString().equals("Select Entity type")) {
            View selectedView = spinnerHouseType.getSelectedView();
            if (selectedView != null && selectedView instanceof TextView) {
                spinnerHouseType.requestFocus();
                TextView selectedTextView = (TextView) selectedView;
                selectedTextView.setError("error");
                selectedTextView.setTextColor(Color.RED);
                selectedTextView.setText("please select type");
                spinnerHouseType.performClick();
            }
            common.closeDialog(activity);
            isValid = false;
        } else if (isVisible.get() && (tv_totalhouse.getText().toString().length() == 0)) {
            common.showAlertBox("Wrong assignment!\nPlease fill.", false, activity,activity);
            isValid = false;
            common.closeDialog(activity);
        } else if (addressTv.get().length() == 0) {
            common.showAlertBox("Wrong assignment!\nPlease enter address.", false, activity,activity);
            isValid = false;
        } else if (identityBitmap == null) {
            common.showAlertBox("कृपया पहले कार्ड की फोटो खींचे .", false, activity, activity);
            isValid = false;
        } else if (houseImage == null) {
            common.showAlertBox("कृपया पहले घर की फोटो खींचे .", false, activity, activity);
            isValid = false;

        } else {
            newMobiles = new ArrayList<>();
            if (mobileTv.get().contains(",")) {
                String[] mobile = mobileTv.get().trim().split(",");
                for (int i = 0; i < mobile.length; i++) {
                    if (mobile[i].trim().length() != 10) {
                        common.showAlertBox("Wrong assignment!\nPlease enter correct mobile number.", false, activity, activity);
                        isValid = false;
                        break;
                    } else {
                        newMobiles.add(mobile[i].trim());
                        isValid = true;
                    }
                }
            } else {
                if (mobileTv.get().trim().length() != 10) {
                    common.showAlertBox("Wrong assignment!\nPlease enter correct mobile number.", false, activity, activity);
                    isValid = false;
                } else {
                    newMobiles.add(mobileTv.get().trim());
                    isValid = true;
                }
            }
        }
        return isValid;
    }

    public void awasiyeButtonClick() {
        isChecked.set(false);
        isCheckedAwasiye.set(true);
        getHouseTypes(false);
    }

    public void commercialButtonClick() {
        isChecked.set(true);
        isCheckedAwasiye.set(false);
        getHouseTypes(true);
    }

    public void awasiyeButtonCardRevisitClick() {
        getHouseTypesCardRevisit(false);
    }

    public void commercialButtonCardRevisitClick() {
        getHouseTypesCardRevisit(true);
    }

    private void getHouseTypes(Boolean isCommercial) {
        houseTypeList.clear();
        houseTypeList.add("Select Entity type");
        JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
        jsonArrayHouseType = new JSONArray();
        try {
            jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
            commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
            residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
            for (int i = 1; i <= jsonObject.length(); i++) {
                if (isCommercial) {
                    try {
                        Log.e("House type", "name " + commercialJsonObject.getString(String.valueOf(i)));
                        houseTypeList.add(commercialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                } else {
                    try {
                        Log.e("House type", "name " + residentialJsonObject.getString(String.valueOf(i)));
                        houseTypeList.add(residentialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bindHouseTypesToSpinner();
    }

    private void bindHouseTypesToSpinner() {
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHouseType.setAdapter(spinnerArrayAdapter);
        spinnerHouseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                String hintText = "";
                isVisible.set(false);
                tv_totalhouse.setVisibility(View.GONE);
//                addMoreRow.setVisibility(View.GONE);
//                tv_totalhouse.setText("");

//                isMoreBtnVisible.set(false);
                Log.e("form page view model ", "method call");
//                rcy_parisar_data.setVisibility(View.GONE);
                try {
                    switch (Integer.parseInt(jsonArrayHouseType.get(position - 1).toString())) {
                        case 19:
//                            hintText = "Enter No of Houses";
                            isVisible.set(true);
                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            addMoreRow.setVisibility(View.VISIBLE);
//                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            break;
                        case 20:
//                            hintText = "Enter No of Shops";
                            isVisible.set(true);
                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            addMoreRow.setVisibility(View.VISIBLE);
//                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            break;
                        default:
                            tv_totalhouse.setVisibility(View.GONE);
                            rcy_parisar_data.setVisibility(View.GONE);
                            addMoreRow.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                }
//                totalHousesTv.set(hintText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public TextWatcher userTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                userTv.set(editable.toString());
            }
        };
    }

    public TextWatcher mobileTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mobileTv.set(editable.toString());
            }
        };
    }

    public TextWatcher totalHousesTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                totalHousesTv.set(editable.toString());
                isMoreBtnVisible.set(true);
                if (tv_totalhouse.getText().toString().isEmpty()) {
                    isMoreBtnVisible.set(false);
//                    btnMore.setVisibility(View.GONE);
                    adapter = new ParisarAdapter(context, new ParisarAdapter.SubFormClick() {
                        @Override
                        public void onClickForm(int pos) {

                        }
                    });
                    parisarAdapter = adapter;
                    rcy_parisar_data.setAdapter(adapter);
                } else {

                    isMoreBtnVisible.set(true);
//                    btnMore.setVisibility(View.VISIBLE);
                    adapter = new ParisarAdapter(context, new ParisarAdapter.SubFormClick() {
                        @Override
                        public void onClickForm(int pos) {
                            Log.e("pos ", "pos= " + pos);
                            Intent intent = new Intent(context, SubFormPageActivity.class);
                            context.startActivity(intent);
                        }
                    });
                    parisarAdapter = adapter;
                    rcy_parisar_data.setAdapter(adapter);
                }
            }
        };
    }

    public TextWatcher addressTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                addressTv.set(editable.toString());
            }
        };
    }

    public TextWatcher revisitNameTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                revisitNameTv.set(editable.toString());
            }
        };
    }

    public void onBack() {
        if (isMoved) {
            try {
                if (myPath != null) {
                    myPath.delete();
                }
            } catch (Exception e) {
            }
            isMoved = false;
            if (from.equalsIgnoreCase("map")) {
                activity.finish();
            } else {
//                Intent intent = new Intent(activity, VerifyPageActivity.class);
//                activity.startActivity(intent);
//                activity.finish();
            }
        }
    }

    private void getHouseTypesCardRevisit(Boolean isCommercial) {
        houseTypeListRevisit.clear();
        houseTypeListRevisit.add("Select Entity type");
        JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
        jsonArrayHouseTypeRevisit = new JSONArray();
        try {
            jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
            commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
            residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
            for (int i = 1; i <= jsonObject.length(); i++) {
                if (isCommercial) {
                    try {
                        houseTypeListRevisit.add(commercialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseTypeRevisit.put(i);
                    } catch (JSONException e) {
                    }
                } else {
                    try {
                        houseTypeListRevisit.add(residentialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseTypeRevisit.put(i);
                    } catch (JSONException e) {
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bindHouseTypesToSpinnerCardRevisit();
    }

    private void bindHouseTypesToSpinnerCardRevisit() {
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, houseTypeListRevisit) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRevisitHouseType.setAdapter(spinnerArrayAdapter);
    }

    private void getCardRevisitReasons() {
        revisitTypeList.add("Select Reason type");
        String listAsString = preferences.getString("revisitReasonList", null);
        String[] reasonString = listAsString.substring(1, listAsString.length() - 1).split(",");
        for (int i = 0; i < reasonString.length; i++) {
            String reasonType = reasonString[i].replace("~", ",");
            revisitTypeList.add(reasonType);
        }
        final ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, revisitTypeList) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRevisitReason.setAdapter(spinnerArrayAdapter1);
    }

    public void showAlertBox(String message, boolean surveyCompleted, String from, String value) {
        common.closeDialog(activity);
        try {
            if (saveAlertBox != null) {
                saveAlertBox.dismiss();
            }
        } catch (Exception e) {
        }
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(activity);
        alertAssignment.setMessage(message);
        alertAssignment.setCancelable(false);
        alertAssignment.setPositiveButton("OK", (dialog, id) -> {
            if (surveyCompleted) {
                preferences.edit().putString("isOnResumeCall", "yes").apply();
                if (from.equalsIgnoreCase("survey")) {
                    saveMarkingData(4, value);
                } else if (from.equalsIgnoreCase("revisit")) {
                    saveMarkingData(3, value);
                } else {
                    saveMarkingData(5, value);
                }
            }
        });
        saveAlertBox = alertAssignment.create();
        if (!activity.isFinishing()) {
            saveAlertBox.show();
        }
    }

    private void saveMarkingData(int i, String v) {
        JSONObject jsonObject = new JSONObject();
        if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
            try {
                jsonObject = new JSONObject(preferences.getString("markingData", ""));
            } catch (Exception e) {
            }
        }
        JSONObject markingDataObject = new JSONObject();
        try {
            markingDataObject = jsonObject.getJSONObject(preferences.getString("line", ""));
            JSONArray jsonArray = markingDataObject.getJSONArray(markingKey);
            jsonArray.put(i, v);
            try {
                markingDataObject.put(markingKey, jsonArray);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        try {
            jsonObject.put(preferences.getString("line", ""), markingDataObject);
        } catch (Exception e) {
        }
        preferences.edit().putString("markingData", jsonObject.toString()).apply();
        activity.finish();
    }

    private void saveOfflineData(String mobile, String rfID, int i) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String line = preferences.getString("line", "");
        String ward = preferences.getString("ward", "");
        String cardNo = preferences.getString("cardNo", "");
        try {
            if (preferences.getString("scanHousesData", "").length() > 0) {
                try {
                    dataObject = new JSONObject(preferences.getString("scanHousesData", "")).getJSONObject(ward);
                } catch (Exception ignored) {
                }
            }
            jsonObject.put("mobile", mobile);
            jsonObject.put("ward", ward);
            jsonObject.put("address", addressTv.get());
            jsonObject.put("cardno", cardNo);
            jsonObject.put("createddate", timeFormat.format(new Date()));
            jsonObject.put("housetype", jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
            jsonObject.put("lat", preferences.getString("lat", ""));
            jsonObject.put("lng", preferences.getString("lng", ""));
            jsonObject.put("line", line);
            jsonObject.put("name", userTv.get());
            jsonObject.put("rfid", rfID);
            jsonObject.put("markingKey", markingKey);
            jsonObject.put("markingRevisit", preferences.getString("markingRevisit", "no"));
            JSONObject temp = new JSONObject();
            temp.put("StorageImage", "no");
            temp.put("Houses", "no");
            temp.put("CardWardMapping", "no");
            temp.put("CardScanData", "no");
            temp.put("EntityMarking", "no");
            temp.put("DailyHouseCount", "no");
            temp.put("TotalHouseCount", "no");
            temp.put("SurveyDateWise", "no");
            temp.put("SurveyStartDate", "no");
            jsonObject.put("details", temp);
            if (isCheckedAwasiye.get()) {
                jsonObject.put("cardType", "आवासीय");
            } else {
                jsonObject.put("cardType", "व्यावसायिक");
            }
            if (isVisible.get()) {
                jsonObject.put("servingcount", tv_totalhouse.getText().toString());
            }
            dataObject.put(cardNo, jsonObject);
            jsonObjectWard.put(ward, dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
            if (i == 1) {
                saveSurveyDetails();
            } else {
                showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true, "offline", "");
            }
        } catch (Exception e) {
        }
    }

    private void saveSurveyDetails() {
        if (validateSurveyForm()) {
            new Repository().CheckWardMapping(activity, currentCardNumber).observeForever(dataSnapshot -> {
                if (dataSnapshot != null) {
                    if (dataSnapshot.getValue() != null) {
                        String line = "", ward = "";
                        if (dataSnapshot.hasChild("line")) {
                            line = dataSnapshot.child("line").getValue().toString();
                        }
                        if (dataSnapshot.hasChild("ward")) {
                            ward = dataSnapshot.child("ward").getValue().toString();
                        }
                        if (line.equalsIgnoreCase(preferences.getString("line", "")) && ward.equalsIgnoreCase(preferences.getString("ward", ""))) {
                            saveSurveyData();
                        } else {
                            isMoved = true;
                            removeCardLocalData();
                            showAlertBox("Error", true, "offline", "");
                        }
                    } else {
                        saveSurveyData();
                    }
                } else {
                    saveSurveyData();
                }
            });
        } else {
            isMoved = true;
            common.closeDialog(activity);
        }
    }

    private void saveSurveyData() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String mobile = mobileTv.get();
        String house_count = tv_totalhouse.getText().toString();
        Log.e("Total house", " " + house_count);
        String lastCharacterOfNumber = mobile.substring(mobile.length() - 1);
        if (lastCharacterOfNumber.equalsIgnoreCase(",")) {
            mobile = mobile.substring(0, mobile.length() - 1);
        }
        HashMap<String, Object> housesMap = new HashMap<>();
        housesMap.put("address", addressTv.get());
        housesMap.put("cardNo", currentCardNumber);
        housesMap.put("phaseNo", "2");
        if (countCheck.equals("2")) {
            housesMap.put("createdDate", timeFormat.format(new Date()));
            housesMap.put("surveyorId", preferences.getString("userId", ""));
        } else {
            if (isDelete) {
                housesMap.put("createdDate", timeFormat.format(new Date()));
                housesMap.put("surveyorId", preferences.getString("userId", ""));
            }
            housesMap.put("surveyModifierId", preferences.getString("userId", ""));
            housesMap.put("modifiedDate", timeFormat.format(new Date()));
        }
        try {
            house_type = jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString();
            housesMap.put("houseType", jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
        } catch (JSONException e) {
        }
        housesMap.put("latLng", "(" + preferences.getString("lat", "") + "," + preferences.getString("lng", "") + ")");
        housesMap.put("line", preferences.getString("line", ""));
        housesMap.put("name", userTv.get());
        housesMap.put("mobile", mobile);
        if (isCheckedAwasiye.get()) {
            housesMap.put("cardType", "आवासीय");
        } else {
            housesMap.put("cardType", "व्यावसायिक");
        }
        housesMap.put("rfid", preferences.getString("rfid", ""));
        housesMap.put("ward", preferences.getString("ward", ""));
        housesMap.put("cardImage", currentCardNumber + ".jpg");
        housesMap.put("houseImage", currentCardNumber + "House" + ".jpg");
        housesMap.put("servingCount", house_count);

        ArrayList<SubHouseModel> list_items = formPageActivity.getSubHouseData();
        Log.e("adater", "ss " + list_items.size() + " " + list.size());
        HashMap<String, Object> subhousesMap = new HashMap<>();
        if (house_type.equals("19") || house_type.equals("20")) {
            for (int i = 0; i < list.size(); i++) {
                HashMap<String, Object> subhouses = new HashMap<>();
                subhouses.put("name", list.get(i).getName());
                subhouses.put("mobile", list.get(i).getMobile());
                subhouses.put("house image", list.get(i).getImg());
                subhousesMap.put(String.valueOf(i + 1), subhouses);
            }

            HashMap<String, Object> subhousesimg = new HashMap<>();
            for (int i = 0; i < bitmaps.size(); i++) {
                subhousesimg.put("img", bitmaps.get(i));
            }
        }

        if (isVisible.get()) {
            housesMap.put("servingCount", tv_totalhouse.getText().toString());
        }

        new Repository().sendHousesData(activity, countCheck, currentCardNumber, identityBitmap, houseImage, myPath, newMobiles, housesMap, subhousesMap, bitmaps, markingKey, jsonObject, dataObject, jsonObjectWard, preferences.getString("ward", ""),
                preferences.getString("userId", ""), preferences.getString("line", ""), preferences.getString("rfid", ""), preferences.getString("markingRevisit", "no"), currentDate).observeForever(dataSnapshot -> {

            if (dataSnapshot.equalsIgnoreCase("success")) {
                showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true, "survey", preferences.getString("cardNo", ""));
            }
        });
    }

    private void removeCardLocalData() {
        try {
            dataObject.remove(currentCardNumber);
            jsonObjectWard.put(preferences.getString("ward", ""), dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
        } catch (Exception e) {
        }
    }

    public void revisitBtn() {
        isVisibleTvRevisitNote.set(false);
        isVisibleBtnRevisit.set(false);
        isVisibleRevisitName.set(true);
        isVisibleBtnRevisitSave.set(true);
        isVisibleReasonRevisit.set(true);
        isVisibleRadioRevisit.set(true);
        isVisibleCardRevisit.set(true);
        identityBitmap = null;
//        houseImage = null;
    }

    public void saveRevisitBtn() {
        if (from.equalsIgnoreCase("map")) {
            common.showAlertBox("card number not found.", false, activity,activity);
        } else {
            if (preferences.getString("markingRevisit", "no").equalsIgnoreCase("no")) {
                sendRevisitData();
            } else {
                common.showAlertBox("Already revisit on marker.", false, activity,activity);
            }
        }
    }

    private void sendRevisitData() {
        activity.runOnUiThread(() -> {
            if (revisitNameTv.get().trim().length() == 0) {
                common.showAlertBox("Wrong assignment!\nPlease enter valid name.", false, activity,activity);
            } else if (spinnerRevisitReason.getSelectedItem().toString().equals("Select Reason type")) {
                common.closeDialog(activity);
                View selectedView = spinnerRevisitReason.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisitReason.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisitReason.performClick();
                }
                return;
            } else if (spinnerRevisitHouseType.getSelectedItem().toString().equals("Select Entity type")) {
                View selectedView = spinnerRevisitHouseType.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisitHouseType.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisitHouseType.performClick();
                }
                return;
            } else if (identityBitmap == null) {
                common.showAlertBox("कृपया पहले फोटो खींचे .", false, activity,activity);
            } else if (houseImage == null) {
                common.showAlertBox("कृपया पहले फोटो खींचे .", false, activity, activity);
            } else {
                common.setProgressBar("Please Wait...", activity, activity);
                HashMap<String, Object> data = new HashMap<>();
                try {
                    data.put("lat", preferences.getString("lat", ""));
                    data.put("lng", preferences.getString("lng", ""));
                    data.put("reason", spinnerRevisitReason.getSelectedItem().toString());
                    data.put("houseType", jsonArrayHouseTypeRevisit.get(spinnerRevisitHouseType.getSelectedItemPosition() - 1).toString());
                    data.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    data.put("id", preferences.getString("userId", ""));
                    data.put("revisitedBy", "Surveyor");
                    data.put("image", preferences.getString("cardNo", "") + ".jpg");
                    data.put("name", revisitNameTv.get());
                } catch (Exception e) {
                }
                new Repository().saveRevisitData(activity, data, identityBitmap, houseImage, preferences.getString("cardNo", "")).observeForever(dataSnapshot -> {
                    showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद !!!!", true, "revisit", preferences.getString("cardNo", ""));
                });
            }
        });
    }

}
