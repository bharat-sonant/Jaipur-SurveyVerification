package com.wevois.surveyapproval;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;


import com.wevois.surveyapproval.databinding.HouseDetailActivityBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class HouseDetailActivity extends AppCompatActivity {

    //    ActivitySubFormPageBinding binding;
    HouseDetailActivityBinding binding;
    String ward, line, serialNo;
    String type, name, userid, mobile, address, htype;
    CommonFunctions common;
    SharedPreferences preferences;
    List<String> houseTypeList = new ArrayList<>();
    JSONArray jsonArrayHouseType = new JSONArray();
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
        common = new CommonFunctions();
        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("userid")) {
                userid = bundle.getString("userid");
            }
            if (bundle.containsKey("mobile")) {
                mobile = bundle.getString("mobile");
            }
            if (bundle.containsKey("address")) {
                address = bundle.getString("address");
            }
            if (bundle.containsKey("userid")) {
                userid = bundle.getString("userid");
            }
            if (bundle.containsKey("type")) {
                type = bundle.getString("type");
            }

            if (bundle.containsKey("name")) {
                name = bundle.getString("name");
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
            if (bundle.containsKey("htype")) {
                htype = bundle.getString("htype");
            }
        }


        if (Integer.parseInt(htype) != 1 && Integer.parseInt(htype) != 19) {
            commercialBtnClick();
        } else {
            awasiyeBtnClick();
        }

        binding.BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        binding.btnSaveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (binding.houseTypeSpinner.getSelectedItemId() != 0) {
                    common.setProgressDialog("", "Please Wait", HouseDetailActivity.this, HouseDetailActivity.this);
                    HashMap<String, Object> subhouses = new HashMap<>();
                    subhouses.put("name", name);
                    subhouses.put("verifierId", userid);
                    subhouses.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                    subhouses.put("entityType", htype);

                    runOnUiThread(() -> {
                        CommonFunctions.getInstance().getDatabaseForApplication(HouseDetailActivity.this).child("SurveyVerifierData/VerifiedHouses/" + preferences.getString("wardno", "") + "/" + preferences.getString("lineno", "") + "/" + serialNo).updateChildren(subhouses).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                common.closeDialog(HouseDetailActivity.this);
                                Log.e("data", "save sucess");
                                showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true, "survey");
//                            finish();
//                            removeLocalData("Houses");
//                            response.setValue(checkAllDataSend("Houses"));
                            }
                        });

                        if (binding.radioAwasiye.isChecked()) {
                            type = "आवासीय";
                        } else {
                            type = "व्यावसायिक";
                        }

                        CommonFunctions.getInstance().getDatabaseForApplication(HouseDetailActivity.this).child("Houses/" + ward + "/" + line + "/" + serialNo + "/houseType").setValue(htype);
                        CommonFunctions.getInstance().getDatabaseForApplication(HouseDetailActivity.this).child("Houses/" + ward + "/" + line + "/" + serialNo + "/cardType").setValue(type);
                    });
                }else {
                    View selectedView = binding.houseTypeSpinner.getSelectedView();
                    if (selectedView != null && selectedView instanceof TextView) {
                        binding.houseTypeSpinner.requestFocus();
                        TextView selectedTextView = (TextView) selectedView;
                        selectedTextView.setError("error");
                        selectedTextView.setTextColor(Color.RED);
                        selectedTextView.setText("please select entity type");
                        binding.houseTypeSpinner.performClick();
                    }
                }

            }
        });

        binding.tvName.setText(name);
        binding.tvAddress.setText(address);
//        binding.tvHouse.setText(type);
        binding.tvPhone.setText(mobile);
//        binding.houseTypeSpinner.setSelection(0);
//        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        binding.houseTypeSpinner.setAdapter(spinnerArrayAdapter);
        binding.houseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (binding.houseTypeSpinner.getSelectedItemId() != 0) {
//                    onSaveClick(view);
                    try {
                        htype = jsonArrayHouseType.get(binding.houseTypeSpinner.getSelectedItemPosition() - 1).toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("Select house type", htype);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.radioCom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commercialBtnClick();
            }
        });

        binding.radioAwasiye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                awasiyeBtnClick();
            }
        });
    }

    public void awasiyeBtnClick() {
//        isChecked.set(false);
//        isCheckedAwasiye.set(true);
        binding.radioAwasiye.setChecked(true);
        binding.radioCom.setChecked(false);
        getHouseTypes(false);
    }

    public void commercialBtnClick() {
//        isChecked.set(true);
//        isCheckedAwasiye.set(false);
        Log.e("commercialBtnClick", "commercialBtnClick" + "");
        binding.radioCom.setChecked(true);
        binding.radioAwasiye.setChecked(false);
        getHouseTypes(true);
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

            Log.e("housesTypeList", "" + jsonObject.toString());
            for (int i = 1; i <= jsonObject.length(); i++) {

                if (isCommercial) {
                    try {
                        houseTypeList.add(commercialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                } else {
                    try {
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
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
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
        binding.houseTypeSpinner.setAdapter(spinnerArrayAdapter);

        try {

            for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                Log.e("houseeeeee", jsonArrayHouseType.get(i).toString() + " " + htype);
                if (jsonArrayHouseType.get(i).toString().equals(htype)) {
                    Log.e("houseeeeee", jsonArrayHouseType.get(i).toString() + " " + htype);
                    binding.houseTypeSpinner.setSelection(i + 1);
                }
            }
        } catch (Exception e) {

        }
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