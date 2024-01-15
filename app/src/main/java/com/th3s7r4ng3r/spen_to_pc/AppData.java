package com.th3s7r4ng3r.spen_to_pc;
import com.google.gson.annotations.SerializedName;

public class AppData {
    // defining instance variables
    @SerializedName("androidVersion")
    private String androidVersion;
    @SerializedName("androidTitle")
    private String androidTitle;
    @SerializedName("androidMessage")
    private String androidMessage;
    @SerializedName("androidPositive")
    private String androidPositive;
    @SerializedName("windowsVersion")
    private String windowsVersion;
    @SerializedName("windowsTitle")
    private String windowsTitle;
    @SerializedName("windowsMessage")
    private String windowsMessage;
    @SerializedName("windowsPositive")
    private String windowsPositive;


    //defining getter methods
    public String getAndroidVersion(){
        return androidVersion;
    }
    public  String getAndroidMessage(){
        return  androidMessage;
    }
    public String getAndroidTitle() { return androidTitle;}
    public String getAndroidPositive() {return androidPositive;}

}
