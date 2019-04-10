package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.impl.Utils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ConfirmFragment extends Fragment {
    private static final String TAG = "ConfirmFragment";
    private static final String PREFS_TXID_COUNTER = "prefs_txid_counter";
    private AppCompatActivity mActivity;
    TextView recipientAddress;
    TextView dollarEquivalent;
    TextView etherSendAmount;
    private SendRequest sendRequest;
    HashMap<String, ExchangeRate> mExchangeRates;
    View progressBarView;
    List<BitgoUser> guardians;
    IBitgoWallet mBitgoWallet;
    private ActionBar mActionBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.confirm_fragment, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
            mActionBar = mActivity.getSupportActionBar();
            if (mActionBar != null) {
                mActionBar.setTitle("Review");
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button submit = view.findViewById(R.id.submitButton);
        Button fakeSubmitButton = view.findViewById(R.id.fakeSubmitButton);
        recipientAddress = view.findViewById(R.id.recipientAddressTextView);
        dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        etherSendAmount = view.findViewById(R.id.etherSendAmount);
        progressBarView = view.findViewById(R.id.progressBarView);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        fakeSubmitButton.setOnClickListener(v ->
        {
            TransactionDetailsFragment tdf = new TransactionDetailsFragment();
            tdf.pendingApproval = new PendingApproval("", new Date(), "", "", "", "", Collections.emptyList(),
                    new BitgoUser("", "", ""), Global.ent.getTokens().get("teth"));
            goToDetails(tdf);
        });
        submit.setOnClickListener(v -> promptFingerprint(this::promptOtp));

        double etherDouble = Utils.integerStringToCoinDouble(sendRequest.amount, sendRequest.tokenInfo.decimalPlaces);
        ExchangeRate exchangeRate = mExchangeRates.get(sendRequest.tokenInfo.type);
        double average24h = 0;
        if (exchangeRate != null) {
            average24h = exchangeRate.average24h;
        }
        dollarEquivalent.setText(String.format(Locale.US, "%s USD", Utils.toMoneyFormat(etherDouble * average24h)));
        etherSendAmount.setText(String.format(Locale.US, "%.3f %s", etherDouble, sendRequest.tokenInfo.getTokenCode().toUpperCase()));
        recipientAddress.setText(sendRequest.recipientAddress);
    }

    interface PasswordCallback {
        @SuppressWarnings("unused")
        void run(String password);
    }

    private void promptFingerprint(PasswordCallback pc) {

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        String encryptedPassword = SecretStorage.getPrefs(activity).getString(SecretStorage.PREFS_PASSWORD_ENCODED, null);
        if (encryptedPassword == null) {
            Toast.makeText(activity, "Something wrong - password not saved?", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] array = SecretStorage.getEncryptedBytes(encryptedPassword);
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        fragment.mCryptoObject = SecretStorage.getCryptoObject(mActivity);
        fragment.input = array;
        fragment.title = "Authorize transaction";
        fragment.callback = new FingerprintAuthenticationDialogFragment.Callback() {
            @Override
            public void done(byte[] result) {
                String password = new String(result);
                pc.run(password);
            }

            @Override
            public void failed() {

            }
        };
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
    }

    public void setRequest(SendRequest sendRequest) {
        this.sendRequest = sendRequest;
    }

    private void sendTransaction(String password, String otp) {
        progressBarView.setVisibility(View.VISIBLE);
        mActionBar.hide();
        new Thread() {
            @Override
            public void run() {
                try {
                    sendRequest.otp = otp;
                    sendRequest.walletPassphrase = password;
                    sendRequest.comment = getNewMemoID();
                    String pendingTxId = mBitgoWallet.sendCoins(sendRequest, null);
                    mBitgoWallet.update(null);
                    dollarEquivalent.post(() -> {
                        mActionBar.show();
                        progressBarView.setVisibility(View.GONE);
                    });

                    TransactionDetailsFragment tdf = new TransactionDetailsFragment();
                    List<PendingApproval> pendingApprovals = mBitgoWallet.getPendingApprovals();
                    for (PendingApproval pa :
                            pendingApprovals) {
                        if (pa.id.equals(pendingTxId)) {
                            tdf.pendingApproval = pa;
                            break;
                        }
                    }
                    if (tdf.pendingApproval == null) {
                        throw new RuntimeException("No pending approval found!");
                    }
                    goToDetails(tdf);

                } catch (Throwable e) {
                    Log.e("TAG", "ex: ", e);
                    dollarEquivalent.post(() -> {
                        progressBarView.setVisibility(View.GONE);
                        mActionBar.show();
                        handleSenderException(e);
                    });
                }
            }
        }.start();
    }

    private void handleSenderException(Throwable e) {
        String message = e.getMessage();
        if (message.contains("the network is offline")) {
            Utils.showErrorDialog(getActivity(), "No connection", "Please check your internet connection and try again later");
        } else if (message.contains("Error: incorrect otp")) {
            Utils.showErrorDialog(getActivity(), "Wrong Yubikey", "The Yubikey dongle you have used is not valid.");
        } else if (message.contains("Error: insufficient balance")) {
            Utils.showErrorDialog(getActivity(), "Insufficient balance", "Your current balance seems to be lower than the amount that you have requested");
        } else if (message.contains("Error: invalid address")) {
            Utils.showErrorDialog(getActivity(), "Invalid address", "The destination address that you have specified is incorrect");
        } else if (message.contains("amount should match pattern") || message.contains("amount should be integer")) {
            Utils.showErrorDialog(getActivity(), "Wrong amount format", "The amount that you have specified does not correspond to the selected asset type");
        } else {
            Utils.showErrorDialog(getActivity(), "Transaction failed!", message);
            new Thread(() -> sendFailureToTabookeySlack(e)).start();
        }
    }

    private void sendFailureToTabookeySlack(Throwable e) {
        try {


            class Message {
                @SuppressWarnings("unused")
                public String text;

                @SuppressWarnings("WeakerAccess")
                public Message(String text) {
                    this.text = text;
                }
            }
            String response = Global.http.sendRequestNotBitgo("https://xycdl3ahgj.execute-api.eu-west-2.amazonaws.com/DEBUG", new Message("failed: " + e.getMessage()), "POST");
            if (!response.equals("ok")) {
                Log.e(TAG, "Failed to report a problem to the dev channel!");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void goToDetails(TransactionDetailsFragment tdf) {
        tdf.showSuccessPopup = true;
        tdf.mExchangeRates = mExchangeRates;
        tdf.guardians = guardians;
        tdf.mBitgoWallet = mBitgoWallet;
        mActivity.runOnUiThread(() -> {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, tdf, MainActivity.DETAILS_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
        });
    }


    private void promptOtp(String password) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }
        activity.promptOtp((otp) -> sendTransaction(password, otp), null);
    }


    /**
     * The bitgo IDs are not reliable and not visible to the guardians.
     * They require a way to tell similar transactions apart easily.
     *
     * @return a string that will be used as an ID by both client and guardians.
     * It will be saved in 'comment' field of the transaction.
     * ! Not reused between requests, threads - cannot generate 2 transactions with same id
     */
    private String getNewMemoID() {
        synchronized (this) {
            SharedPreferences prefs = SecretStorage.getPrefs(mActivity);
            int txidCounter = prefs.getInt(PREFS_TXID_COUNTER, 1);
            prefs.edit()
                    .putInt(PREFS_TXID_COUNTER, txidCounter + 1)
                    .apply();
            return String.format(Locale.US, "Txn#%03d", txidCounter);
        }
    }
}