package com.wevois.surveyapproval;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.wevois.surveyapproval.adapter.ParisarAdapter;
import com.wevois.surveyapproval.databinding.ActivityFormPageBinding;
import com.wevois.surveyapproval.repository.Repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FormPageActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 101;
    ActivityFormPageBinding binding;
    FormPageViewModel viewModel;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_CODE = 10;
    public ArrayList<SubHouseModel> list = new ArrayList<>();
    public ArrayList<Bitmap> bitmap_list = new ArrayList<>();
    ParisarAdapter parisarAdapter;
    SharedPreferences preferences;
    String from = "from", currentCardNumber;
    String mobileNumber = "";
    ArrayList<SubHouseModel> entitie_list = new ArrayList<>();
    List<String> houseTypeList = new ArrayList<>();
    JSONArray jsonArrayHouseType = new JSONArray();
    ArrayList<String> oldMobiles = new ArrayList<>();
    String countCheck = "2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_form_page);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_form_page);
        viewModel = ViewModelProviders.of(this).get(FormPageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding.setFormpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);

        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        currentCardNumber = preferences.getString("cardNo", "");

        binding.rcyParisarData.setLayoutManager(new LinearLayoutManager(FormPageActivity.this, LinearLayoutManager.VERTICAL, false));
        binding.etTotalHouse.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (binding.etTotalHouse.getText().toString().isEmpty()) {
                    binding.addMoreRow.setVisibility(View.GONE);
                    parisarAdapter = new ParisarAdapter(FormPageActivity.this, new ParisarAdapter.SubFormClick() {
                        @Override
                        public void onClickForm(int pos) {

                        }
                    });
                    binding.rcyParisarData.setAdapter(parisarAdapter);
                } else {
                    Log.e("pos ", "pos= ");
                    binding.addMoreRow.setVisibility(View.VISIBLE);
                    parisarAdapter = new ParisarAdapter(FormPageActivity.this, entitie_list, new ParisarAdapter.SubFormClick() {
                        @Override
                        public void onClickForm(int pos) {
                            Log.e("pos ", "pos= " + pos);
                            SubHouseModel model = parisarAdapter.getItemRow(pos);
                            Intent intent = new Intent(FormPageActivity.this, HouseDetailActivity.class);
                            intent.putExtra("pos", pos);
                            intent.putExtra("type", "edit");
                            intent.putExtra("name", model.name);
                            intent.putExtra("mobile", model.mobile);
                            startActivityForResult(intent, REQUEST_CODE);
                        }
                    });

                    binding.rcyParisarData.setAdapter(parisarAdapter);
                    viewModel.init(parisarAdapter);
                }
            }
        });

        binding.addMoreRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String house_count = binding.etTotalHouse.getText().toString();
                int count = Integer.parseInt(house_count);
                if (count > list.size()) {
                    Intent intent = new Intent(FormPageActivity.this, SubFormPageActivity.class);
                    intent.putExtra("pos", "1");
                    intent.putExtra("type", "add");
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(FormPageActivity.this, "आपने सबी फॉर्म भर लिए हैं", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (!from.equalsIgnoreCase("map")) {
            new Repository().checkSurveyDetailsIfAlreadyExists(this, currentCardNumber).observeForever(dataSnapshot -> {
                if (dataSnapshot != null) {
                    if (dataSnapshot.getValue() != null) {
                        int count = 0;
                        entitie_list.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                            Log.e("DataSnapshot",""+count);
                            for (DataSnapshot snapshot1 : snapshot.child("Entities").getChildren()) {
                                Log.e("Sub Houses value", "" + snapshot.child("Entities").child(snapshot1.getKey()).child("name").getValue());
//                                Log.e("Sub Houses value", "" + snapshot.child(snapshot1.getKey()).child("name").getValue());
                                count++;
                                SubHouseModel subHouseModel = new SubHouseModel();
                                subHouseModel.setName(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("name").getValue()));
                                subHouseModel.setMobile(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("mobile").getValue()));
                                subHouseModel.setImg(String.valueOf(snapshot.child("Entities").child(snapshot1.getKey()).child("house image").getValue()));
                                entitie_list.add(subHouseModel);
                            }

                            if (entitie_list.size() > 0) {
                                list = entitie_list;
                                binding.addMoreRow.setVisibility(View.VISIBLE);
//                                binding.etTotalHouse.setText(String.valueOf(count));
//                            totalHousesTv.set(String.valueOf(count));
                                parisarAdapter = new ParisarAdapter(FormPageActivity.this, entitie_list, new ParisarAdapter.SubFormClick() {
                                    @Override
                                    public void onClickForm(int pos) {
                                        Intent intent = new Intent(FormPageActivity.this, HouseDetailActivity.class);
                                        intent.putExtra("pos", pos);
                                        intent.putExtra("name", entitie_list.get(pos).getName());
                                        intent.putExtra("type", "edit");
                                        intent.putExtra("mobile", entitie_list.get(pos).getMobile());
                                        startActivityForResult(intent, REQUEST_CODE);
                                    }
                                });

                                binding.rcyParisarData.setAdapter(parisarAdapter);
                            }
                            if (snapshot.child("Entities").child("1") != null) {
                                Log.e("Sub Houses value", "" + snapshot.child("Entities").child("1").child("name").getValue());
                            }
                            if (snapshot.child("name").getValue() != null && snapshot.child("name").getValue().toString().length() > 0) {
                                binding.etName.setText(snapshot.child("name").getValue().toString());
                            }
                            if (snapshot.child("houseType").getValue() != null && snapshot.child("houseType").getValue().toString().length() > 0) {
                                if (Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 1 && Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 19) {
                                    commercialButtonClick();
                                } else {
                                    awasiyeButtonClick();
                                }
                                try {
                                    Log.e("Spinner type", "" + snapshot.child("houseType").getValue().toString());
                                    for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                                        if (jsonArrayHouseType.get(i).toString().equals(snapshot.child("houseType").getValue().toString())) {
                                            binding.spnrHouseType.setSelection(i + 1);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (snapshot.child("address").getValue() != null && snapshot.child("address").getValue().toString().length() > 0) {
                                binding.etAddress.setText(snapshot.child("address").getValue().toString());
                            }
                            if (snapshot.child("servingCount").getValue() != null && snapshot.child("servingCount").getValue().toString().length() > 0) {
                                binding.etTotalHouse.setText(snapshot.child("servingCount").getValue().toString());
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
                                binding.etMobile.setText(mobileNumber);
                            }
                        }
                    }
                }
            });
            new Repository().checkRfidAlreadyExists(this, preferences.getString("ward", ""), preferences.getString("line", ""), preferences.getString("rfid", "")).observeForever(dataSnapshot -> {
                if (dataSnapshot != null) {
                    if (dataSnapshot.getValue() != null) {
                        countCheck = "1";
                    } else {
                        countCheck = "2";
                    }
                }
            });
        }

        FormPageActivity formPageActivity = new FormPageActivity();
        viewModel.init(this, formPageActivity, binding.etTotalHouse, binding.addMoreRow, binding.rcyParisarData, binding.imgCard, binding.imgHouse, binding.spnrHouseType, getIntent().getStringExtra("from"), binding.spnrHouseTypeCardRevisit, binding.spnrReason);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.showAlertDialog(true);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.showAlertDialog(false);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        viewModel.onBack();
    }

    public void awasiyeButtonClick() {
//        isChecked.set(false);
//        isCheckedAwasiye.set(true);
        binding.radioAwasiye.setChecked(true);
        binding.radioCom.setChecked(false);
        getHouseTypes(false);
    }

    public void commercialButtonClick() {
//        isChecked.set(true);
//        isCheckedAwasiye.set(false);
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
        binding.spnrHouseType.setAdapter(spinnerArrayAdapter);
        binding.spnrHouseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

//                    String hintText = "";
//                isVisible.set(false);
                Log.e("form page activity ", "method call");
                binding.etTotalHouse.setVisibility(View.GONE);
//                binding.addMoreRow.setVisibility(View.GONE);
//                binding.etTotalHouse.setText("");
                /*if (list.size() > 0) {
                    list.clear();
                    bitmap_list.clear();
                    parisarAdapter.notifyDataSetChanged();
                }*/
                try {
                    switch (Integer.parseInt(jsonArrayHouseType.get(position - 1).toString())) {
                        case 19:
//                            hintText = "Enter No of Houses";
//                            isVisible.set(true);
                            binding.etTotalHouse.setVisibility(View.VISIBLE);
                            binding.rcyParisarData.setVisibility(View.VISIBLE);
                            binding.addMoreRow.setVisibility(View.GONE);
//                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            break;
                        case 20:
//                            hintText = "Enter No of Shops";
//                            isVisible.set(true);
                            binding.etTotalHouse.setVisibility(View.VISIBLE);
                            binding.rcyParisarData.setVisibility(View.VISIBLE);
                            binding.addMoreRow.setVisibility(View.GONE);
//                            rcy_parisar_data.setVisibility(View.VISIBLE);
                            break;
                        default:
                            binding.etTotalHouse.setVisibility(View.GONE);
                            binding.addMoreRow.setVisibility(View.GONE);
                            binding.rcyParisarData.setVisibility(View.GONE);
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

    public boolean alertDialogs() {
        new AlertDialog.Builder(FormPageActivity.this)
//                .setTitle("Delete entry")
                .setMessage("क्या आप परिसर का प्रकार बदलना चाहते हैं?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        return;
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            list = parisarAdapter.getItemRowdata();
            int count = 0;
            if (list != null) {
                count = list.size() + 1;
            }
            String name = data.getStringExtra("name");
            String mobile = data.getStringExtra("mobile");
//            String img = data.getStringExtra("house_img");
            String img = currentCardNumber + "Entities_" + count + ".jpg";
            byte[] byteArray = data.getByteArrayExtra("img_bitmap");
            Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            int pos = data.getIntExtra("pos", 0);
//            Bitmap bitmap = data.getParcelableExtra("img_bitmap");
            String type = data.getStringExtra("type");
            if (type.equals("edit")) {
                String img1 = currentCardNumber + "Entities_" + (pos + 1) + ".jpg";
                SubHouseModel model = new SubHouseModel();
                model.setName(name);
                model.setMobile(mobile);
                model.setImg(img1);
                parisarAdapter.editItem(model, pos);
                parisarAdapter.notifyDataSetChanged();
            } else {
                SubHouseModel model = new SubHouseModel();
                model.setName(name);
                model.setMobile(mobile);
                model.setImg(img);
                parisarAdapter.addItem(model, pos);
                parisarAdapter.notifyDataSetChanged();
            }
            list = parisarAdapter.getItemRowdata();
            bitmap_list.add(bmp);
            viewModel.init(list, bitmap_list);
            Log.e("name =", "size " + list.size());
        }
    }


    public ParisarAdapter getParisarAdapter() {

        return parisarAdapter;
    }

    public ArrayList<SubHouseModel> getSubHouseData() {

        return list;
    }
}