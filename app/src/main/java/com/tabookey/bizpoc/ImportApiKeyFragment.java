package com.tabookey.bizpoc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import com.tabookey.logs.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.fasterxml.jackson.databind.JsonNode;
import com.infideap.blockedittext.BlockEditText;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.impl.HttpReq;
import com.tabookey.bizpoc.impl.Utils;
import com.tabookey.bizpoc.utils.Crypto;
import com.tabookey.bizpoc.utils.FakeSafetynetHelper;
import com.tabookey.bizpoc.utils.SafetyNetHelper;
import com.tabookey.bizpoc.utils.SafetynetHelperInterface;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.tabookey.bizpoc.SecretStorage.PREFS_API_KEY_ENCODED;
import static com.tabookey.bizpoc.SecretStorage.PREFS_PASSWORD_ENCODED;

//https://stackoverflow.com/questions/46875774/using-fingerprints-for-encryption-in-combination-with-a-password
public class ImportApiKeyFragment extends Fragment {

    static String TAG = "importapikey";
    private View progressStepsDescriptionView;
    private SecretStorage secretStorage = new SecretStorage();
    private static String defApi = "{\"token\":\"v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8\", \"password\":\"asd/asd-ASD\"}";
    private MainActivity mActivity;
    private View activationKeyView;
    private Button scanApiKeyButton;
    private Button submitButton;
    private BlockEditText blockEditText;
    private TextView invalidCodeWarning;
    private CardView activationFailureCard;
    private View progressBar;
    private Button sendReportButton;

    private static final String DEBUG_PROVISION_SERVER_URL = "https://dprov-bizpoc.ddns.tabookey.com";
    private static final String PROD_PROVISION_SERVER_URL = "https://prov-bizpoc.ddns.tabookey.com";

    static String provisionServerUrl;

    private String mOtp;
    private Button useTestCredentialsButton;

    SafetynetHelperInterface mSafetyNetHelper;

    // Ordinals values in enum match indexes of animation views
    enum ActivationState {
        NOT_INITIATED,
        VERIFY_SAFETYNET,
        REQUESTING_CREDENTIALS,
        DECRYPTING_CREDENTIALS,
        CHECKING_BALANCE,
        VALIDATING_ACCOUNT,
        LOADING_HISTORY
    }

    private ActivationState mActivationState = ActivationState.NOT_INITIATED;
    private String mActivationError = null;
    private Throwable mActivationException = null;

    ArrayList<LottieAnimationView> mAnimationViews = new ArrayList<>();
    ArrayList<TextView> mProgressStepsText = new ArrayList<>();

    public static class TokenPassword {
        @SuppressWarnings("WeakerAccess")
        public String token, password;
        @SuppressWarnings("WeakerAccess")
        public boolean prod;

        @SuppressWarnings("unused")
        public TokenPassword() {
        }

        public TokenPassword(String token, String password, boolean prod) {
            this.token = token;
            this.password = password;
            this.prod = prod;
        }
    }

    public static class RespResult {
        public String result;
    }

