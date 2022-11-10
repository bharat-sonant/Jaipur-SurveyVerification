package com.wevois.surveyapproval;

import android.app.Application;

import com.reader.ble.BU01_Reader;


public class BU01Application extends Application {

    //region BU01 BLE Reader
    private BU01_Reader bleReader;

    public BU01_Reader getBleReader() {
        return bleReader;
    }

    public void setBleReader(BU01_Reader bleReader) {
        this.bleReader = bleReader;
    }
    //endregion

    
    //region BU01U USB Reader
//    private BU01UsbReader usbReader;

//    public BU01UsbReader getUsbReader() {
//        return usbReader;
//    }

//    public void setUsbReader(BU01UsbReader usbReader) {
//        this.usbReader = usbReader;
//    }
    //endregion
}
