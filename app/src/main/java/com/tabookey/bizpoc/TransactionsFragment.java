package com.tabookey.bizpoc;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;

import java.util.List;

public class TransactionsFragment extends Fragment {
    private ListView lvt;
    private View progressBar;
    ExchangeRate mExchangeRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.transaction_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvt = view.findViewById(R.id.list_view_transactions);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(this::fillWindow).start();
    }

    public void fillWindow() {
        IBitgoWallet ethWallet = Global.ent.getWallets("teth").get(0);
        List<PendingApproval> pendingApprovals = ethWallet.getPendingApprovals();
        List<Transfer> transfers = ethWallet.getTransfers();
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            TransactionHistoryAdapter historyAdapter = new TransactionHistoryAdapter(getActivity(), mExchangeRate);
            historyAdapter.addItem("Pending");
            historyAdapter.addItems(pendingApprovals);
            historyAdapter.addItem("History");
            historyAdapter.addItems(transfers);

            lvt.setAdapter(historyAdapter);

//            TransactionPendingAdapter pendingAdapter = new TransactionPendingAdapter(getActivity(), R.layout.pending_transaction_line, pending);
//            lvp.setAdapter(pendingAdapter);
        });
    }
}
