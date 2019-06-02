package com.tabookey.bizpoc;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.Transfer;

import java.util.List;

public class TransactionsFragment extends Fragment {
    private ListView transactionsListView;
    private ProgressBar progressBar;
    private View progressView;
    private Button retryButton;
    private MainActivity mActivity;
    private IBitgoWallet ethWallet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.transaction_fragment, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mActivity = (MainActivity) context;
            ActionBar supportActionBar = mActivity.getSupportActionBar();
            if (supportActionBar == null) {
                return;
            }
            supportActionBar.setTitle("History");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transactionsListView = view.findViewById(R.id.list_view_transactions);
        transactionsListView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Object item = transactionsListView.getItemAtPosition(position);
            mActivity.openPendingDetails(item);
        });

        progressBar = view.findViewById(R.id.progressBar);
        progressView = view.findViewById(R.id.progressView);
        retryButton = view.findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> fillWindow());
        fillWindow();
    }

    public void fillWindow() {
        progressView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        new Thread(() -> {
            List<Transfer> transfers;
            try {
                ethWallet = Global.ent.getMergedWallets().get(0);
                transfers = ethWallet.getTransfers(0);
            } catch (Exception e) {
                mActivity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    retryButton.setVisibility(View.VISIBLE);
                });
                return;
            }
            mActivity.runOnUiThread(() -> {
                progressView.setVisibility(View.GONE);
                TransactionHistoryAdapter historyAdapter = new TransactionHistoryAdapter(mActivity, Global.sExchangeRates, null);
                historyAdapter.addItems(transfers);
                transactionsListView.setAdapter(historyAdapter);
            });
        }).start();

    }
}
