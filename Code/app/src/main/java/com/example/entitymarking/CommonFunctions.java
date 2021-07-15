package com.example.entitymarking;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.net.HttpURLConnection;
import java.net.URL;

public class CommonFunctions {
    AlertDialog.Builder builder;
    AlertDialog alertDialog;
    ProgressDialog dialog;
    int moveMarker = 1;
    Marker markerManOne, markerManTwo, markerManThree, markerManFour, markerManFive, markerManSix, markerManStop;
    public static final int LOCATION_REQUEST = 500;


    private SharedPreferences getDatabaseSp(Context context){
        return context.getSharedPreferences("LoginDetails",Context.MODE_PRIVATE);
    }

    public DatabaseReference getDatabaseRef(Context context){
        return FirebaseDatabase.getInstance(getDatabaseSp(context).getString("dbPath"," ")).getReference();
    }

    public StorageReference getDatabaseStoragePath(Context context){
        return FirebaseStorage.getInstance().getReferenceFromUrl(getDatabaseSp(context).getString("storagePath"," "));
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

    private void setVisibleMarker() {
        if (moveMarker == 1) {
            moveMarker = 2;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManOne.setVisible(true);
        } else if (moveMarker == 2) {
            moveMarker = 3;
            markerManStop.setVisible(false);
            markerManOne.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManTwo.setVisible(true);
        } else if (moveMarker == 3) {
            moveMarker = 4;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManOne.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManThree.setVisible(true);
        } else if (moveMarker == 4) {
            moveMarker = 5;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManOne.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManFour.setVisible(true);
        } else if (moveMarker == 5) {
            moveMarker = 6;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManOne.setVisible(false);
            markerManSix.setVisible(false);
            markerManFive.setVisible(true);
        } else {
            moveMarker = 1;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManOne.setVisible(false);
            markerManSix.setVisible(true);
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
        if (netInfo != null && netInfo.isConnected() && internetIsConnected()) {
            return true;
        } else {
            return false;
        }
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
}
