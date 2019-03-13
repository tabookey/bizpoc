package com.tabookey.bizpoc;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tabookey.bizpoc.api.Global;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private View frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frame = findViewById(R.id.frame_layout);
        promptFingerprint();
    }

    private void promptFingerprint() {
        try {

            String encryptedApiKey = SecretStorge.getPrefs(this).getString(SecretStorge.PREFS_API_KEY_ENCODED, null);

            if (encryptedApiKey == null) {
                Fragment firstFragment = new ImportApiKeyFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.frame_layout, firstFragment).commit();
                return;
            }

            byte[] array = SecretStorge.getEncryptedBytes(encryptedApiKey);
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.mCryptoObject = SecretStorge.getCryptoObject();
            fragment.input = array;
            fragment.callback = apiKeyBytes -> {
                String apiKeyPlaintext = new String(apiKeyBytes);
                Global.setAccessToken(apiKeyPlaintext);
                Fragment firstFragment = new FirstFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.frame_layout, firstFragment).commit();
            };
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
        } catch (KeyPermanentlyInvalidatedException e) {
            e.printStackTrace();
        }
    }

    EditText input;
    ConfirmFragment.PasswordCallback otpCallback;
    AlertDialog dialog;

    public void promptOtp(ConfirmFragment.PasswordCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        this.otpCallback = callback;
        builder.setPositiveButton("OK", (a, b) -> {
            otpCallback.run(input.getText().toString());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        dialog = builder.show();
    }

    private static final Pattern otpPattern = Pattern.compile("^.*?([cbdefghijklnrtuv]{32,64})$");

    public void onNewIntent(Intent intent) {
        String data = intent.getDataString();
        if (data == null) {
            return;
        }
        Log.d("TAG", "data: " + data);
        Matcher matcher = otpPattern.matcher(data);
        if (matcher.matches()) {
            String otp = matcher.group(1);
            if (dialog != null && input != null && otpCallback != null) {
                input.setText(otp);

                new Handler().postDelayed(() -> {
                    otpCallback.run(otp);
                    dialog.dismiss();
                }, 1000);
            }
        } else {
            Toast.makeText(this, "No no no...", Toast.LENGTH_SHORT).show();
        }
    }
}
