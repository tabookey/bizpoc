package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FirstFragment extends Fragment {
    public static boolean didShowSplashScreen = false;
    private View progressView;
    private SpinKitView progressBar;
    private Button retryButton;
    private ExchangeRate mExchangeRate;
    private ListView balancesListView;
    BalancesAdapter adapter;
    private TextView balanceInDollarsText;
    private AppCompatActivity mActivity;
    List<BitgoUser> guardians;
    List<BalancesAdapter.Balance> balances;
    private IBitgoWallet mBitgoWallet;
    private View mainContentsLayout;
    private ListView historyListView;
    private ListView pendingListView;
    private TextView pendingTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.first_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton sendButton = view.findViewById(R.id.sendButton);
        mainContentsLayout = view.findViewById(R.id.mainContentsLayout);
        balancesListView = view.findViewById(R.id.balancesListView);
        historyListView = view.findViewById(R.id.historyListView);
        pendingListView = view.findViewById(R.id.pendingListView);
        progressView = view.findViewById(R.id.progressView);
        progressBar = view.findViewById(R.id.progressBar);
        retryButton = view.findViewById(R.id.retryButton);
        pendingTitle = view.findViewById(R.id.pendingTitle);
        retryButton.setOnClickListener(v -> fillWindow());
        balanceInDollarsText = view.findViewById(R.id.balanceInDollarsText);
        sendButton.setOnClickListener(v -> {
            SendFragment sf = new SendFragment();
            sf.exchangeRate = mExchangeRate;
            sf.guardians = guardians;
            sf.balances = balances;
            sf.mBitgoWallet = mBitgoWallet;
            mActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, sf, MainActivity.SEND_FRAGMENT)
                    .addToBackStack("to_send").commit();
        });

        Button transactionsButton = view.findViewById(R.id.transactionsButton);
        transactionsButton.setOnClickListener(v -> {
            TransactionsFragment tf = new TransactionsFragment();
            tf.mExchangeRate = mExchangeRate;
            tf.mGuardians = guardians;
            mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, tf).addToBackStack(null).commit();
        });

        fillWindow();
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
        progressView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        if (didShowSplashScreen) {
            progressView.setBackgroundColor(Color.WHITE);
            progressBar.setColor(R.color.colorPrimaryDark);
        }
        didShowSplashScreen = true;
        new Thread() {
            public void run() {
                double assetsWorth = 0;
                try {
                    List<IBitgoWallet> allw = Global.ent.getMergedWallets();
                    mBitgoWallet = allw.get(0);
                    mExchangeRate = Global.ent.getMarketData("teth");
                    List<String> coins = mBitgoWallet.getCoins();
                    balances = new ArrayList<>();
                    for (String coin : coins) {
                        String coinBalance = mBitgoWallet.getBalance(coin);
                        double exRate = Global.ent.getMarketData(coin).average24h;

                        TokenInfo token = Global.ent.getTokens().get(coin);
                        if (token == null) {
                            throw new RuntimeException("Unknown token balance!");
                        }
                        BalancesAdapter.Balance balance = new BalancesAdapter.Balance(coin, coinBalance, exRate, token);
                        balances.add(balance);
                        assetsWorth += balance.getDollarValue();
                    }
                    guardians = mBitgoWallet.getGuardians();

                    /* * * */
                    List<PendingApproval> pendingApprovals;
                    List<Transfer> transfers;
                    try {
                        pendingApprovals = mBitgoWallet.getPendingApprovals();
                        transfers = mBitgoWallet.getTransfers();
                    } catch (Exception e) {
                        mActivity.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            retryButton.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                    mActivity.runOnUiThread(() -> {
                        progressView.setVisibility(View.GONE);
                        TransactionHistoryAdapter historyAdapter = new TransactionHistoryAdapter(mActivity, mExchangeRate);
                        if ( pendingApprovals.size()>0) {
                            pendingTitle.setVisibility(View.VISIBLE);
                            pendingListView.setVisibility(View.VISIBLE);
                            TransactionHistoryAdapter pendingAdapter = new TransactionHistoryAdapter(mActivity, mExchangeRate);
                            pendingAdapter.addItems(pendingApprovals);
                            pendingListView.setAdapter(pendingAdapter);
                            Utils.setListViewHeightBasedOnChildren(pendingListView);
                        }
                        historyAdapter.addItem("History");
                        historyAdapter.addItems(transfers);
                        historyListView.setAdapter(historyAdapter);
                        Utils.setListViewHeightBasedOnChildren(historyListView);
                    });
                } catch (Exception e) {
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        retryButton.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                double finalAssetsWorth = assetsWorth;
                mActivity.runOnUiThread(() -> {
                    progressView.setVisibility(View.GONE);
                    mainContentsLayout.setVisibility(View.VISIBLE);
                    View view = getView();
                    if (view == null) {
                        return;
                    }
                    TextView address = view.findViewById(R.id.addressText);
                    TextView owner = view.findViewById(R.id.ownerText);
                    balanceInDollarsText.setText(String.format(Locale.US, "%.2f USD", finalAssetsWorth));
                    address.setText(mBitgoWallet.getAddress());
                    owner.setText(String.format("Welcome %s", Global.ent.getMe().name));
                    adapter = new BalancesAdapter(mActivity, 0, balances);
                    balancesListView.setAdapter(adapter);
                    Utils.setListViewHeightBasedOnChildren(balancesListView);
                    ImageButton copyButton = view.findViewById(R.id.shareButton);
                    copyButton.setOnClickListener(v -> {
                        ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard == null) {
                            return;
                        }
                        ClipData clip = ClipData.newPlainText("label", mBitgoWallet.getAddress());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(mActivity, "Wallet address copied to clipboard", Toast.LENGTH_LONG).show();
                    });

                });
            }
        }.start();
    }
}
