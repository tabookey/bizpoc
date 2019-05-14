package com.tabookey.bizpoc.utils;

import android.app.Activity;

import com.google.android.gms.tasks.OnFailureListener;

public interface SafetynetHelperInterface {

    public interface OnSuccessParsedListener {
        void onSuccess(SafetyNetResponse response);
    }

    void sendSafetyNetRequest(Activity activity, byte[] nonce, SafetyNetHelper.OnSuccessParsedListener successListener, OnFailureListener failureListener);
}
