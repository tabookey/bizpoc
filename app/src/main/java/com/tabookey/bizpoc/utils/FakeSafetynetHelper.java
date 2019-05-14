package com.tabookey.bizpoc.utils;

import android.app.Activity;
import android.os.Handler;

import com.google.android.gms.tasks.OnFailureListener;

// This will be even more helpful shall we decide to write tests for the app
// In the meantime - for use in manual checks only.
public class FakeSafetynetHelper implements SafetynetHelperInterface {
    @Override
    public void sendSafetyNetRequest(Activity activity, byte[] bytes, OnSuccessParsedListener successListener, OnFailureListener failureListener) {
        activity.runOnUiThread(() -> new Handler().postDelayed(() -> {
            if (false && System.currentTimeMillis() % 2 == 0) {
                failureListener.onFailure(new RuntimeException("I don't wanna work! :'-( "));
            } else {
                String[] apkDigest = new String[1];
                apkDigest[0] = "whaat?";
                successListener.onSuccess(
                        new SafetyNetResponse("nonce",
                                1000,
                                "com.tabookey.bizpoc",
                                apkDigest,
                                "apkDigest",
                                "DefinitelyFakeResult",
                                false,
                                false));
            }

        }, 7000));
    }
}
