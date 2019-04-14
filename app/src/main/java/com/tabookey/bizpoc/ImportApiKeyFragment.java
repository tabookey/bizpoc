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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.infideap.blockedittext.BlockEditText;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.impl.Utils;
import com.tabookey.bizpoc.utils.Crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.tabookey.bizpoc.SecretStorage.PREFS_API_KEY_ENCODED;
import static com.tabookey.bizpoc.SecretStorage.PREFS_PASSWORD_ENCODED;

//https://stackoverflow.com/questions/46875774/using-fingerprints-for-encryption-in-combination-with-a-password
public class ImportApiKeyFragment extends Fragment {

    private View progressBar;
    private SecretStorage secretStorage = new SecretStorage();
    private static String defApi = "{\"token\":\"v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8\", \"password\":\"asd/asd-ASD\"}";
    private MainActivity mActivity;
    private TextView testNameTextView;
    private View activationKeyView;
    private Button scanApiKeyButton;
    private Button submitButton;
    private BlockEditText blockEditText;
    private TextView invalidCodeWarning;

    private String mOtp;
    private Button useTestCredentialsButton;

    public static class TokenPassword {
        @SuppressWarnings("WeakerAccess")
        public String token, password;
        @SuppressWarnings("WeakerAccess")
        public boolean prod;
    }

