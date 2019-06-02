package com.tabookey.bizpoc;

import android.app.Application;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.logs.Log;

public class BizPocApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Global.applicationContext = this;
        if (Global.getEnvironment() == -1) {
            if (BuildConfig.DEBUG){
                Global.setEnvironment(1);
            }
            else {
                Global.setEnvironment(0);
            }
        }
        Log.initLogger(this.getFileStreamPath("logs").getAbsolutePath(), BuildConfig.DEBUG);
    }
}
