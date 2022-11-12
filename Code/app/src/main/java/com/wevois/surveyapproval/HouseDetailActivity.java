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

import com.wevois.surveyapproval.databinding.HouseDetailActivityBinding;

import java.util.HashMap;

public class HouseDetailActivity extends AppCompatActivity {

//    ActivitySubFormPageBinding binding;
    HouseDetailActivityBinding binding;
    String ward,line,serialNo;
    String type, name, mobile,address;
    CommonFunctions common;
    SharedPreferences preferences;
    AlertDialog saveAlertBox;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.house_detail_activity);
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
                common.setProgressDialog("", "Please Wait", HouseDetailActivity.this, HouseDetailActivity.this);
                runOnUiThread(() -> {
                    CommonFunctions.getInstance().getDatabaseForApplication(HouseDetailActivity.this).child("SurveyVerifierData/VerifiedHouses/" + ward + "/" + line + "/" + serialNo).updateChildren(subhouses).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            common.closeDialog(HouseDetailActivity.this);
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

        binding.tvName.setText(name);
        binding.tvAddress.setText(address);
        binding.tvHouse.setText(type);
        binding.tvPhone.setText(mobile);
    }


    public void showAlertBox(String message, boolean surveyCompleted, String from) {
        common.closeDialog(HouseDetailActivity.this);
        try {
            if (saveAlertBox != null) {
                saveAlertBox.dismiss();
            }
        } catch (Exception e) {
        }
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(HouseDetailActivity.this);
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