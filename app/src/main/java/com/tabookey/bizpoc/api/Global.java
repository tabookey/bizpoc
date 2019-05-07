package com.tabookey.bizpoc.api;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.CachedEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;
import com.tabookey.bizpoc.utils.SafetyNetResponse;

public class Global {
    public static IBitgoEnterprise ent;
    public static HttpReq http;
    private static final String PREFS_IS_TEST = "istest";
    private static final String PREFS_SAFETYNET = "safetynet";


    public static boolean isTest() {
        return getPrefs().getBoolean(PREFS_IS_TEST, true);
    }

    public static void setIsTest(boolean isTest) {
        getPrefs().edit().putBoolean(PREFS_IS_TEST, isTest).apply();
    }

    public static void setAccessToken(String accessToken) {
        Global.accessToken = accessToken;
        BitgoEnterprise netent = new BitgoEnterprise(Global.accessToken, isTest());
        CachedEnterprise cached = new CachedEnterprise(netent);
        ent = cached;
    }

    @SuppressLint("ApplySharedPref") // There will be 2 threads involved in read/write JWT token
    public static void setSafetynetResponseJwt(String safetynetResponseJwt) {
        getPrefs().edit().putString(PREFS_SAFETYNET, safetynetResponseJwt).commit();
        Global.lastSafetyNetResponseJwt = safetynetResponseJwt;
    }

    public static String getSafetynetResponseJwt() {
        return getPrefs().getString(PREFS_SAFETYNET, "");
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
    private static String lastSafetyNetResponseJwt;

}
