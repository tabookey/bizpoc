package com.tabookey.bizpoc.api;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.tabookey.bizpoc.MainActivity;
import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.CachedEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;

public class Global {
    public static MainActivity mainActivity;
    public static IBitgoEnterprise ent;
    public static HttpReq http;
    private static final String PREFS_IS_TEST = "istest";
    private static final String PREFS_SAFETYNET = "safetynet";
    private static final String PREFS_FAKE_SAFETYNET = "fake_safetynet";
    private static final String PREFS_ENVIRONMENT = "environment";
    private static final String PREFS_PROV_SERVER_URL = "prov_server_url";
    private static final String PREFS_CREDS_BEFORE_WOOHOO = "woohoo";


    public static boolean isTest() {
        return getPrefs().getBoolean(PREFS_IS_TEST, true);
    }

    public static void setIsTest(boolean isTest) {
        getPrefs().edit().putBoolean(PREFS_IS_TEST, isTest).apply();
    }

    public static void forgetAccessToken() {
        Global.accessToken = null;
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

    public static void setFakeSafetynet(boolean isFakeSafetynet) {
        getPrefs().edit().putBoolean(PREFS_FAKE_SAFETYNET, isFakeSafetynet).apply();
    }

    public static boolean getFakeSafetynet() {
        return getPrefs().getBoolean(PREFS_FAKE_SAFETYNET, false);
    }


    public static void setEnvironment(int environment) {
        getPrefs().edit().putInt(PREFS_ENVIRONMENT, environment).apply();
    }

    public static int getEnvironment() {
        return getPrefs().getInt(PREFS_ENVIRONMENT, 0);
    }

    //    static String accessToken = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    private static String accessToken;

    public static String getTestProvisionServer() {
        return getPrefs().getString(PREFS_PROV_SERVER_URL, "");
    }

    public static void setTestProvisionServer(String url) {
        getPrefs().edit().putString(PREFS_PROV_SERVER_URL, url).apply();
    }


    public static void setCredentialBeforeWoohoo(String credentialBeforeWoohoo) {
        getPrefs().edit().putString(PREFS_CREDS_BEFORE_WOOHOO, credentialBeforeWoohoo).apply();
    }

    public static String getCredentialBeforeWoohoo() {
        return getPrefs().getString(PREFS_CREDS_BEFORE_WOOHOO, "");
    }

}
