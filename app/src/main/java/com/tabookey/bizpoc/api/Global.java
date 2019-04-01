package com.tabookey.bizpoc.api;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.CachedEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;

public class Global {
    public static IBitgoEnterprise ent;
    public static HttpReq http;


    public static boolean isTest() {
        return getPrefs().getBoolean("istest", true);
    }

    public static void setIsTest(boolean isTest) {
        getPrefs().edit().putBoolean("istest", isTest).apply();
    }

    public static void setAccessToken(String accessToken) {
        Global.accessToken = accessToken;
        BitgoEnterprise netent = new BitgoEnterprise(Global.accessToken, isTest());
        CachedEnterprise cached = new CachedEnterprise(netent);
        ent = cached;
    }

    private static SharedPreferences getPrefs() {
        return applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static Application applicationContext;


    //    static String accessToken = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    private static String accessToken;
}
