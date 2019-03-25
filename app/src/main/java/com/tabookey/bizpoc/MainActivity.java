package com.tabookey.bizpoc;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.tabookey.bizpoc.api.Global;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public static String DETAILS_FRAGMENT = "details_frag";
    public static String SEND_FRAGMENT = "send_frag";
    private FirstFragment mFirstFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.drawable.tabookey_safe);
        promptFingerprint();
    }

    public void promptFingerprint() {
        try {

            String encryptedApiKey = SecretStorage.getPrefs(this).getString(SecretStorage.PREFS_API_KEY_ENCODED, null);

            if (encryptedApiKey == null) {
                Fragment apiKeyFragment = new ImportApiKeyFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_layout, apiKeyFragment).commit();
                return;
            }

            byte[] array = SecretStorage.getEncryptedBytes(encryptedApiKey);
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.mCryptoObject = SecretStorage.getCryptoObject();
            fragment.input = array;
            fragment.title = getString(R.string.sign_in);
            fragment.cancelled = this::finish;
            fragment.callback = apiKeyBytes -> {
                String apiKeyPlaintext = new String(apiKeyBytes);
                Global.setAccessToken(apiKeyPlaintext);
                mFirstFragment = new FirstFragment();
                // If we re-scan fingerprint, we also re-show splash loading view
                FirstFragment.didShowSplashScreen = false;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_layout, mFirstFragment).commit();
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

    OtpDialogFragment otpDialogfragment;

    public void promptOtp(ConfirmFragment.PasswordCallback callback) {
        otpDialogfragment = new OtpDialogFragment();
        otpDialogfragment.callback = callback;
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        otpDialogfragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
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
            if (otpDialogfragment != null) {
                otpDialogfragment.onOtpTagRecognised(otp);
            }
        } else {
            Toast.makeText(this, "No no no...", Toast.LENGTH_SHORT).show();
        }
    }



    /* ****************************/

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(canGoBack);
        actionBar.setDisplayUseLogoEnabled(!canGoBack);
        if (canGoBack) {
            actionBar.show();
        } else {
            actionBar.hide();
        }

    }

    @Override
    public void onBackPressed() {
        if (check_skip_send_flow())
        {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!check_skip_send_flow())
        {
            getSupportFragmentManager().popBackStack();
        }
        return true;
    }

    // After sending a transaction, we want to get to first screen when clicking 'back'
    private boolean check_skip_send_flow() {
        if (getSupportFragmentManager().findFragmentByTag(DETAILS_FRAGMENT) != null &&
                getSupportFragmentManager().findFragmentByTag(SEND_FRAGMENT) != null){

            getSupportFragmentManager().popBackStack("to_send", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return true;
        }
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }
}