    private void setActivationFailureReason(String error, Throwable exception) {
        mActivationError = error;
        mActivationException = exception;

        Log.e(TAG,"Activation failed: "+error, exception);
        Log.restartLogs();

        mActivity.runOnUiThread(() -> {
            int ordinal = mActivationState.ordinal();
            LottieAnimationView animationView = mAnimationViews.get(ordinal);
            TextView textView = mProgressStepsText.get(ordinal);
            textView.setTextColor(mActivity.getColor(R.color.reddish_brown));
            animationView.setImageResource(R.drawable.ic_cancelled);
            activationFailureCard.setVisibility(View.VISIBLE);
            if (BuildConfig.DEBUG) {
                Toast.makeText(mActivity, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setActivationState(ActivationState activationState) {
        mActivationState = activationState;
        mActivity.runOnUiThread(() -> {
            for (int i = 0; i < mAnimationViews.size(); i++) {
                int ordinal = mActivationState.ordinal();
                LottieAnimationView animationView = mAnimationViews.get(i);
                TextView textView = mProgressStepsText.get(i);
                if (i < ordinal) {
                    animationView.setImageResource(R.drawable.ic_checkmark_small);
                    textView.setTextColor(mActivity.getColor(R.color.text_color));
                } else if (i > ordinal) {
                    animationView.setImageResource(R.drawable.ic_circle_pending);
                } else {
                    animationView.setAnimation(R.raw.progress_loader);
                }
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Global.getFakeSafetynet()) {
            mSafetyNetHelper = new FakeSafetynetHelper();
        } else {
            mSafetyNetHelper = new SafetyNetHelper();
        }

        switch (Global.getEnvironment()) {
            case 2:
                provisionServerUrl = Global.getTestProvisionServer();
                break;
            case 1:
                provisionServerUrl = DEBUG_PROVISION_SERVER_URL;
                break;
            case 0:
            default:
                provisionServerUrl = PROD_PROVISION_SERVER_URL;
                break;
        }
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
        activationKeyView = view.findViewById(R.id.activationKeyView);
        progressStepsDescriptionView = view.findViewById(R.id.progressStepsDescriptionView);
        blockEditText = view.findViewById(R.id.blockEditText);
        invalidCodeWarning = view.findViewById(R.id.invalidCodeWarning);
        useTestCredentialsButton = view.findViewById(R.id.useTestCredentialsButton);
        activationFailureCard = view.findViewById(R.id.activationFailureCard);
        sendReportButton = view.findViewById(R.id.sendReportButton);
        progressBar = view.findViewById(R.id.progressBar);
        mAnimationViews.add(new LottieAnimationView(mActivity)); // Dull object at index 0
        mAnimationViews.add(view.findViewById(R.id.animationView1));
        mAnimationViews.add(view.findViewById(R.id.animationView2));
        mAnimationViews.add(view.findViewById(R.id.animationView3));
        mAnimationViews.add(view.findViewById(R.id.animationView4));
        mAnimationViews.add(view.findViewById(R.id.animationView5));
        mAnimationViews.add(view.findViewById(R.id.animationView6));
        mProgressStepsText.add(new TextView(mActivity)); // Dull object at index 0
        mProgressStepsText.add(view.findViewById(R.id.stepDescription1));
        mProgressStepsText.add(view.findViewById(R.id.stepDescription2));
        mProgressStepsText.add(view.findViewById(R.id.stepDescription3));
        mProgressStepsText.add(view.findViewById(R.id.stepDescription4));
        mProgressStepsText.add(view.findViewById(R.id.stepDescription5));
        mProgressStepsText.add(view.findViewById(R.id.stepDescription6));
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

        sendReportButton.setOnClickListener(v ->
        {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email"); // Normal SENDTO intents don't seem to pick up files. We will attach log files soon.
            String[] to = {"support@tabookey.com"};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Activation error - TabooKey Safe");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi there,\n\nActivation failed during step:\n" + getFailedStep() + "\n\nThank you,\nName: ");
            File file = Log.getZipLogsToSend(Log.getAppInfo(), 30*60);
            Uri uriForFile = FileProvider.getUriForFile(mActivity, "com.tabookey.bizpoc.fileprovider", file);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        });
        scanApiKeyButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, 11);
                return;
            }
            startActivityForResult(new Intent(mActivity, ScanActivity.class), 1);
        });
        submitButton.setOnClickListener(v -> {
            progressStepsDescriptionView.setVisibility(View.VISIBLE);
            hideKeyboard(mActivity);
            setActivationState(ActivationState.VERIFY_SAFETYNET);
            mSafetyNetHelper.sendSafetyNetRequest(mActivity, null, response -> {
                Global.setSafetynetResponseJwt(response.getJwsResult());
                if (SafetyNetHelper.isAttestationLookingGood(response)) {
                    new Thread(() -> {
                        String encodedCredentials = "";
                        boolean didRequest = false;
                        boolean didReceiveValidResponse = false;
                        try {
                            setActivationState(ActivationState.REQUESTING_CREDENTIALS);
                            String activationKeyInput = blockEditText.getText();
                            String checksum = "0000";
                            if (activationKeyInput.length() == 16) { // Only during manual testing
                                checksum = activationKeyInput.substring(12, 16);
                            }
                            String api = String.format(provisionServerUrl + "/getEncryptedCredentials/%s/%s", mOtp, checksum);
                            String safetynetJwt = response.getJwsResult();
                            Map<String, String> headers = new HashMap<>();
                            headers.put("x-safetynet", safetynetJwt);
                            String resp = HttpReq.sendRequestNotBitgo(api, null, "GET", headers);
                            didRequest = true;
                            setActivationState(ActivationState.DECRYPTING_CREDENTIALS);
                            JsonNode json = Utils.fromJson(resp, JsonNode.class).get("encryptedCredentials");
                            encodedCredentials = json.get("enc").toString();
                            Crypto.ScryptOptions scryptOptions = Utils.fromJson(json.get("scryptOptions").toString(), Crypto.ScryptOptions.class);
                            if (encodedCredentials.length() > 0) {
                                didReceiveValidResponse = true;
                            }
                            String activationKey = activationKeyInput.substring(0, 12);
                            byte[] scrypt = Crypto.scrypt(activationKey, scryptOptions);
                            String stringPasswBase64 = Crypto.toBase64(scrypt);
                            Log.e("scrypt", stringPasswBase64);
                            String decryptedInfo = Crypto.decrypt(encodedCredentials, stringPasswBase64.toCharArray());
                            saveTokenDataAndWoohoo(decryptedInfo);
                            if (BuildConfig.DEBUG) {
                                mActivity.runOnUiThread(() -> Toast.makeText(mActivity, decryptedInfo, Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            String message;
                            if (didReceiveValidResponse) {
                                message = "Did receive response from provisioning server, seems to be decryption error." + encodedCredentials;
                            } else if (didRequest) {
                                message = "Did make a request to provisioning server and response seems to be invalid.";
                            } else {
                                message = "Please check your internet connection or try again later";
                            }
                            setActivationFailureReason(message, e);
                        }
                    }).start();
                } else {
                    setActivationFailureReason("SafetyNet attestation does not look valid: " + response.getJwsResult(), null);
                }
            }, exception -> setActivationFailureReason("SafetyNet attestation request failed", exception));
        });

        init();
    }

    private String getFailedStep() {
        return mProgressStepsText.get(mActivationState.ordinal()).getText().toString();
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
                if (otp.equals("OFFLINE")) {
                    setYubikeyExistsState("fakafakefakafakefakafakefakafakefakafake");
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                hideKeyboard(mActivity);
                String encryptedCredentialsBefore = Global.getCredentialBeforeWoohoo();

                if (encryptedCredentialsBefore.length() != 0) {
                    mOtp = otp;
                    progressStepsDescriptionView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    setActivationState(ActivationState.VALIDATING_ACCOUNT);
                    ConfirmFragment.PasswordCallback decryptionCallback = result -> {
                        TokenPassword tokenPassword = Utils.fromJson(result, TokenPassword.class);
                        new Thread(() -> saveTokenDataAndWoohoo(Utils.toJson(tokenPassword))).start();
                    };
                    mActivity.promptFingerprint(decryptionCallback, SecretStorage.getEncryptedBytes(encryptedCredentialsBefore), "Resume activation");
                    return;
                }
                new Thread(() ->
                {
                    try {
                        String api = String.format(provisionServerUrl + "/checkYubikeyExists/%s", otp);
                        String resultCheckBitgo = HttpReq.sendRequestNotBitgo(api, null, "GET", null);
                        RespResult resp = Utils.fromJson(resultCheckBitgo, RespResult.class);
                        if (resp.result.equals("ok")) {
                            setYubikeyExistsState(otp);
                        } else {
                            mActivity.runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Utils.showErrorDialog(getActivity(), "Wrong Yubikey", "The Yubikey dongle you have used is not valid.", () -> mActivity.promptOtp(this, cancelCallback));
                            });
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        mActivity.runOnUiThread(() -> {
                            String errorMessage = "Contact us at support@tabookey.com";
                            if (e.getMessage().contains("Invalid otp/checksum")) {
                                errorMessage = "Invalid otp/checksum"; // TODO: come up with a real error message
                            }
                            Utils.showErrorDialog(getActivity(), "Error", errorMessage, () -> mActivity.promptOtp(this, cancelCallback));
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                }).start();
            }
        };
        mActivity.promptOtp(passwordCallback, cancelCallback);
    }

    private void setYubikeyExistsState(String otp) {
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
            progressStepsDescriptionView.setVisibility(View.VISIBLE);
            setActivationState(ActivationState.VALIDATING_ACCOUNT);
            hideKeyboard(mActivity);
            new Thread(() -> saveTokenDataAndWoohoo(tokenPwdString)).start();
        }
    }

    void saveTokenDataAndWoohoo(String tokenPwdString) {
        try {
            // First, save tekenPwd without verification
            // This is in case verification fails with some unrelated error
            byte[] encryptTokenPwd = secretStorage.encrypt(tokenPwdString.getBytes());
            String encryptedTokenPwd = Arrays.toString(encryptTokenPwd);
            Global.setCredentialBeforeWoohoo(encryptedTokenPwd);

            TokenPassword tokenPassword = Utils.fromJson(tokenPwdString, TokenPassword.class);
            Global.setIsTest(!tokenPassword.prod);
            Global.setAccessToken(tokenPassword.token);
            setActivationState(ActivationState.CHECKING_BALANCE);
            String name = Global.ent.getMe().name;
            IBitgoWallet wallet = Global.ent.getMergedWallets(null).get(0);

            setActivationState(ActivationState.VALIDATING_ACCOUNT);
            if (!wallet.checkPassphrase(tokenPassword.password))
                throw new RuntimeException("Invalid Activation Details\nwallet: " + wallet.getLabel() + "\nUser: " + name);
            byte[] encryptToken = secretStorage.encrypt(tokenPassword.token.getBytes());
            String encryptedToken = Arrays.toString(encryptToken);
            byte[] encryptPwd = secretStorage.encrypt(tokenPassword.password.getBytes());
            String encryptedPassword = Arrays.toString(encryptPwd);
            SecretStorage.getPrefs(mActivity).edit()
                    .putString(PREFS_API_KEY_ENCODED, encryptedToken)
                    .putString(PREFS_PASSWORD_ENCODED, encryptedPassword)
                    .apply();
            setActivationState(ActivationState.LOADING_HISTORY); // There is no distinct 6th step in this flow, but I will keep it just in case
            Global.setCredentialBeforeWoohoo("");
            Thread.sleep(500);
            mActivity.runOnUiThread(() -> {
                WoohooFragment f = new WoohooFragment();
                mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
            });
        } catch (Exception e) {
            setActivationFailureReason("Saving token seems to have failed", e);
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
        int checksum = Math.abs(new BigInteger(1, checksumBytes).intValue() % 10000);
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
