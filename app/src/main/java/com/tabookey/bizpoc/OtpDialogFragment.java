/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.tabookey.bizpoc;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.Global;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class OtpDialogFragment extends DialogFragment {

    private Button mCancelButton;
    private View mFingerprintContent;

    ConfirmFragment.PasswordCallback callback;
    Runnable cancelCallback;

    private ImageView mIcon;
    private TextView mErrorTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        //debug mode: if user doesn't have yubikey, use totp instead..
        if (BuildConfig.DEBUG && Global.isTest() &&
                Global.ent != null &&
                !Global.ent.getMe().hasOtp(BitgoUser.OtpType.yubikey) &&
                Global.ent.getMe().hasOtp(BitgoUser.OtpType.totp)) {
            new Thread(() -> {
                SystemClock.sleep(2000);
                // TODO: this will crash if click 'cancel' too soon.
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Google Authenticator 000000", Toast.LENGTH_LONG).show();
                    onOtpTagRecognised("000000");
                });
            }).start();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle("2-Factor authentication");
        View v = inflater.inflate(R.layout.otp_dialog_container, container, false);
        mCancelButton = v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(view -> {
                    if (cancelCallback != null) {
                        cancelCallback.run();
                    }
                    dismiss();
                }
        );
        mIcon = v.findViewById(R.id.fingerprint_icon);
        mErrorTextView = v.findViewById(R.id.fingerprint_status);
        mFingerprintContent = v.findViewById(R.id.fingerprint_container);
        mCancelButton.setText(R.string.cancel);
        mFingerprintContent.setVisibility(View.VISIBLE);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void onOtpTagRecognised(String result) {
        mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        mErrorTextView.setTextColor(
                mErrorTextView.getResources().getColor(R.color.success_color, null));
        mErrorTextView.setText("Yubikey NFC tag recognised");
        mIcon.postDelayed(() -> {
                    callback.run(result);
                    dismiss();
                },
                FingerprintUiHelper.SUCCESS_DELAY_MILLIS);
    }
}
