package com.tabookey.bizpoc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.impl.Utils;

import java.util.Arrays;

import static com.tabookey.bizpoc.SecretStorge.PREFS_API_KEY_ENCODED;
import static com.tabookey.bizpoc.SecretStorge.PREFS_PASSWORD_ENCODED;

//https://stackoverflow.com/questions/46875774/using-fingerprints-for-encryption-in-combination-with-a-password
public class ImportApiKeyFragment extends Fragment {

    private SecretStorge secretStorge = new SecretStorge();
    EditText et;

    public static class TokenPassword {
        @SuppressWarnings("WeakerAccess")
        public String token, password;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.import_api_key, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 11);
        }

        Button save = view.findViewById(R.id.saveApiKeyButton);
        Button test = view.findViewById(R.id.testApiKeyButton);
        Button scan = view.findViewById(R.id.scanApiKeyButton);
        et = view.findViewById(R.id.apiKeyEditText);
        TextView fingerprintTextView = view.findViewById(R.id.fingerprintEnabledTextView);
        TextView testNameTextView = view.findViewById(R.id.testNameTextView);
        FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            fingerprintTextView.setText("Device doesn't support fingerprint authentication");
            save.setEnabled(false);
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            save.setEnabled(false);
            fingerprintTextView.setText("User hasn't enrolled any fingerprints to authenticate with");
        }
        scan.setOnClickListener(v -> {
            startActivityForResult(new Intent(activity, ScanActivity.class), 1);
        });
        test.setOnClickListener(v -> {
            new Thread(() -> {
                String tokenPwdString = et.getText().toString();
                TokenPassword tokenPassword = Utils.fromJson(tokenPwdString, TokenPassword.class);
                Global.setAccessToken(tokenPassword.token);
                String name = Global.ent.getMe().name;
                activity.runOnUiThread(() -> {
                    testNameTextView.setText("wallet name is: " + name);
                });
            }).start();
        });
        save.setOnClickListener(v -> {
            String tokenPwdString = et.getText().toString();
            TokenPassword tokenPassword = Utils.fromJson(tokenPwdString, TokenPassword.class);
            try {
                byte[] encryptToken = secretStorge.encrypt(tokenPassword.token.getBytes());
                String encryptedToken = Arrays.toString(encryptToken);
                byte[] encryptPwd = secretStorge.encrypt(tokenPassword.password.getBytes());
                String encryptedPassword = Arrays.toString(encryptPwd);
                secretStorge.getPrefs(activity).edit()
                        .putString(PREFS_API_KEY_ENCODED, encryptedToken)
                        .putString(PREFS_PASSWORD_ENCODED, encryptedPassword)
                        .apply();

                FirstFragment f = new FirstFragment();
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            et.setText(data.getStringExtra("apiKey"));
        }
    }
}
