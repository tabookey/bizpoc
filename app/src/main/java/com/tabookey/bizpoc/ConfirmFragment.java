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
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.impl.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConfirmFragment extends Fragment {
    private static final String PREFS_TXID_COUNTER = "prefs_txid_counter";
    private AppCompatActivity mActivity;
    TextView recipientAddress;
    TextView dollarEquivalent;
    TextView etherSendAmount;
    private SendRequest sendRequest;
    ExchangeRate exchangeRate;
    View progressBar;
    List<BitgoUser> guardians;
    IBitgoWallet mBitgoWallet;

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
            mActivity.getSupportActionBar().setTitle("Review");
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
        progressBar = view.findViewById(R.id.progressBar);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        fakeSubmitButton.setOnClickListener(v ->
        {
            TransactionDetailsFragment tdf = new TransactionDetailsFragment();
            tdf.pendingApproval = new PendingApproval("", new Date(), "", "", "", "", new ArrayList<BitgoUser>() {
            }, new BitgoUser("", "", ""), Global.ent.getTokens().get("teth"));
            goToDetails(tdf);
        });
        submit.setOnClickListener(v -> promptFingerprint(this::promptOtp));

        TokenInfo token = Global.ent.getTokens().get(sendRequest.coin);
        if (token == null) {
            throw new RuntimeException("No TokenInfo selected");
        }
        double etherDouble = Utils.integerStringToCoinDouble(sendRequest.amount, token.decimalPlaces);
        dollarEquivalent.setText(String.format(Locale.US, "%.2f USD", etherDouble * exchangeRate.average24h));
        etherSendAmount.setText(String.format(Locale.US, "%.3f ETH", etherDouble));
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
        progressBar.setVisibility(View.VISIBLE);
        new Thread() {
            @Override
            public void run() {
                try {
                    sendRequest.otp = otp;
                    sendRequest.walletPassphrase = password;
                    sendRequest.comment = getNewMemoID();
                    String pendingTxId = mBitgoWallet.sendCoins(sendRequest, null);
                    dollarEquivalent.post(() -> progressBar.setVisibility(View.GONE));

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
                        progressBar.setVisibility(View.GONE);
                        Utils.showErrorDialog(getActivity(), "Transaction failed!", e.getMessage());
                    });
                }
            }
        }.start();
    }

    private void goToDetails(TransactionDetailsFragment tdf) {
        tdf.showSuccessPopup = true;
        tdf.exchangeRate = exchangeRate;
        tdf.guardians = guardians;
        tdf.ethWallet = mBitgoWallet;
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
        activity.promptOtp((otp) -> sendTransaction(password, otp));
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