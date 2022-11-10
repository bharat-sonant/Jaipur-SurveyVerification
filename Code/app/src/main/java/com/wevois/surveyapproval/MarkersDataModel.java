package com.wevois.surveyapproval;

public class MarkersDataModel {
    boolean isAlreadyInstalled;
    boolean status;
    String date, imageName;
    int houseType;
    int markerNumber;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setAlreadyInstalled(boolean alreadyInstalled) {
        isAlreadyInstalled = alreadyInstalled;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setHouseType(int houseType) {
        this.houseType = houseType;
    }

    public boolean isAlreadyInstalled() {
        return isAlreadyInstalled;
    }

    public String getDate() {
        return date;
    }

    public String getImageName() {
        return imageName;
    }

    public int getHouseType() {
        return houseType;
    }

    public int getMarkerNumber() {
        return markerNumber;
    }

    public void setMarkerNumber(int markerNumber) {
        this.markerNumber = markerNumber;
    }

    public MarkersDataModel(boolean isAlreadyInstalled, boolean status, String date, String imageName, int houseType, int markerNumber) {
        this.isAlreadyInstalled = isAlreadyInstalled;
        this.status = status;
        this.date = date;
        this.imageName = imageName;
        this.houseType = houseType;
        this.markerNumber = markerNumber;
    }
}
