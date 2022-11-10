package com.wevois.surveyapproval;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText userIdEt, userPassWordEt;
    TextInputLayout userLayout, passwordLayout;
    Button loginBtn;
    SharedPreferences preferences;
    String userId = "", password = "";
    boolean isPass = true;
    DatabaseReference rootRef;
    CommonFunctions common = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setPageTitle();
        inIt();
    }

    private void inIt() {
        userIdEt = findViewById(R.id.username);
        userLayout = findViewById(R.id.userUsername);
        userPassWordEt = findViewById(R.id.password);
        passwordLayout = findViewById(R.id.userPassword);
        loginBtn = findViewById(R.id.login);
        rootRef = common.getDatabaseRef(LoginActivity.this);
        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        common.mFetchAlreadyInstalledCBHeading(LoginActivity.this);
    }

    private void setPageTitle() {
        findViewById(R.id.screen_title).setOnLongClickListener(view -> {
            preferences.edit().putString("dbPath", "https://dtdnavigatortesting.firebaseio.com/").apply();
//            preferences.edit().putString("dbPath", "https://dtdreengus.firebaseio.com/").apply();
            preferences.edit().putString("storagePath", "gs://dtdnavigator.appspot.com/Test/").apply();
//            preferences.edit().putString("storagePath", "Reengus").apply();
            rootRef = common.getDatabaseRef(LoginActivity.this);
            common.showAlertBox("Testing Mode Enabled", "Ok", "", LoginActivity.this);
            return false;
        });
    }

    @SuppressLint("StaticFieldLeak")
    public void onLoginClick(View view) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                common.setProgressDialog("Please wait...", " ", LoginActivity.this, LoginActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(LoginActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    if (editTextValidation()) {
                        checkUserIdAndPassword();
                    }
                } else {
                    common.showAlertBox("Please Connect to internet", "Ok", "", LoginActivity.this);
                }
                common.closeDialog(LoginActivity.this);
            }
        }.execute();
    }

    private boolean editTextValidation() {
        userId = String.valueOf(userIdEt.getText()).trim();
        password = String.valueOf(userPassWordEt.getText()).trim();
        if (userId.length() > 0) {
            if (password.length() > 0) {
                return true;
            } else {
                common.showAlertBox("Please Enter password", "ok", "", this);
                return false;
            }
        } else {
            common.showAlertBox("Please Enter Username", "ok", "", this);
            return false;
        }
    }

    private void checkUserIdAndPassword() {
        if (isPass) {
            isPass = false;
            common.setProgressDialog("Please Wait", "", LoginActivity.this, LoginActivity.this);
            rootRef.child("EntityMarkingData/MarkerAppAccess/").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                if (snapshot.hasChild("isActive")) {
                                    if (snapshot.hasChild("password")) {
                                        if (snapshot.hasChild("name")) {
                                            if (snapshot.hasChild("assignedWard")) {
                                                if (String.valueOf(snapshot.child("password").getValue()).equals(password)) {
                                                    if (Boolean.parseBoolean(String.valueOf(snapshot.child("isActive").getValue()))) {
                                                        preferences.edit().putString("userId", userId).apply();
                                                        preferences.edit().putString("assignment", String.valueOf(snapshot.child("assignedWard").getValue())).apply();
                                                        Intent intent = new Intent(LoginActivity.this, BleServiceActivity.class);
                                                        common.closeDialog(LoginActivity.this);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        isPass = true;
                                                        common.closeDialog(LoginActivity.this);
                                                        common.showAlertBox("InActive user", "ok", "", LoginActivity.this);
                                                    }
                                                } else {
                                                    isPass = true;
                                                    common.closeDialog(LoginActivity.this);
                                                    common.showAlertBox("Incorrect Password", "ok", "", LoginActivity.this);
                                                }
                                            } else {
                                                isPass = true;
                                                common.closeDialog(LoginActivity.this);
                                                common.showAlertBox("No Work Assigned", "ok", "", LoginActivity.this);
                                            }

                                        } else {
                                            isPass = true;
                                            common.closeDialog(LoginActivity.this);
                                            common.showAlertBox("name missing", "ok", "", LoginActivity.this);
                                        }
                                    } else {
                                        isPass = true;
                                        common.closeDialog(LoginActivity.this);
                                        common.showAlertBox("password missing", "ok", "", LoginActivity.this);
                                    }
                                } else {
                                    isPass = true;
                                    common.closeDialog(LoginActivity.this);
                                    common.showAlertBox("InActive User", "ok", "", LoginActivity.this);
                                }
                            } else {
                                isPass = true;
                                common.closeDialog(LoginActivity.this);
                                common.showAlertBox("Incorrect Username", "ok", "", LoginActivity.this);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }

}