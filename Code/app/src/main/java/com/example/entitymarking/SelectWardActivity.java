package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectWardActivity extends AppCompatActivity {
    CommonFunctions common = new CommonFunctions();
    List<WardNameModel> wardNameModelArrayList = new ArrayList<>();
    boolean isPass = true;
    WardAdapter adapter = new WardAdapter();

    @Override
    @SuppressLint("StaticFieldLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_ward);
        setPageTitle();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                common.setProgressDialog("Please wait...", " ", SelectWardActivity.this, SelectWardActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(SelectWardActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    fetchAllWadNames();
                } else {
                    common.closeDialog(SelectWardActivity.this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(SelectWardActivity.this);
                    builder.setMessage("Please Connect to internet").setCancelable(false)
                            .setPositiveButton("Ok", (dialog, id) -> {
                                Toast.makeText(SelectWardActivity.this, "Turn On Internet", Toast.LENGTH_SHORT).show();
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
        }.execute();
    }

    private void setPageTitle() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void fetchAllWadNames() {
        try {
            DatabaseReference dbRef = common.getDatabaseRef(SelectWardActivity.this);
            dbRef.child("Defaults/AvailableWard").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        wardNameModelArrayList = new ArrayList<>();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            if (!String.valueOf(dataSnapshot.getValue()).equals("BinLifting")) {
                                wardNameModelArrayList.add(new WardNameModel(String.valueOf(dataSnapshot.getValue())));
                            } else {
                                break;
                            }
                        }
                        ListView wrdNameListView = findViewById(R.id.ward_name_listview);
                        wrdNameListView.setAdapter(adapter);
                        common.closeDialog(SelectWardActivity.this);
                    } else {
                        common.closeDialog(SelectWardActivity.this);
                        Toast.makeText(SelectWardActivity.this, "No Data Found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static class WardNameModel {
        String name;

        public String getName() {
            return name;
        }

        public WardNameModel(String name) {
            this.name = name;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class WardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return wardNameModelArrayList.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint({"ViewHolder", "InflateParams"})
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.wrd_name_listview_layout, null, true);
            TextView wrdNameTv = view.findViewById(R.id.wrd_name_tv);
            WardNameModel model = wardNameModelArrayList.get(i);
            wrdNameTv.setText(model.getName());

            wrdNameTv.setOnClickListener(view1 -> {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        common.setProgressDialog("Please wait...", " ", SelectWardActivity.this, SelectWardActivity.this);
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        return common.network(SelectWardActivity.this);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            if (isPass) {
                                isPass = false;
                                Intent i1 = new Intent(SelectWardActivity.this, MapActivity.class);
                                i1.putExtra("ward", model.getName());
                                startActivity(i1);
                                finish();
                            }
                        } else {
                            common.showAlertBox("Please Connect to internet","Ok","",SelectWardActivity.this);
                        }
                        common.closeDialog(SelectWardActivity.this);
                    }
                }.execute();
            });

            return view;
        }
    }

}