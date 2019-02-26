package com.tabookey.bizpoc.api;

import com.tabookey.bizpoc.impl.BitgoEnterprise;

public class Global {
    public static IBitgoEnterprise ent;

    static String accessKey = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";

    static {
        ent = new BitgoEnterprise(accessKey, true);
    }

}
