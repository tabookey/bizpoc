package com.tabookey.bizpoc;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.tabookey.logs.Log;

import android.widget.Toast;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;
import com.tabookey.bizpoc.utils.FakeSafetynetHelper;
import com.tabookey.bizpoc.utils.SafetyNetHelper;
import com.tabookey.bizpoc.utils.SafetynetHelperInterface;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public static String TAG = "MainActivity";
    public static String DETAILS_FRAGMENT = "details_frag";
    public static String SEND_FRAGMENT = "send_frag";
    private FirstFragment mFirstFragment;
    private static boolean active = false;

    SafetynetHelperInterface mSafetyNetHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.drawable.tabookey_safe);

        Global.mainActivity = this;

        if (Global.getFakeSafetynet()) {
            mSafetyNetHelper = new FakeSafetynetHelper();
        } else {
            mSafetyNetHelper = new SafetyNetHelper();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        promptFingerprint();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Global.forgetAccessToken();
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;
        //TODO: temporarily, save logs on each suspend.
        // use "./logs.sh getzip" to get this logfile
        if (BuildConfig.DEBUG)
            Log.getZipLogsToSend(Log.getAppInfo(), 30 * 60);
    }

    //TODO: duplicate code for fingerprint. Optimize!!!
    public void promptFingerprint() {

        if (Global.getAccessToken() != null) {
            // TODO: should we re-prompt the fingerprint after onPause?
            Log.v(TAG, "Access token already decrypted");
            return;
        }

        String encryptedApiKey = SecretStorage.getPrefs(this).getString(SecretStorage.PREFS_API_KEY_ENCODED, null);

        if (encryptedApiKey == null) {
            Fragment welcomeFragment = new WelcomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, welcomeFragment).commit();
            return;
        }

        mSafetyNetHelper.sendSafetyNetRequest(this, null, response -> {
            // TODO: if attestation takes long time, it may arrive too late
            // this may lead to crashes (activity gone, etc.)
            if (!SafetyNetHelper.isAttestationLookingGood(response)) {
                onSafetynetFailure();
                return;
            }
            Global.setSafetynetResponseJwt(response.getJwsResult());
        }, exception -> onSafetynetFailure());
        byte[] array = SecretStorage.getEncryptedBytes(encryptedApiKey);
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        fragment.mCryptoObject = SecretStorage.getCryptoObject(this);
        if (fragment.mCryptoObject == null) {
            return;
        }
        fragment.input = array;
        fragment.title = getString(R.string.sign_in);
        fragment.cancelled = this::finish;
        fragment.callback = new FingerprintAuthenticationDialogFragment.Callback() {
            @Override
            public void done(byte[] apiKeyBytes) {
                if (!active) {
                    Log.e(TAG, "Activity is not in foreground, quitting!");
                    finish();
                    return;
                }
                String apiKeyPlaintext = new String(apiKeyBytes);
                Global.setAccessToken(apiKeyPlaintext);
                mFirstFragment = new FirstFragment();
                // If we re-scan fingerprint, we also re-show splash loading view
                FirstFragment.didShowSplashScreen = false;
                // Do not support background decryption - quit the app silently
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_layout, mFirstFragment).commit();
            }

            @Override
            public void failed(Throwable e) {
                e.printStackTrace();
                Utils.showErrorDialog(MainActivity.this, "Error", "Failed to decrypt the credentials. " +
                        "Reinstall the application if the problem does not resolve.", null);
            }
        };
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
    }

    OtpDialogFragment otpDialogfragment;

    public void promptOtp(ConfirmFragment.PasswordCallback callback, Runnable cancelCallback) {
        otpDialogfragment = new OtpDialogFragment();
        otpDialogfragment.callback = callback;
        otpDialogfragment.cancelCallback = cancelCallback;
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

    @SuppressLint("RestrictedApi")
    public void shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setShowHideAnimationEnabled(false);
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
        if (check_skip_send_flow()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!check_skip_send_flow()) {
            getSupportFragmentManager().popBackStack();
        }
        return true;
    }

    // After sending a transaction, we want to get to first screen when clicking 'back'
    private boolean check_skip_send_flow() {
        if (getSupportFragmentManager().findFragmentByTag(DETAILS_FRAGMENT) != null &&
                getSupportFragmentManager().findFragmentByTag(SEND_FRAGMENT) != null) {

            getSupportFragmentManager().popBackStack("to_send", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return true;
        }
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    public void openPendingDetails(Object item, HashMap<String, ExchangeRate> exchangeRates, List<BitgoUser> guardians, IBitgoWallet ethWallet) {
        if (item instanceof String) {
            return;
        }
        TransactionDetailsFragment tdf = new TransactionDetailsFragment();
        tdf.mExchangeRates = exchangeRates;
        tdf.guardians = guardians;
        tdf.mBitgoWallet = ethWallet;
        if (item instanceof PendingApproval) {
            tdf.pendingApproval = (PendingApproval) item;
        } else if (item instanceof Transfer) {
            tdf.transfer = (Transfer) item;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, tdf)
                .addToBackStack(null)
                .commit();
    }

    public void onSafetynetFailure() {
        Utils.showErrorDialog(this, "Safetynet", "Error", this::finish);
    }

    //TODO: duplicate code for fingerprint. Optimize!!!
    public void promptFingerprint(ConfirmFragment.PasswordCallback pc, byte[] input, String title) {
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        fragment.mCryptoObject = SecretStorage.getCryptoObject(this);
        fragment.input = input;
        fragment.title = title;
        fragment.callback = new FingerprintAuthenticationDialogFragment.Callback() {
            @Override
            public void done(byte[] result) {
                String password = new String(result);
                pc.run(password);
            }

            @Override
            public void failed(Throwable e) {
                e.printStackTrace();
                Log.e(TAG, "promptFingerprint failed");
            }
        };
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
    }

    public boolean isActive() {
        return active;
    }
}
