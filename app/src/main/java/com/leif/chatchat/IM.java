package com.leif.chatchat;

import android.app.Application;

/**
 * Created by leif on 6/1/16.
 */
public class IM extends Application {

//    public static final String HOST =  "172.28.137.120";
//    public static final String HOST =  "10.0.2.2";
//    public static final String HOST =  "192.168.191.5";
    public static final String HOST =  "172.29.25.156";
//    public static final String HOST =  "192.168.31.221";

    public static final int IMPORT =  5222;
    public static final int ONLINEPORT =  5223;
    public static final int FILEPORT =  8080;

    public static IM im;

    public static int account_id;
    public static String account, nickname, avatar, key, password, currentSession = null;

    public void onCreate() {
        super.onCreate();
        im = this;
    }
}
