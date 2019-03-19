package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.TokenInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FirstFragment extends Fragment {
    private View progressBar;
    private ExchangeRate exchangeRate;
    private ListView balancesListView;
    BalancesAdapter adapter;
    private TextView balanceInDollarsText;
    private AppCompatActivity mActivity;
    List<BitgoUser> guardians;
    List<BalancesAdapter.Balance> balances;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.first_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button sendButton = view.findViewById(R.id.sendButton);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        balancesListView = view.findViewById(R.id.balancesListView);
        progressBar = view.findViewById(R.id.progressBar);
        balanceInDollarsText = view.findViewById(R.id.balanceInDollarsText);
        sendButton.setOnClickListener(v -> {
            SendFragment sf = new SendFragment();
            sf.exchangeRate = exchangeRate;
            sf.guardians = guardians;
            sf.balances = balances;
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, sf, MainActivity.SEND_FRAGMENT)
                    .addToBackStack("to_send").commit();
        });

        Button transactionsButton = view.findViewById(R.id.transactionsButton);
        transactionsButton.setOnClickListener(v -> {
            TransactionsFragment tf = new TransactionsFragment();
            tf.mExchangeRate = exchangeRate;
            tf.mGuardians = guardians;
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, tf).addToBackStack(null).commit();
        });

        progressBar.setVisibility(View.VISIBLE);
        new Thread() {
            public void run() {
                fillWindow();
                progressBar.post(() -> progressBar.setVisibility(View.GONE));
            }
        }.start();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    void fillWindow() {
        List<IBitgoWallet> allw = Global.ent.getMergedWallets();
        IBitgoWallet ethWallet = allw.get(0);
        exchangeRate = Global.ent.getMarketData("teth");
        List<String> coins = ethWallet.getCoins();
        balances = new ArrayList<>();
        double assetsWorth = 0;
        for (String coin : coins) {
            String coinBalance = ethWallet.getBalance(coin);
            double exRate = Global.ent.getMarketData(coin).average24h;

            TokenInfo token = Global.ent.getTokens().get(coin);
            if (token == null) {
                throw new RuntimeException("Unknown token balance!");
            }
            BalancesAdapter.Balance balance = new BalancesAdapter.Balance(coin, coinBalance, exRate, token);
            balances.add(balance);
            assetsWorth += balance.getDollarValue();
        }
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }
        guardians = ethWallet.getGuardians();
        double finalAssetsWorth = assetsWorth;
        activity.runOnUiThread(() -> {
            TextView address = view.findViewById(R.id.addressText);
            TextView owner = view.findViewById(R.id.ownerText);
            balanceInDollarsText.setText(String.format(Locale.US, "%.2f USD", finalAssetsWorth));
            address.setText(ethWallet.getAddress());
            owner.setText(String.format("Welcome %s", Global.ent.getMe().name));
            adapter = new BalancesAdapter(activity, 0, balances);
            balancesListView.setAdapter(adapter);
            ImageButton copyButton = view.findViewById(R.id.copyButton);
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) {
                    return;
                }
                ClipData clip = ClipData.newPlainText("label", ethWallet.getAddress());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mActivity, "Wallet address copied to clipboard", Toast.LENGTH_LONG).show();
            });

        });

    }
}