    public static class RespResult {
        public String result;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.import_api_key, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scanApiKeyButton = view.findViewById(R.id.scanApiKeyButton);
        submitButton = view.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);
        submitButton.setBackgroundColor(mActivity.getColor(R.color.disabled_button));
        TextView fingerprintTextView = view.findViewById(R.id.fingerprintEnabledTextView);
        testNameTextView = view.findViewById(R.id.testNameTextView);
        activationKeyView = view.findViewById(R.id.activationKeyView);
        progressBar = view.findViewById(R.id.progressBar);
        blockEditText = view.findViewById(R.id.blockEditText);
        invalidCodeWarning = view.findViewById(R.id.invalidCodeWarning);
        useTestCredentialsButton = view.findViewById(R.id.useTestCredentialsButton);
        blockEditText.setTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String activationKey = blockEditText.getText();
                if (activationKey.length() == 16) {
                    try {
                        if (!verifyChecksum(activationKey)) {
                            invalidCodeWarning.setVisibility(View.VISIBLE);
                            return;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        invalidCodeWarning.setVisibility(View.VISIBLE);
                        return;
                    }
                    submitButton.setEnabled(true);
                    submitButton.setBackgroundColor(mActivity.getColor(R.color.colorPrimary));
                } else {
                    submitButton.setEnabled(false);
                    submitButton.setBackgroundColor(mActivity.getColor(R.color.disabled_button));
                }
                invalidCodeWarning.setVisibility(View.GONE);
            }
        });
        FingerprintManager fingerprintManager = (FingerprintManager) mActivity.getSystemService(Context.FINGERPRINT_SERVICE);
        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            fingerprintTextView.setText("Device doesn't support fingerprint authentication");
            scanApiKeyButton.setEnabled(false);
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            scanApiKeyButton.setEnabled(false);
            fingerprintTextView.setText("User hasn't enrolled any fingerprints to authenticate with");
        }
        scanApiKeyButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, 11);
                return;
            }
            startActivityForResult(new Intent(mActivity, ScanActivity.class), 1);
        });
        submitButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            hideKeyboard(mActivity);
            new Thread(() -> {
                boolean didRequest = false;
                boolean didReceiveValidResponse = false;
                try {
                    String resp = Global.http.sendRequestNotBitgo("getEncodedCredentials", null, "GET");
                    didRequest = true;
                    String encodedCredentials = Utils.fromJson(resp, JsonNode.class).get("encodedCredentials").toString();
                    if (encodedCredentials.length() > 0) {
                        didReceiveValidResponse = true;
                    }
                    String activationKey = blockEditText.getText();
                    byte[] scrypt = Crypto.scrypt(activationKey, "pepper");
                    char[] scryptPwdChar = new char[32];
                    for (int i = 0; i < 32; i++) {
                        scryptPwdChar[i] = (char) scrypt[i];
                    }
                    String decryptedInfo = Crypto.decrypt(encodedCredentials, scryptPwdChar);

                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(mActivity, decryptedInfo, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    boolean finalDidReceiveValidResponse = didReceiveValidResponse;
                    boolean finalDidRequest = didRequest;
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        String title;
                        String message;
                        if (finalDidReceiveValidResponse) {
                            title = "Activation failed";
                            message = "Contact us at support@tabookey.com";
                        } else if (finalDidRequest) {
                            title = "Activation failed";
                            message = "Contact us at support@tabookey.com";
                        } else {
                            title = "No connection";
                            message = "Please check your internet connection or try again later";
                        }
                        Utils.showErrorDialog(mActivity, title, message, null);
                    });
                }
            }).start();
        });

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void init() {
        Runnable cancelCallback = () -> mActivity.finish();
        ConfirmFragment.PasswordCallback passwordCallback = new ConfirmFragment.PasswordCallback() {
            @Override
            public void run(String otp) {

                progressBar.setVisibility(View.VISIBLE);
                hideKeyboard(mActivity);
                new Thread(() ->
                {
                    String resultCheckBitgo = Global.http.sendRequestNotBitgo("checkYubikeyExists", null, "GET");
                    RespResult resp = Utils.fromJson(resultCheckBitgo, RespResult.class);
                    if (resp.result.equals("ok")) {
                        mActivity.runOnUiThread(() -> {
                            mOtp = otp;
                            progressBar.setVisibility(View.GONE);
                            activationKeyView.setVisibility(View.VISIBLE);
                            submitButton.setVisibility(View.VISIBLE);

                            if (BuildConfig.DEBUG) {
                                useTestCredentialsButton.setVisibility(View.VISIBLE);
                                useTestCredentialsButton.setOnClickListener(v -> {
                                    Intent data = new Intent();
                                    data.putExtra(ScanActivity.SCANNED_STRING_EXTRA, defApi);
                                    onActivityResult(0, Activity.RESULT_OK, data);
                                });
                                scanApiKeyButton.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        mActivity.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Utils.showErrorDialog(getActivity(), "Wrong Yubikey", "The Yubikey dongle you have used is not valid.", ()->mActivity.promptOtp(this, cancelCallback));
                        });
                    }
                }).start();
            }
        };
        mActivity.promptOtp(passwordCallback, cancelCallback);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (MainActivity) context;
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String tokenPwdString = data.getStringExtra(ScanActivity.SCANNED_STRING_EXTRA);
            progressBar.setVisibility(View.VISIBLE);
            hideKeyboard(mActivity);
            new Thread(() -> {
                try {
                    TokenPassword tokenPassword = Utils.fromJson(tokenPwdString, TokenPassword.class);
                    Global.setIsTest(!tokenPassword.prod);
                    Global.setAccessToken(tokenPassword.token);
                    String name = Global.ent.getMe().name;
                    IBitgoWallet wallet = Global.ent.getMergedWallets().get(0);

                    if (!wallet.checkPassphrase(tokenPassword.password))
                        throw new RuntimeException("Invalid QRcode\nwallet: " + wallet.getLabel() + "\nUser: " + name);
                    byte[] encryptToken = secretStorage.encrypt(tokenPassword.token.getBytes());
                    String encryptedToken = Arrays.toString(encryptToken);
                    byte[] encryptPwd = secretStorage.encrypt(tokenPassword.password.getBytes());
                    String encryptedPassword = Arrays.toString(encryptPwd);
                    SecretStorage.getPrefs(mActivity).edit()
                            .putString(PREFS_API_KEY_ENCODED, encryptedToken)
                            .putString(PREFS_PASSWORD_ENCODED, encryptedPassword)
                            .apply();
                    mActivity.runOnUiThread(() -> {
                        WoohooFragment f = new WoohooFragment();
                        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Utils.showErrorDialog(getActivity(), "Error", e.getMessage(), null);
                    });
                }
            }).start();
        }
    }

    private boolean verifyChecksum(String activationKey) throws NoSuchAlgorithmException {
        String password = activationKey.substring(0, 12);
        String yubikeyId = mOtp.substring(0, 12);
        int checksumInput = Integer.parseInt(activationKey.substring(12, 16));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String toHash = password + yubikeyId;
        byte[] hash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
        byte[] checksumBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            checksumBytes[i] = hash[i];
        }
        int checksum = new BigInteger(checksumBytes).intValue() % 10000;
        return checksum == checksumInput;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
