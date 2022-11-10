package com.wevois.surveyapproval;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import com.wevois.surveyapproval.databinding.ActivitySubFormPageBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class SubFormPageActivity extends AppCompatActivity {

    ActivitySubFormPageBinding binding;
    String ward,line,serialNo;
    String type, name, mobile,address;
    CommonFunctions common;
    SharedPreferences preferences;
    AlertDialog saveAlertBox;

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
            if (bundle.containsKey("address")) {
                address = bundle.getString("address");
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
            if (bundle.containsKey("ward")) {
                ward = bundle.getString("ward");
            }
            if (bundle.containsKey("line")) {
                line = bundle.getString("line");
            }
            if (bundle.containsKey("serail")) {
                serialNo = bundle.getString("serail");
            }
        }


        common = new CommonFunctions();
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        binding.BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        HashMap<String, Object> subhouses = new HashMap<>();
        subhouses.put("name", name);
        subhouses.put("mobile", mobile);
        subhouses.put("address", address);
        subhouses.put("house_type", type);
        binding.btnSaveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                common.setProgressDialog("", "Please Wait", SubFormPageActivity.this, SubFormPageActivity.this);
                runOnUiThread(() -> {
                    CommonFunctions.getInstance().getDatabaseForApplication(SubFormPageActivity.this).child("VerifiedHouses/" + ward + "/" + line + "/" + serialNo).updateChildren(subhouses).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            common.closeDialog(SubFormPageActivity.this);
                            Log.e("data","save sucess");
                            showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true, "survey");
//                            finish();
//                            removeLocalData("Houses");
//                            response.setValue(checkAllDataSend("Houses"));
                        }
                    });
                });

            }
        });

        binding.etName.setText(name);
        binding.etAddress.setText(address);
        binding.etType.setText(type);
        binding.etMobile.setText(mobile);
    }


    public void showAlertBox(String message, boolean surveyCompleted, String from) {
        common.closeDialog(SubFormPageActivity.this);
        try {
            if (saveAlertBox != null) {
                saveAlertBox.dismiss();
            }
        } catch (Exception e) {
        }
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(SubFormPageActivity.this);
        alertAssignment.setMessage(message);
        alertAssignment.setCancelable(false);
        alertAssignment.setPositiveButton("OK", (dialog, id) -> {
            finish();
        });
        saveAlertBox = alertAssignment.create();
        if (!this.isFinishing()) {
            saveAlertBox.show();
        }
    }

}