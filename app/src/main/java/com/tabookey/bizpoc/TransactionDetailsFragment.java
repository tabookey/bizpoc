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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tabookey.bizpoc.impl.Utils.NBSP;

public class TransactionDetailsFragment extends Fragment {
    private static final String TAG = "DetailsFragment";

    public PendingApproval pendingApproval;
    Transfer transfer;
    HashMap<String, ExchangeRate> mExchangeRates;

    List<BitgoUser> guardians;
    IBitgoWallet mBitgoWallet;

    View progressBarView;
    Button senderAddressButton;
    TextView senderTitleTextView;
    TextView recipientTitleTextView;
    TextView guardiansTitleTextView;
    RecyclerView guardiansRecyclerView;

    Button recipientAddressButton;
    TextView sendAmountTextView;
    TextView transactionCommentTextView;
    TextView transactionDateText;
    TextView validatorsTitle;
    Button transactionsHashButton;
    Button cancelTransaction;
    Button greatThanksButton;
    ImageView popupImage;
    TextView popupTitle;
    private AppCompatActivity mActivity;
    private ActionBar mActionBar;
    boolean showSuccessPopup = false;
    private View successPopup;
    private View transactionCommentLabel;

    Thread refresher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transaction_details, container, false);

        senderAddressButton = view.findViewById(R.id.senderAddressButton);
        recipientAddressButton = view.findViewById(R.id.recipientAddressButton);
        sendAmountTextView = view.findViewById(R.id.sendAmount);
        senderTitleTextView = view.findViewById(R.id.senderTitleTextView);
        recipientTitleTextView = view.findViewById(R.id.recipientTitleTextView);
        guardiansTitleTextView = view.findViewById(R.id.guardiansTitleTextView);
        transactionCommentLabel = view.findViewById(R.id.transactionCommentLabel);
        validatorsTitle = view.findViewById(R.id.validatorsTitle);
        transactionCommentTextView = view.findViewById(R.id.transactionCommentTextView);
        transactionsHashButton = view.findViewById(R.id.transactionsHashButton);
        guardiansRecyclerView = view.findViewById(R.id.guardiansRecyclerView);
        transactionDateText = view.findViewById(R.id.transactionDateText);
        successPopup = view.findViewById(R.id.successPopup);
        progressBarView = view.findViewById(R.id.progressBarView);
        cancelTransaction = view.findViewById(R.id.cancelTransaction);
        greatThanksButton = view.findViewById(R.id.greatThanksButton);
        popupImage = view.findViewById(R.id.popupImage);
        popupTitle = view.findViewById(R.id.popupTitle);

        Utils.makeButtonCopyable(recipientAddressButton, mActivity);
        Utils.makeButtonCopyable(senderAddressButton, mActivity);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (transfer != null) {
            fillTransfer();
        } else if (pendingApproval != null) {
            fillPending();
        } else {
            throw new RuntimeException("No transaction object");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().post(() -> {
            if (showSuccessPopup) {
                showSuccessPopup = false;
                mActionBar.hide();
                successPopup.setVisibility(View.VISIBLE);
                greatThanksButton.setOnClickListener(v -> {
                    mActionBar.show();
                    successPopup.setVisibility(View.GONE);
                });
            }
        });
        if (refresher != null) {
            refresher.interrupt();
        }
        newRefresher();
        refresher.start();
    }

    private void cancelTransaction() {
        progressBarView.setVisibility(View.VISIBLE);
        mActionBar.hide();
        new Thread(() -> {
            try {
                Global.ent.getMergedWallets().get(0).rejectPending(pendingApproval);
                mBitgoWallet.update(null);
                mActivity.runOnUiThread(() -> {
                    mActionBar.show();
                    progressBarView.setVisibility(View.GONE);
                    showDeletedPopup();
                });
            } catch (Exception e) {
                mActivity.runOnUiThread(() -> {
                    mActionBar.show();
                    progressBarView.setVisibility(View.GONE);
                    Throwable cause = e.getCause();
                    if (cause instanceof UnknownHostException || cause instanceof SocketTimeoutException) {
                        Utils.showErrorDialog(getActivity(), "No connection", "Please check your internet connection and try again later", null);
                    } else {
                        Utils.showErrorDialog(mActivity, "Error", e.getMessage(), null);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            mActivity = (AppCompatActivity) context;
            mActionBar = mActivity.getSupportActionBar();
        }
    }

    private void fillPending() {
        if (pendingApproval == null) { // late callback hit too late
            return;
        }
        String approvalId = pendingApproval.id;
        Optional<PendingApproval> optionalApproval = mBitgoWallet.getPendingApprovals().stream().filter((a) -> a.id.equals(approvalId)).findAny();
        if (!optionalApproval.isPresent()) {
            // two possible reasons: transaction approved or declined.
            List<Transfer> transfers = mBitgoWallet.getTransfers(0);
            Optional<Transfer> optionalTransfer = transfers.stream().filter(a -> a.pendingApproval != null && a.pendingApproval.equals(approvalId)).findAny();
            if (optionalTransfer.isPresent()) {
                transfer = optionalTransfer.get();
                pendingApproval = null;
                refresher.interrupt();
                fillTransfer();
            } else {
                Log.e(TAG, "Pending transaction with approval ID " + approvalId + " seems to disappear!");
            }
            return;
        }

        pendingApproval = optionalApproval.get();
        Log.e(TAG, "New pending: " + pendingApproval.toString());

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
        mActionBar.setTitle("Pending");

        double etherDouble = Utils.integerStringToCoinDouble(pendingApproval.amount, pendingApproval.token.decimalPlaces);

        List<Approval> approvalList = pendingApproval.getApprovals(guardians);
        guardiansRecyclerView.setHasFixedSize(true);
        guardiansRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        StringBuilder appr = new StringBuilder();
        for (Approval a : approvalList) {
            appr.append(a.name).append(":").append(a.state).append(" ");
        }
        Log.e(TAG, "Approvers: " + appr);
        guardiansRecyclerView.setAdapter(new ApprovalsRecyclerAdapter(mActivity, approvalList, ApprovalState.WAITING));
        ExchangeRate exchangeRate = mExchangeRates.get(pendingApproval.token.type);
        double average24h = 0;
        if (exchangeRate != null) {
            average24h = exchangeRate.average24h;
        }
        sendAmountTextView.setText(String.format(Locale.US, "%.3f %s | %s"+NBSP+"USD", etherDouble, pendingApproval.token.getTokenCode().toUpperCase(), Utils.toMoneyFormat(etherDouble * average24h)));
        recipientAddressButton.setText(pendingApproval.recipientAddr);
        transactionCommentTextView.setText(pendingApproval.comment);
        senderTitleTextView.setVisibility(View.GONE);
        senderAddressButton.setVisibility(View.GONE);
        senderAddressButton.setText(mBitgoWallet.getAddress());
    }

    private void newRefresher() {
        refresher = new Thread(() -> {
            Log.e(TAG, "refresher thread started, ID:" + refresher.getId());
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(10000);
                    mBitgoWallet.update(() ->
                            mActivity.runOnUiThread(() -> new Handler().post(this::fillPending))
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.e(TAG, "refresher thread interrupted, ID:" + refresher.getId());
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (refresher != null) {
            refresher.interrupt();
        }
    }

    private void fillTransfer() {
        cancelTransaction.setVisibility(View.GONE);

        String dateFormat = DateFormat.format("MMMM dd, yyyy, hh:mm a", transfer.date).toString();
        transactionDateText.setText(dateFormat);
        if (transfer.txid != null) {
            transactionsHashButton.setVisibility(View.VISIBLE);
            transactionsHashButton.setOnClickListener(v -> {
                String networkEtherscanName = "";
                if (Global.isTest()) {
                    networkEtherscanName = "kovan.";
                }
                String url = String.format("https://%setherscan.io/tx/%s", networkEtherscanName, transfer.txid);

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            });
        } else {
            transactionsHashButton.setVisibility(View.GONE);
        }
        mActionBar.setTitle("History");


        guardiansRecyclerView.setHasFixedSize(true);
        guardiansRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        validatorsTitle.setVisibility(View.GONE);
        // TODO: Not known if approved or rejected here
        List<Approval> collect = guardians.stream().map(g -> {
            ApprovalState state = ApprovalState.WAITING;
            if (transfer.cancelledBy != null && transfer.cancelledBy.equals(g.id)) {
                state = ApprovalState.DECLINED;
            }
            if (transfer.approvals.contains(g.id)) {
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
            valueFormat += String.format(Locale.US, " | %s"+NBSP+"USD", Utils.toMoneyFormat(dollarVal));
        } else {
            ExchangeRate exchangeRate = mExchangeRates.get(transfer.token.type);
            if (exchangeRate != null) {
                valueFormat += String.format(Locale.US, " | %s"+NBSP+"USD", Utils.toMoneyFormat(value * exchangeRate.average24h));
            }
        }
        sendAmountTextView.setText(valueFormat);
        if (transfer.comment != null && transfer.comment.length() > 0) {
            transactionCommentTextView.setText(transfer.comment);
        } else {
            transactionCommentLabel.setVisibility(View.GONE);
            transactionCommentTextView.setVisibility(View.GONE);
        }
        boolean isOutgoingTx = transfer.valueString.contains("-");
        if (isOutgoingTx) {
            senderTitleTextView.setVisibility(View.GONE);
            senderAddressButton.setVisibility(View.GONE);
            recipientAddressButton.setText(transfer.remoteAddress);
        } else {

            guardiansTitleTextView.setVisibility(View.GONE);
            if (transfer.state == ApprovalState.APPROVED) {
                guardiansRecyclerView.setVisibility(View.GONE);
            }
            validatorsTitle.setVisibility(View.GONE);


            recipientTitleTextView.setVisibility(View.GONE);
            recipientAddressButton.setVisibility(View.GONE);

            senderTitleTextView.setVisibility(View.VISIBLE);
            senderAddressButton.setVisibility(View.VISIBLE);
            senderAddressButton.setText(transfer.remoteAddress);
        }
    }

    private void showDeletedPopup() {
        mActionBar.hide();
        successPopup.setVisibility(View.VISIBLE);
        popupTitle.setText("Your transaction has been\ncancelled successfully");
        popupImage.setImageResource(R.drawable.ic_trash);
        greatThanksButton.setOnClickListener(v -> {
            mActionBar.show();
            mActivity.onBackPressed();
        });
    }
}
