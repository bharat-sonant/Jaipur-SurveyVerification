package com.wevois.surveyapproval;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;


import com.wevois.surveyapproval.databinding.ActivitySubFormPageBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SubFormPageActivity extends AppCompatActivity {


    ActivitySubFormPageBinding binding;
    int pos;
    String type, name, mobile;
    CommonFunctions common;
    SharedPreferences preferences;
    String subhouse_name,subhouse_mobile;
    boolean isvalid;
    boolean isMoved = true;
    ArrayList<String> newMobiles;
    AlertDialog customTimerAlertBox, customTimerAlertBoxForImage;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    File myPath = null;
    File myHousePath = null;
    Bitmap identityBitmap = null;
    Bitmap houseImage = null;
    String currentCardNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sub_form_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding.setLifecycleOwner(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("pos")) {
                pos = bundle.getInt("pos");
            }

            if (bundle.containsKey("type")) {
                type = bundle.getString("type");
            }

            if (bundle.containsKey("name")) {
                name = bundle.getString("name");
            }
            if (bundle.containsKey("mobile")) {
                mobile = bundle.getString("mobile");
            }
        }

        if (type.equals("edit")) {

            binding.etName.setText("" + name);
            binding.etMobile.setText("" + mobile);
        }

        common = CommonFunctions.getInstance();
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        currentCardNumber = preferences.getString("cardNo", "");
        binding.BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.houseBtnImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        binding.btnSaveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (validateSurveyForm()) {
                    subhouse_name = binding.etName.getText().toString().trim();
                    subhouse_mobile = binding.etMobile.getText().toString().trim();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    houseImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    Intent output = new Intent();
                    output.putExtra("name", subhouse_name);
                    output.putExtra("mobile", subhouse_mobile);
                    output.putExtra("house_img", currentCardNumber+"Entities_"+pos+".jpg");
                    output.putExtra("pos", pos);
                    output.putExtra("type", type);
                    output.putExtra("img_bitmap",byteArray);
                    setResult(RESULT_OK, output);
                    finish();

                }
            }
        });
    }

    public void saveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                showAlertDialog(true);
            }
        }
    }

    public void showAlertDialog(boolean isHouses) {
        try {
            if (customTimerAlertBox != null) {
                customTimerAlertBox.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
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
                setCameraDisplayOrientation(SubFormPageActivity.this, 0, mCamera);
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
            common.setProgressBar("Processing...", this, SubFormPageActivity.this);
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
        if (!isFinishing()) {
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

    private void showAlertBoxForImage(Bitmap bitmap, boolean isHouses) {
        try {
            common.closeDialog(SubFormPageActivity.this);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        View dialogLayout = inflater.inflate(R.layout.image_view_layout, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
        if (bitmap != null) {
            markerImage.setImageBitmap(bitmap);
        }
        dialogLayout.findViewById(R.id.okeyBtn).setOnClickListener(view1 -> {
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }

            if (isHouses) {
                common.setProgressBar("Processing...", this, this);
                houseImage = bitmap;
                binding.imgHouse.setImageBitmap(houseImage);
                setOnLocalHouse();
            } else {
                common.setProgressBar("Processing...", this, this);
                houseImage = bitmap;
                setOnLocalHouse();
            }
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_view_btn);
        closeBtn.setOnClickListener(view1 -> {
            common.closeDialog(SubFormPageActivity.this);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
            isMoved = true;
        });
        customTimerAlertBoxForImage = alertDialog.create();
        if (!isFinishing()) {
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
                myHousePath = new File(root, currentCardNumber + "SubHouse" + ".jpg");
                Log.e("my house path",""+myHousePath);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myHousePath);
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
                common.closeDialog(SubFormPageActivity.this);
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
                common.closeDialog(SubFormPageActivity.this);
                return null;
            }
        }.execute();
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

    private boolean validateSurveyForm() {
        if (binding.etName.length() == 0) {
            common.showAlertBox("Wrong assignment!\nPlease enter valid name.", false, this,this);
            isvalid = false;

        }  else if (binding.etMobile.getText().toString().trim().length() == 0){
            newMobiles = new ArrayList<>();
            if (binding.etMobile.getText().toString().contains(",")) {
                String[] mobile = binding.etMobile.getText().toString().trim().split(",");
                for (int i = 0; i < mobile.length; i++) {
                    if (mobile[i].trim().length() != 10) {
                        common.showAlertBox("Wrong assignment!\nPlease enter correct mobile number.", false, this,this);
                        isvalid = false;
                        break;
                    } else {
                        newMobiles.add(mobile[i].trim());
                        isvalid = true;
                    }
                }
            } else {
                if (binding.etMobile.getText().toString().trim().length() != 10) {
                    common.showAlertBox("Wrong assignment!\nPlease enter correct mobile number.", false, this,this);
                    isvalid = false;
                } else {
                    newMobiles.add(binding.etMobile.getText().toString().trim());
                    isvalid = true;
                }
            }
        }else if (houseImage == null) {
            common.showAlertBox("कृपया पहले घर की फोटो खींचे .", false, this,this);
            isvalid = false;

        }else {
            isvalid = true;
        }
        return isvalid;
    }
}