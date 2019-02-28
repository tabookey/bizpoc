package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Global;

import java.util.Arrays;

//https://stackoverflow.com/questions/46875774/using-fingerprints-for-encryption-in-combination-with-a-password
public class ImportApiKeyFragment extends Fragment {

    private static final String PREFS_API_KEY_ENCODED = "api_key_encoded";
    private SecretStorge secretStorge = new SecretStorge();

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
        Button save = view.findViewById(R.id.saveApiKeyButton);
        Button test = view.findViewById(R.id.testApiKeyButton);
        EditText et = view.findViewById(R.id.apiKeyEditText);
        TextView fingerprintTextView = view.findViewById(R.id.fingerprintEnabledTextView);
        TextView testNameTextView = view.findViewById(R.id.testNameTextView);
        FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
        if (!fingerprintManager.isHardwareDetected()) {
            fingerprintTextView.setText("Device doesn't support fingerprint authentication");
            save.setEnabled(false);
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            save.setEnabled(false);
            fingerprintTextView.setText("User hasn't enrolled any fingerprints to authenticate with");
        }
        test.setOnClickListener(v -> {
            new Thread(() -> {
                String key = et.getText().toString();
                Global.setApiKey(key);
                String name = Global.ent.getMe().name;
                activity.runOnUiThread(() -> {
                    testNameTextView.setText("wallet name is: " + name);
                });
            }).start();
        });
        save.setOnClickListener(v -> {
            String key = et.getText().toString();
            try {
                byte[] encrypt = secretStorge.encrypt(key.getBytes());
                String encryptedApiKey = Arrays.toString(encrypt);
                secretStorge.getPrefs(activity).edit().putString(PREFS_API_KEY_ENCODED, encryptedApiKey).apply();
                FirstFragment f = new FirstFragment();
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
