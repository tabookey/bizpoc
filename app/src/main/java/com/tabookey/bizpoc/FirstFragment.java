package com.tabookey.bizpoc;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import java.util.ArrayList;
import java.util.Arrays;

public class FirstFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.first_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null) {
            return;
        }
        Button sendButton = view.findViewById(R.id.sendButton);
        SendFragment sf = new SendFragment();
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        sendButton.setOnClickListener(v -> activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, sf).addToBackStack(null).commit());

        Button transactionsButton = view.findViewById(R.id.transactionsButton);
        TransactionsFragment tf = new TransactionsFragment();
        transactionsButton.setOnClickListener(v ->
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, tf).addToBackStack(null).commit()
        );

        Button transactionDetailsButtonDev = view.findViewById(R.id.transactionDetailsButtonDev);
        transactionDetailsButtonDev.setOnClickListener(v -> {
            TransactionDetailsFragment tdf = new TransactionDetailsFragment();
            tdf.transfer = new Transfer("123412", "12312312312312312", "teth", "1234", "12/12/1212", "0x123123", "TXID321543");
            tdf.setExchangeRate(new ExchangeRate(100.01));

            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, tdf).commit();
        });
        Button pendingDetailsButtonDev = view.findViewById(R.id.pendingDetailsButtonDev);
        pendingDetailsButtonDev.setOnClickListener(v -> {
            TransactionDetailsFragment tdf = new TransactionDetailsFragment();
            tdf.pendingApproval = new PendingApproval("123412",
                    "12312312312312312", "0x123123",
                    "1234", "teth", "200000",
                    Arrays.asList(new BitgoUser("id", "e@m", "Na me"),
                            new BitgoUser("id", "e@m", "Na me")),
                    new BitgoUser("id", "e@m", "Na me"));
            tdf.setExchangeRate(new ExchangeRate(100.01));

            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, tdf).commit();
        });

        new Thread() {
            public void run() {
                fillWindow();
            }
        }.start();
    }

    void fillWindow() {
        IBitgoWallet ethWallet = Global.ent.getWallets("teth").get(0);
        setText(R.id.ownerText, "Hello, %s", Global.ent.getMe().name);
        String balanceString = ethWallet.getBalance();
        double etherDouble = Utils.weiStringToEtherDouble(balanceString);
        setText(R.id.balanceText, "%.6f", etherDouble);
        ExchangeRate e = Global.ent.getMarketData();
        setText(R.id.balanceInDollarsText, "$%.2f", etherDouble * e.average24h);
        setText(R.id.addressText, ethWallet.getAddress());

    }

    void setText(int id, String fmt, Object... args) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            View view = getView();
            if (view == null) {
                return;
            }
            TextView v = view.findViewById(id);
            v.setText(String.format(fmt, args));
        });
    }
}
