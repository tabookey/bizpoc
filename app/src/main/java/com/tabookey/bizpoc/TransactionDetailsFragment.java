package com.tabookey.bizpoc;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.Locale;

public class TransactionDetailsFragment extends Fragment {

    public PendingApproval pendingApproval;
    Transfer transfer;
    ExchangeRate exchangeRate;

    TextView senderNameTextView;
    TextView senderAddressTextView;

    TextView recipientAddressTextView;
    TextView etherSendAmountTextView;
    TextView dollarSentAmountTextView;
    TextView transactionCommentTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.transaction_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        senderNameTextView = view.findViewById(R.id.senderNameTextView);
        senderAddressTextView = view.findViewById(R.id.senderAddressTextView);
        recipientAddressTextView = view.findViewById(R.id.recipientAddressTextView);
        etherSendAmountTextView = view.findViewById(R.id.etherSendAmountTextView);
        dollarSentAmountTextView = view.findViewById(R.id.dollarSentAmountTextView);
        transactionCommentTextView = view.findViewById(R.id.transactionCommentTextView);
        if (transfer != null) {
            fillTransfer();
        } else if (pendingApproval != null) {
            fillPending();
        } else {
            throw new RuntimeException("No transaction object");
        }
        new Thread(() -> {
            IBitgoWallet ethWallet = Global.ent.getWallets("teth").get(0);
            String name = Global.ent.getMe().name;
            activity.runOnUiThread(() -> {
                senderNameTextView.setText(name);
                senderAddressTextView.setText(ethWallet.getAddress());

            });
        }).start();
    }

    private void fillPending() {
        double etherDouble = Utils.integerStringToCoinDouble(pendingApproval.amount, pendingApproval.token.decimalPlaces);
        etherSendAmountTextView.setText(String.format(Locale.US, "%.6f ETH", etherDouble));
        dollarSentAmountTextView.setText(String.format(Locale.US, "$%.2f USD", etherDouble * exchangeRate.average24h));
        recipientAddressTextView.setText(pendingApproval.recipientAddr);
        transactionCommentTextView.setText(pendingApproval.comment);
    }

    private void fillTransfer() {
        double etherDouble = Utils.integerStringToCoinDouble(transfer.valueString, transfer.token.decimalPlaces);
        etherSendAmountTextView.setText(String.format(Locale.US, "%.6f ETH", etherDouble));
        dollarSentAmountTextView.setText(String.format(Locale.US, "$%.2f USD", etherDouble * exchangeRate.average24h));
        recipientAddressTextView.setText(transfer.remoteAddress);
        transactionCommentTextView.setText(transfer.comment);
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}
