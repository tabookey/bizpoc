package com.tabookey.bizpoc;

import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.impl.Utils;

import java.util.List;
import java.util.Locale;

public class ConfirmFragment extends Fragment {
    TextView recipientAddress;
    TextView dollarEquivalent;
    TextView etherSendAmount;
    private SendRequest sendRequest;
    private ExchangeRate exchangeRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.confirm_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button submit = view.findViewById(R.id.submitButton);
        recipientAddress = view.findViewById(R.id.recipientAddressTextView);
        dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        etherSendAmount = view.findViewById(R.id.etherSendAmount);

        ListView guardiansListView = view.findViewById(R.id.guardiansListView);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        adapter.addAll("liraz", "eli");
        guardiansListView.setAdapter(adapter);
        submit.setOnClickListener(v -> promptFingerprint(this::promptOtp));

        double etherDouble = Utils.weiStringToEtherDouble(sendRequest.amount);
        dollarEquivalent.setText(String.format(Locale.US, "$%.2f USD", etherDouble * exchangeRate.average24h));
        etherSendAmount.setText(String.format(Locale.US, "%.6f ETH", etherDouble));
        recipientAddress.setText(sendRequest.recipientAddress);
    }

    interface PasswordCallback {
        @SuppressWarnings("unused")
        void run(String password);
    }

    private void promptFingerprint(PasswordCallback pc) {
        try {

            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            String encryptedPassword = SecretStorge.getPrefs(activity).getString(SecretStorge.PREFS_PASSWORD_ENCODED, null);
            if (encryptedPassword == null) {
                Toast.makeText(activity, "Something wrong - password not saved?", Toast.LENGTH_LONG).show();
                return;
            }
            byte[] array = SecretStorge.getEncryptedBytes(encryptedPassword);
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.mCryptoObject = SecretStorge.getCryptoObject();
            fragment.input = array;
            fragment.title = "Authorize transaction";
            fragment.callback = result -> {
                String password = new String(result);
                pc.run(password);
            };
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                return;
            }
            fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
        } catch (KeyPermanentlyInvalidatedException e) {
            e.printStackTrace();
        }
    }

    public void setRequest(SendRequest sendRequest) {
        this.sendRequest = sendRequest;
    }

    private void sendTransaction(String password, String otp) {
        new Thread() {
            @Override
            public void run() {
                try {
                    IBitgoWallet w = Global.ent.getWallets("teth").get(0);
                    SendRequest req = new SendRequest(sendRequest.recipientAddress, sendRequest.comment, sendRequest.amount, otp, password);
                    String pendingTxId = Utils.fromJson(w.sendCoins(req, null), JsonNode.class).get("pendingApproval").get("id").asText();

                    TransactionDetailsFragment tdf = new TransactionDetailsFragment();
                    List<PendingApproval> pendingApprovals = w.getPendingApprovals();
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
                    getFragmentManager().beginTransaction()
                            .replace(R.id.frame_layout, tdf).commit();

                } catch (Throwable e) {
                    Log.e("TAG", "ex: ", e);
                }
            }
        }.start();
    }


    private void promptOtp(String password) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }
        activity.promptOtp((otp) -> sendTransaction(password, otp));
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}