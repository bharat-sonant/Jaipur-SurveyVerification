package com.wevois.surveyapproval;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;


public class CustomDialog {
    public static void showLowPower(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(context.getResources().getString(R.string.text_dialog_low_power));
        dialog.setPositiveButton(context.getResources().getString(R.string.text_dialog_btn_confirm), (dialog1, which) -> {
        });
        dialog.show();
    }

    public static void showTurnOnGPS(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(context.getResources().getString(R.string.text_dialog_turn_on_gps));
        dialog.setPositiveButton(context.getResources().getString(R.string.text_dialog_btn_confirm), (dialog1, which) -> {
        });
        dialog.show();
    }
}
