package com.tabookey.bizpoc;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        LinearLayout popup = findViewById(R.id.fingerprintPopupLinearLayout);
        SecretStorge secretStorge = new SecretStorge();
        String encryptedApiKey = secretStorge.getPrefs(this).getString(SecretStorge.PREFS_API_KEY_ENCODED, null);

        if (encryptedApiKey == null) {
            Fragment firstFragment = new ImportApiKeyFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, firstFragment).commit();
            return;
        }
        popup.setVisibility(View.VISIBLE);
        byte[] array = secretStorge.getEncryptedBytes(encryptedApiKey);
        try {
            secretStorge.decrypt(this, array, apiKeyBytes -> {
                popup.setVisibility(View.GONE);
                String apiKeyPlaintext = new String(apiKeyBytes);
                Global.setAccessToken(apiKeyPlaintext);
                popup.setBackgroundColor(0xff1122);
                new Handler().postDelayed(()->{
                    Fragment firstFragment = new FirstFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.frame_layout, firstFragment).commit();
                }, 1000);
            });
        } catch (KeyPermanentlyInvalidatedException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    EditText input;
    Dialog.OnClickListener positiveListener;
    AlertDialog dialog;
    public void promptOtp(Dialog.OnClickListener positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        this.positiveListener = positiveListener;
        builder.setPositiveButton("OK", positiveListener);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        dialog = builder.show();
    }

    private static final Pattern otpPattern = Pattern.compile("^.*?([cbdefghijklnrtuv]{32,64})$");
    public void onNewIntent(Intent intent) {
        // get the actual URI from the ndef tag
        String data = intent.getDataString();
        Log.d("TAG", "data: " + data);
        Matcher matcher = otpPattern.matcher(data);
        if(matcher.matches()) {
            // if the otp matched our regex open up a contextmenu
            String otp = matcher.group(1);
            if (dialog != null && input != null && positiveListener != null) {
                input.setText(otp);

                new Handler().postDelayed(()-> {
                    positiveListener.onClick(null, 0);
                    dialog.dismiss();
                }, 1000);
            }
        } else {
            Toast.makeText(this, "No no no...", Toast.LENGTH_SHORT).show();
        }
    }
}
