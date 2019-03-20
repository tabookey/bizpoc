package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TransactionDetailsFragment extends Fragment {

    public PendingApproval pendingApproval;
    Transfer transfer;
    ExchangeRate exchangeRate;

    List<BitgoUser> guardians;
    IBitgoWallet ethWallet;

    View progressBar;
    TextView senderNameTextView;
    TextView senderAddressTextView;
    TextView guardiansApprovalTitle;
    ListView guardiansListView;
    View guardiansView;

    TextView recipientAddressTextView;
    TextView etherSendAmountTextView;
    TextView dollarSentAmountTextView;
    TextView transactionCommentTextView;
    TextView transactionDateText;
    Button transactionsHashButton;
    Button cancelTransaction;
    private AppCompatActivity mActivity;

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
        etherSendAmountTextView = view.findViewById(R.id.etherSendAmount);
        dollarSentAmountTextView = view.findViewById(R.id.dollarEquivalent);
        transactionCommentTextView = view.findViewById(R.id.transactionCommentTextView);
        transactionsHashButton = view.findViewById(R.id.transactionsHashButton);
        guardiansListView = view.findViewById(R.id.guardiansListView);
        transactionDateText = view.findViewById(R.id.transactionDateText);
        guardiansView = view.findViewById(R.id.guardiansView);
        guardiansApprovalTitle = view.findViewById(R.id.guardiansApprovalTitle);
        progressBar = view.findViewById(R.id.progressBar);
        cancelTransaction = view.findViewById(R.id.cancelTransaction);

        if (transfer != null) {
            transactionsHashButton.setVisibility(View.VISIBLE);
            cancelTransaction.setVisibility(View.GONE);
            guardiansView.setVisibility(View.GONE);
            guardiansApprovalTitle.setVisibility(View.GONE);

            String dateFormat = DateFormat.format("dd/MM/yy, hh:mm a", transfer.date).toString();
            transactionDateText.setText(dateFormat);
            transactionsHashButton.setOnClickListener(v -> {
                String url = "https://kovan.etherscan.io/tx/" + transfer.txid;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            });
            fillTransfer();
        } else if (pendingApproval != null) {
            transactionsHashButton.setVisibility(View.GONE);
            String dateFormat = DateFormat.format("dd/MM/yy, hh:mm a", pendingApproval.createDate).toString();
            transactionDateText.setText(dateFormat);
            cancelTransaction.setOnClickListener(v -> {
                AlertDialog dialog = new AlertDialog.Builder(activity).create();
                dialog.setTitle("Are you sure you want to canccel the transaction?");
                dialog.setMessage("This transaction will be cancelled (and your guardians will be notified about this change)");
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes, I'm sure",
                        (d, w) -> {
                            cancelTransaction();
                            dialog.dismiss();
                        });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Go back", (d, w) -> d.dismiss());
                dialog.show();
            });
            fillPending();
        } else {
            throw new RuntimeException("No transaction object");
        }
        String name = Global.ent.getMe().name;
        senderNameTextView.setText(name);
        senderAddressTextView.setText(ethWallet.getAddress());
    }

    private void cancelTransaction() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                IBitgoWallet ethWallet = Global.ent.getWallets("teth").get(0);
                ethWallet.rejectPending(pendingApproval);
                mActivity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    mActivity.onBackPressed();
                });
            } catch (Exception e) {
                Utils.showErrorDialog(mActivity, e.getMessage());
            }
        }).start();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    private void fillPending() {
        double etherDouble = Utils.integerStringToCoinDouble(pendingApproval.amount, pendingApproval.token.decimalPlaces);

        List<ApprovalsAdapter.Approval> collect = guardians.stream().map(b -> {
            boolean isApproved = false;

            for (BitgoUser user : pendingApproval.approvedByUsers) {
                if (user.email.equals(b.email))
                    isApproved = true;
            }
            return new ApprovalsAdapter.Approval(b.name, isApproved);

        }).collect(Collectors.toList());
        ApprovalsAdapter adapter = new ApprovalsAdapter(mActivity, 0, collect);
        guardiansListView.setAdapter(adapter);
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
}
