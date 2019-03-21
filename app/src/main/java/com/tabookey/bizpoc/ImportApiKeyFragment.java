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
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.impl.Utils;

import java.util.Arrays;

import static com.tabookey.bizpoc.SecretStorage.PREFS_API_KEY_ENCODED;
import static com.tabookey.bizpoc.SecretStorage.PREFS_PASSWORD_ENCODED;

//https://stackoverflow.com/questions/46875774/using-fingerprints-for-encryption-in-combination-with-a-password
public class ImportApiKeyFragment extends Fragment {

    private View progressBar;
    private SecretStorage secretStorage = new SecretStorage();
    String defApi = "{\"token\":\"v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8\", \"password\":\"asd/asd-ASD\"}";
    private AppCompatActivity mActivity;
    private TextView testNameTextView;
    private Button scanApiKeyButton;

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

        scanApiKeyButton = view.findViewById(R.id.scanApiKeyButton);
        Button useTestCredentialsButton = view.findViewById(R.id.useTestCredentialsButton);
        TextView fingerprintTextView = view.findViewById(R.id.fingerprintEnabledTextView);
        testNameTextView = view.findViewById(R.id.testNameTextView);
        progressBar = view.findViewById(R.id.progressBar);
        FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            fingerprintTextView.setText("Device doesn't support fingerprint authentication");
            scanApiKeyButton.setEnabled(false);
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            scanApiKeyButton.setEnabled(false);
            fingerprintTextView.setText("User hasn't enrolled any fingerprints to authenticate with");
        }
        scanApiKeyButton.setOnClickListener(v -> {
            startActivityForResult(new Intent(activity, ScanActivity.class), 1);
        });
        useTestCredentialsButton.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(ScanActivity.SCANNED_STRING_EXTRA, defApi);
            onActivityResult(0, Activity.RESULT_OK, data);
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String tokenPwdString = data.getStringExtra(ScanActivity.SCANNED_STRING_EXTRA);
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    TokenPassword tokenPassword = Utils.fromJson(tokenPwdString, TokenPassword.class);
                    Global.setAccessToken(tokenPassword.token);
                    String name = Global.ent.getMe().name;
                    IBitgoWallet wallet = Global.ent.getMergedWallets().get(0);

                    if ( !wallet.checkPassphrase(tokenPassword.password) )
                        throw new RuntimeException("Invalid QRcode\nwallet: "+wallet.getLabel()+"\nUser: "+name);
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        scanApiKeyButton.setVisibility(View.GONE);
                        testNameTextView.setText("wallet name is: " + name);
                    });
                    byte[] encryptToken = secretStorage.encrypt(tokenPassword.token.getBytes());
                    String encryptedToken = Arrays.toString(encryptToken);
                    byte[] encryptPwd = secretStorage.encrypt(tokenPassword.password.getBytes());
                    String encryptedPassword = Arrays.toString(encryptPwd);
                    SecretStorage.getPrefs(mActivity).edit()
                            .putString(PREFS_API_KEY_ENCODED, encryptedToken)
                            .putString(PREFS_PASSWORD_ENCODED, encryptedPassword)
                            .apply();

                    Thread.sleep(800);
                    mActivity.runOnUiThread(() -> {
                        FirstFragment f = new FirstFragment();
                        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
                    });
                } catch (Exception e) {
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Utils.showErrorDialog(getActivity(), e.getMessage());
                    });
                }
            }).start();
        }
    }
}
