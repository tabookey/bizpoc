package com.tabookey.bizpoc.api;

import android.app.Application;

import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;

public class Global {
    public static IBitgoEnterprise ent;
    public static HttpReq http;

    static boolean is_test = true;

    public static boolean isTest() { return is_test; }

    public static void setApiKey(String apiKey) {
        Global.accessKey = apiKey;
        ent = new BitgoEnterprise(accessKey, isTest());
    }

    public static String getAccessToken() {
        return accessKey;
    }

    public static Application applicationContext;

    //    static String accessKey = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    private static String accessKey;
}
