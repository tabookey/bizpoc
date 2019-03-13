package com.tabookey.bizpoc;

import android.app.Application;

import com.tabookey.bizpoc.api.Global;

public class BizPocApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Global.applicationContext = this;
    }
}
