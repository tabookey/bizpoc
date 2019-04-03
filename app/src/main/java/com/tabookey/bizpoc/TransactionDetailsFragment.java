package com.tabookey.bizpoc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TransactionDetailsFragment extends Fragment {

    public PendingApproval pendingApproval;
    Transfer transfer;
    HashMap<String, ExchangeRate> mExchangeRates;

    List<BitgoUser> guardians;
    IBitgoWallet ethWallet;

    View progressBarView;
    TextView senderAddressTextView;
    TextView senderTitleTextView;
    TextView recipientTitleTextView;
    TextView guardiansTitleTextView;
    RecyclerView guardiansRecyclerView;

    TextView recipientAddressTextView;
    TextView sendAmountTextView;
    TextView transactionCommentTextView;
    TextView transactionDateText;
    TextView validatorsTitle;
    Button transactionsHashButton;
    Button cancelTransaction;
    Button greatThanksButton;
    private AppCompatActivity mActivity;
    boolean showSuccessPopup = false;
    private View successPopup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transaction_details, container, false);

        senderAddressTextView = view.findViewById(R.id.senderAddressTextView);
        recipientAddressTextView = view.findViewById(R.id.recipientAddressTextView);
        sendAmountTextView = view.findViewById(R.id.sendAmount);
        senderTitleTextView = view.findViewById(R.id.senderTitleTextView);
        recipientTitleTextView = view.findViewById(R.id.recipientTitleTextView);
        guardiansTitleTextView = view.findViewById(R.id.guardiansTitleTextView);
        validatorsTitle = view.findViewById(R.id.validatorsTitle);
        transactionCommentTextView = view.findViewById(R.id.transactionCommentTextView);
        transactionsHashButton = view.findViewById(R.id.transactionsHashButton);
        guardiansRecyclerView = view.findViewById(R.id.guardiansRecyclerView);
        transactionDateText = view.findViewById(R.id.transactionDateText);
        successPopup = view.findViewById(R.id.successPopup);
        progressBarView = view.findViewById(R.id.progressBarView);
        cancelTransaction = view.findViewById(R.id.cancelTransaction);
        greatThanksButton = view.findViewById(R.id.greatThanksButton);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (transfer != null) {
            cancelTransaction.setVisibility(View.GONE);

            String dateFormat = DateFormat.format("MMMM dd, yyyy, hh:mm a", transfer.date).toString();
            transactionDateText.setText(dateFormat);
            if (transfer.txid != null) {
                transactionsHashButton.setVisibility(View.VISIBLE);
                transactionsHashButton.setOnClickListener(v -> {
                    String url = "https://kovan.etherscan.io/tx/" + transfer.txid;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                });
            }
            fillTransfer();
            mActivity.getSupportActionBar().setTitle("History");
        } else if (pendingApproval != null) {
            transactionsHashButton.setVisibility(View.GONE);
            String dateFormat = DateFormat.format("MMMM dd, yyyy, hh:mm a", pendingApproval.createDate).toString();
            transactionDateText.setText(dateFormat);
            cancelTransaction.setOnClickListener(v -> {
                AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
                dialog.setTitle("Are you sure you want to cancel the transaction?");
                dialog.setMessage("\nThis transaction will be cancelled and your guardians will be notified about this change\n\n");
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "   Yes, I'm sure   ",
                        (d, w) -> {
                            cancelTransaction();
                            dialog.dismiss();
                        });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "   Go back   ", (d, w) -> d.dismiss());
                dialog.show();
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);


                int pL = positiveButton.getPaddingLeft();
                int pT = positiveButton.getPaddingTop();
                int pR = positiveButton.getPaddingRight();
                int pB = positiveButton.getPaddingBottom();

                positiveButton.setBackgroundResource(R.drawable.custom_button);
                positiveButton.setPadding(pL, pT, pR, pB);

                positiveButton.setAllCaps(false);
                positiveButton.setTextColor(mActivity.getColor(android.R.color.white));


                negativeButton.setAllCaps(false);
                negativeButton.setTextColor(mActivity.getColor(R.color.text_color));
            });
            fillPending();
            mActivity.getSupportActionBar().setTitle("Pending");
        } else {
            throw new RuntimeException("No transaction object");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().post(() -> {
            if (showSuccessPopup) {
                mActivity.getSupportActionBar().hide();
                successPopup.setVisibility(View.VISIBLE);
                greatThanksButton.setOnClickListener(v -> {
                    mActivity.getSupportActionBar().show();
                    successPopup.setVisibility(View.GONE);
                });
            }
        });
    }

    private void cancelTransaction() {
        progressBarView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Global.ent.getMergedWallets().get(0).rejectPending(pendingApproval);
                ethWallet.update(null);
                mActivity.runOnUiThread(() -> {
                    progressBarView.setVisibility(View.GONE);
                    mActivity.onBackPressed();
                });
            } catch (Exception e) {
                mActivity.runOnUiThread(() -> {
                    progressBarView.setVisibility(View.GONE);
                    Utils.showErrorDialog(mActivity, "Error", e.getMessage());
                });
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

        List<Approval> collect = pendingApproval.getApprovals(guardians);
        guardiansRecyclerView.setHasFixedSize(true);
        guardiansRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        guardiansRecyclerView.setAdapter(new ApprovalsRecyclerAdapter(mActivity, collect, ApprovalState.WAITING));
        ExchangeRate exchangeRate = mExchangeRates.get(pendingApproval.token.type);
        double average24h = 0;
        if (exchangeRate != null) {
            average24h = exchangeRate.average24h;
        }
        sendAmountTextView.setText(String.format(Locale.US, "%.3f %s | %.2f USD", etherDouble, pendingApproval.token.getTokenCode().toUpperCase(), etherDouble * average24h));
        recipientAddressTextView.setText(pendingApproval.recipientAddr);
        transactionCommentTextView.setText(pendingApproval.comment);
        senderTitleTextView.setVisibility(View.GONE);
        senderAddressTextView.setVisibility(View.GONE);
        senderAddressTextView.setText(ethWallet.getAddress());
    }

    private void fillTransfer() {
        guardiansRecyclerView.setHasFixedSize(true);
        guardiansRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        validatorsTitle.setVisibility(View.GONE);
        // TODO: Not known if approved or rejected here
        List<Approval> collect = guardians.stream().map(g -> {
            ApprovalState state = ApprovalState.WAITING;
            if (transfer.cancelledBy != null && transfer.cancelledBy.equals(g.id)){
                state = ApprovalState.DECLINED;
            }
            if (transfer.approvals.contains(g.id)){
                state = ApprovalState.APPROVED;
            }
            return new Approval(g.name, state);
        }).collect(Collectors.toList());
        guardiansRecyclerView.setAdapter(new ApprovalsRecyclerAdapter(mActivity, collect, transfer.state));
        double etherDouble = Utils.integerStringToCoinDouble(transfer.valueString, transfer.token.decimalPlaces);

        double value = Math.abs(etherDouble);
        String valueFormat = String.format(Locale.US, "%.3f %s", value, transfer.token.getTokenCode().toUpperCase());
        if (transfer.usd != null) {
            String usd = transfer.usd.replaceAll("-", "");
            double dollarVal = Double.parseDouble(usd);
            valueFormat += String.format(Locale.US, " | %.2f USD", dollarVal);
        } else {
            ExchangeRate exchangeRate = mExchangeRates.get(transfer.token.type);
            if (exchangeRate != null) {
                valueFormat += String.format(Locale.US, " | %.2f USD", value * exchangeRate.average24h);
            }
        }
        sendAmountTextView.setText(valueFormat);
        transactionCommentTextView.setText(transfer.comment);
        boolean isOutgoingTx = transfer.valueString.contains("-");
        if (isOutgoingTx) {
            senderTitleTextView.setVisibility(View.GONE);
            senderAddressTextView.setVisibility(View.GONE);
            recipientAddressTextView.setText(transfer.remoteAddress);
        } else {

            guardiansTitleTextView.setVisibility(View.GONE);
            if(transfer.state == ApprovalState.APPROVED){
                guardiansRecyclerView.setVisibility(View.GONE);
            }
            validatorsTitle.setVisibility(View.GONE);


            recipientTitleTextView.setVisibility(View.GONE);
            recipientAddressTextView.setVisibility(View.GONE);

            senderTitleTextView.setVisibility(View.VISIBLE);
            senderAddressTextView.setText(transfer.remoteAddress);
        }
    }
}
