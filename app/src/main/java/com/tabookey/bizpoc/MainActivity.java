package com.tabookey.bizpoc;

import android.os.Handler;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.tabookey.bizpoc.api.Global;

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
                Global.setApiKey(apiKeyPlaintext);
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
}
