package com.th3s7r4ng3r.spen_to_pc;
import com.google.gson.annotations.SerializedName;

public class AppData {
    // defining instance variables
    @SerializedName("windowsVersion")
    private String windowsVersion;
    @SerializedName("windowsChangedLog")
    private String windowsChangedLog;
    @SerializedName("androidVersion")
    private String androidVersion;
    @SerializedName("androidChangedLog")
    private String androidChangedLog;


    //defining getter methods
    public String getAndroidVersion(){
        return androidVersion;
    }
    public String getWindowsChangedLog() {
        return windowsChangedLog;
    }
    public  String getAndroidChangedLog(){
        return  androidChangedLog;
    }

}
