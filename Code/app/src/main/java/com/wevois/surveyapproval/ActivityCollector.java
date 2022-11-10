package com.wevois.surveyapproval;


import java.util.ArrayList;
import java.util.List;

class ActivityCollector {
    private static List<BleBaseActivity>  sActivities = new ArrayList<>();

    static void addActivity(BleBaseActivity activity){
        sActivities.add(activity);
    }

    static void removeActivity(BleBaseActivity activity){
        sActivities.remove(activity);
    }

    static void invalidateOptionsMenu(){
        for (BleBaseActivity activity: sActivities){
            activity.invalidateOptionsMenu();
        }
    }

    static void onReaderConnect(){
        for (BleBaseActivity activity : sActivities){
            activity.onReaderConnect();
        }
    }

    static void onReaderDisconnect(){
        for (BleBaseActivity activity : sActivities){
            activity.onReaderDisconnect();
        }
    }
}
