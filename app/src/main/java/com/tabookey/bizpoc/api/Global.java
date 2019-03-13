package com.tabookey.bizpoc.api;

import android.app.Application;

import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;

public class Global {
    public static IBitgoEnterprise ent;
    public static HttpReq http;

    static boolean is_test = true;

    public static boolean isTest() { return is_test; }

    public static void setAccessToken(String accessToken) {
        Global.accessToken = accessToken;
        ent = new BitgoEnterprise(Global.accessToken, isTest());
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static Application applicationContext;

    //    static String accessToken = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    private static String accessToken;
}
