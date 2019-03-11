package com.tabookey.bizpoc.api;

import com.tabookey.bizpoc.impl.BitgoEnterprise;
import com.tabookey.bizpoc.impl.HttpReq;

public class Global {
    public static IBitgoEnterprise ent;
    public static HttpReq http;

    public static void setApiKey(String apiKey) {
        Global.accessKey = apiKey;
        ent = new BitgoEnterprise(accessKey, true);
    }

    public static String getAccessKey() {
        return accessKey;
    }
    //    static String accessKey = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    private static String accessKey;
}
