package com.example.entitymarking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.SpringAnimation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CommonFunctions {
    AlertDialog.Builder builder;
    AlertDialog alertDialog;
    ProgressDialog dialog;
    int moveMarker = 1;
    private float moveDistance[] = new float[1];
    LatLng previousLatLng;
    Marker markerManOne, markerManTwo, markerManThree, markerManFour, markerManFive, markerManSix, markerManStop;
    public static final int LOCATION_REQUEST = 500;


    public SharedPreferences getDatabaseSp(Context context) {
        return context.getSharedPreferences("LoginDetails", Context.MODE_PRIVATE);
    }

    public DatabaseReference getDatabaseRef(Context context) {
        return FirebaseDatabase.getInstance(getDatabaseSp(context).getString("dbPath", " ")).getReference();
    }

    public StorageReference getDatabaseStoragePath(Context context) {
        return FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/"+getDatabaseSp(context).getString("storagePath", " "));
    }

    public void showAlertBox(String message, String pBtn, String nBtn, Context ctx) {
        hideAlertBox();
        builder = new AlertDialog.Builder(ctx);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(pBtn, (dialog, id) -> dialog.cancel())
                .setNegativeButton(nBtn, (dialog, i) -> dialog.cancel());
        alertDialog = builder.create();
        alertDialog.show();
    }

    public void hideAlertBox() {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    public void setProgressDialog(String title, String message, Context context, Activity activity) {
        closeDialog(activity);
        dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.create();
        }
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (!dialog.isShowing() && !activity.isFinishing()) {
            dialog.show();
        }
    }

    public void closeDialog(Activity activity) {
        if (dialog != null) {
            if (dialog.isShowing() && !activity.isFinishing()) {
                dialog.dismiss();
            }
        }
    }

    public boolean locationPermission(Activity context) {
        boolean st = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
                st = false;
            } else {
                st = true;
            }
        }
        return st;
    }

    public BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getMinimumWidth(), vectorDrawable.getMinimumHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public boolean network(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected() && internetIsConnected();
    }

    public boolean internetIsConnected() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("https://google.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(10000);
            urlc.setReadTimeout(10000);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void setMovingMarker(GoogleMap mMap, LatLng currentLatLng, Context context) {

        previousLatLng = currentLatLng;

        markerManOne = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man1)));
        markerManTwo = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man2)));
        markerManThree = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man3)));
        markerManFour = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man4)));
        markerManFive = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man5)));
        markerManSix = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.man6)));
        markerManStop = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapFromVector(context.getApplicationContext(), R.drawable.manstop)));

        setOneToSixMarkerInvisible();
    }

    private void setOneToSixMarkerInvisible() {
        markerManOne.setVisible(false);
        markerManTwo.setVisible(false);
        markerManThree.setVisible(false);
        markerManFour.setVisible(false);
        markerManFive.setVisible(false);
        markerManSix.setVisible(false);
    }

    @SuppressLint("MissingPermission")
    public void currentLocationShow(LatLng currentLatLng) {
        Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude, previousLatLng.latitude, previousLatLng.longitude, moveDistance);
        if (moveDistance[0] > 2) {
            final LatLng startPosition = previousLatLng;
            final LatLng finalPosition = currentLatLng;
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final Interpolator interpolator = new AccelerateDecelerateInterpolator();
            final float durationInMs = 2000;
            handler.post(new Runnable() {
                long elapsed;
                float t;
                float v;

                @Override
                public void run() {
                    elapsed = SystemClock.uptimeMillis() - start;
                    t = elapsed / durationInMs;
                    v = interpolator.getInterpolation(t);
                    LatLng currentPosition = new LatLng(
                            startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                            startPosition.longitude * (1 - t) + finalPosition.longitude * t);

                    markerManStop.setPosition(currentPosition);
                    markerManOne.setPosition(currentPosition);
                    markerManTwo.setPosition(currentPosition);
                    markerManThree.setPosition(currentPosition);
                    markerManFour.setPosition(currentPosition);
                    markerManFive.setPosition(currentPosition);
                    markerManSix.setPosition(currentPosition);

                    if (t < 1) {
                        setVisibleMarker();
                        handler.postDelayed(this, 150);
                    } else {
                        moveMarker = 1;
                        setOneToSixMarkerInvisible();
                        markerManStop.setVisible(true);
                    }
                }
            });
            previousLatLng = currentLatLng;
        }
    }

    private void setVisibleMarker() {

        markerManStop.setVisible(false);
        setOneToSixMarkerInvisible();

        switch (moveMarker) {
            case 1:
                moveMarker = 2;
                markerManOne.setVisible(true);
                break;
            case 2:
                moveMarker = 3;
                markerManTwo.setVisible(true);
                break;
            case 3:
                moveMarker = 4;
                markerManThree.setVisible(true);
                break;
            case 4:
                moveMarker = 5;
                markerManFour.setVisible(true);
                break;
            case 5:
                moveMarker = 6;
                markerManFive.setVisible(true);
                break;
            default:
                moveMarker = 1;
                markerManSix.setVisible(true);
                break;

        }
    }

    public void increaseCountByOne(DatabaseReference tempRef) {
        tempRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue(String.valueOf((Integer.parseInt(currentData.getValue().toString())  + 1)));
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
            }
        });
    }

    public void decCountByOne(DatabaseReference tempRef) {
        tempRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue("");
                } else {
                    currentData.setValue(String.valueOf((Integer.parseInt(currentData.getValue().toString())  - 1)));
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
            }
        });
    }

    public void mFetchAlreadyInstalledCBHeading(Context context) {
        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/Common/EntityMarkingData/alreadyInstalledCheckBoxText.json")
                .getMetadata().addOnSuccessListener(storageMetadata -> {
                    long serverUpdation = storageMetadata.getCreationTimeMillis();
                    long localUpdation = getDatabaseSp(context).getLong("alreadyInstalledLastUpdate",0);
                    if (serverUpdation != localUpdation) {
                        getDatabaseSp(context).edit().putLong("alreadyInstalledLastUpdate", serverUpdation).apply();
                        try {
                            File local = File.createTempFile("temp","txt");
                            FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/Common/EntityMarkingData/alreadyInstalledCheckBoxText.json")
                                    .getFile(local).addOnCompleteListener(task -> {
                                        try {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(local)));
                                            StringBuilder sb = new StringBuilder();
                                            String str;
                                            while ((str = br.readLine()) != null) {
                                                sb.append(str);
                                            }
                                            getDatabaseSp(context).edit().putString("alreadyInstalledCbHeading", sb.toString().trim()).apply();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
    }

}
