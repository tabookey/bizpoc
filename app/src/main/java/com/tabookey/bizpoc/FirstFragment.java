package com.tabookey.bizpoc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.tabookey.logs.Log;

import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;
import com.tabookey.bizpoc.impl.Wallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.tabookey.bizpoc.impl.Utils.NBSP;
import static com.tabookey.bizpoc.impl.Utils.collapse;
import static com.tabookey.bizpoc.impl.Utils.expand;

public class FirstFragment extends Fragment {

    //&nbsp; - non-breaking whitespace.

    public static String TAG = "1frag";

    public static boolean didShowSplashScreen = false;
    private View progressView;
    private SpinKitView progressBar;
    private View retryView;
    HashMap<String, ExchangeRate> mExchangeRates = new HashMap<>();
    private ListView balancesListView;
    BalancesAdapter adapter;
    private TextView balanceInDollarsText;
    private TextView emptyPendingTextView;
    private MainActivity mActivity;
    List<BitgoUser> mGuardians;
    List<BalancesAdapter.Balance> balances;
    private IBitgoWallet mBitgoWallet;
    private View mainContentsLayout;
    private View overlayInfoCardView;
    private ScrollView mainContentsScrollView;
    private ListView historyListView;
    private ListView pendingListView;
    private TextView searchingNetworkWarning;

    boolean whiteBackground = false;
    private boolean didShowWarningOnGuardians = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.first_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton sendButton = view.findViewById(R.id.sendButton);
        mainContentsLayout = view.findViewById(R.id.mainContentsLayout);
        mainContentsScrollView = view.findViewById(R.id.mainContentsScrollView);
        overlayInfoCardView = view.findViewById(R.id.overlayInfoCardView);
        balancesListView = view.findViewById(R.id.balancesListView);
        historyListView = view.findViewById(R.id.historyListView);
        pendingListView = view.findViewById(R.id.pendingListView);
        emptyPendingTextView = view.findViewById(R.id.emptyPendingTextView);
        progressView = view.findViewById(R.id.progressView);
        progressBar = view.findViewById(R.id.progressBar);
        Button retryButton = view.findViewById(R.id.retryButton);
        retryView = view.findViewById(R.id.retryView);
        searchingNetworkWarning = view.findViewById(R.id.searchingNetworkWarning);
        retryButton.setOnClickListener(v -> fillWindow(true));
        balanceInDollarsText = view.findViewById(R.id.balanceInDollarsText);
        balanceInDollarsText.setOnLongClickListener(a -> {
            mActivity.showSomethingWrongLogsDialog(false);
                /*
                String[] values = {"0", "1", "3", "5", "7", "9"};
                int currentItem = Arrays.asList(values).indexOf(String.valueOf(Wallet.balanceExtraDigits));
                new AlertDialog.Builder(getActivity())
                        .setTitle("Balance Suffix extra digits")
                        .setSingleChoiceItems(values, currentItem, (dialog, which) -> {
                            Wallet.balanceExtraDigits = Integer.parseInt(values[which]);
                            triggerRefresh();
                            dialog.dismiss();
                        })
                        .show();
                */
            return true;
        });
        sendButton.setOnClickListener(v -> {
            SendFragment sf = new SendFragment();
            sf.mExchangeRates = mExchangeRates;
            sf.guardians = mGuardians;
            sf.balances = balances;
            sf.mBitgoWallet = mBitgoWallet;
            mActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, sf, MainActivity.SEND_FRAGMENT)
                    .addToBackStack("to_send").commit();
        });

        Button transactionsButton = view.findViewById(R.id.viewAllTransactionsButton);
        transactionsButton.setOnClickListener(v -> {
            TransactionsFragment tf = new TransactionsFragment();
            tf.mExchangeRates = mExchangeRates;
            tf.mGuardians = mGuardians;
            mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, tf).addToBackStack(null).commit();
        });
        if (whiteBackground) {
            view.setBackgroundColor(mActivity.getColor(android.R.color.white));
            progressBar.setColor(mActivity.getColor(R.color.colorPrimaryDark));
        }
        fillWindow(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mActivity = (MainActivity) context;
        }
    }

    Thread refresher;

    //trigger refresher thread to update now..
    public void triggerRefresh() {
        synchronized (refresher) {
            refresher.notify();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (refresher != null) {
            refresher.interrupt();
        }
        refresher = new Thread(() -> {
            Log.e(TAG, "refresher thread started, ID:" + refresher.getId());
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(20000);

                    Global.ent.update(() -> {
                        //TODO: can't be really used, since it TERC/TBST areupdated on EACH call (with random data, probably...)
                        Log.d("TAG", "========= ENTERPRISE CHANGE");
                    });
                    if (mBitgoWallet == null) {
                        return;
                    }
                    mBitgoWallet.update(() ->
                            mActivity.runOnUiThread(() -> fillWindow(false))
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    mActivity.runOnUiThread(() ->
                    {
                        expand(searchingNetworkWarning, 1500, searchingNetworkWarning.getHeight(), (int) Utils.convertDpToPixel(20, mActivity));
                    });

                }
            }
            Log.e(TAG, "refresher thread interrupted, ID:" + refresher.getId());
        });
        refresher.start();

    }

    @Override
    public void onPause() {
        super.onPause();
        refresher.interrupt();
    }

    void fillWindow(boolean showProgress) {
        collapse(searchingNetworkWarning, 1500, searchingNetworkWarning.getHeight(), 0);
        if (showProgress) {
            mainContentsLayout.setVisibility(View.GONE);
            overlayInfoCardView.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            retryView.setVisibility(View.GONE);
            if (didShowSplashScreen) {
                progressView.setBackgroundColor(Color.WHITE);
                progressBar.setColor(R.color.colorPrimaryDark);
            }
        }
        didShowSplashScreen = true;
        new Thread() {
            public void run() {
                double assetsWorth = 0;
                try {
                    List<IBitgoWallet> allw = Global.ent.getMergedWallets();
                    mBitgoWallet = allw.get(0);
                    List<String> coins = mBitgoWallet.getCoins();
                    balances = new ArrayList<>();
                    for (String coin : coins) {
                        String coinBalance = mBitgoWallet.getBalance(coin);
                        ExchangeRate exchangeRate = Global.ent.getMarketData(coin);
                        mExchangeRates.put(coin, exchangeRate);
                        TokenInfo token = Global.ent.getTokens().get(coin);
                        if (token == null) {
                            throw new RuntimeException("Unknown token balance!");
                        }
                        BalancesAdapter.Balance balance = new BalancesAdapter.Balance(coin, coinBalance, exchangeRate.average24h, token);
                        balances.add(balance);
                        assetsWorth += balance.getDollarValue();
                    }
                    mGuardians = mBitgoWallet.getGuardians();

                    if (!didShowWarningOnGuardians && (mGuardians == null || mGuardians.size() == 0)) {
                        mActivity.runOnUiThread(() -> {
                            didShowWarningOnGuardians = true;
                            Utils.showErrorDialog(mActivity, "Warning!", "There seems to be no guardians added to this wallet", null);
                        });
                    }

                    /* * * */
                    List<PendingApproval> pendingApprovals;
                    List<Transfer> transfers;
                    try {
                        pendingApprovals = mBitgoWallet.getPendingApprovals();
                        pendingApprovals.sort((a, b) -> b.createDate.compareTo(a.createDate));
                        transfers = mBitgoWallet.getTransfers(3);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mActivity.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            retryView.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                    mActivity.runOnUiThread(() -> {
                        progressView.setVisibility(View.GONE);
                        TransactionHistoryAdapter historyAdapter = new TransactionHistoryAdapter(mActivity, mExchangeRates, null);
                        if (pendingApprovals.size() > 0) {
                            pendingListView.setVisibility(View.VISIBLE);
                            emptyPendingTextView.setVisibility(View.GONE);
                            TransactionHistoryAdapter pendingAdapter = new TransactionHistoryAdapter(mActivity, mExchangeRates, mGuardians);
                            pendingAdapter.addItems(pendingApprovals);
                            pendingListView.setAdapter(pendingAdapter);
                            Utils.setListViewHeightBasedOnChildren(pendingListView);
                            pendingListView.setOnItemClickListener((adapterView, view1, position, id) -> {
                                Object item = pendingListView.getItemAtPosition(position);
                                mActivity.openPendingDetails(item, mExchangeRates, mGuardians, mBitgoWallet);
                            });
                        } else {
                            pendingListView.setVisibility(View.GONE);
                            emptyPendingTextView.setVisibility(View.VISIBLE);
                        }
                        historyAdapter.addItems(transfers);
                        historyListView.setAdapter(historyAdapter);
                        historyListView.setOnItemClickListener((adapterView, view1, position, id) -> {
                            Object item = historyListView.getItemAtPosition(position);
                            mActivity.openPendingDetails(item, mExchangeRates, mGuardians, mBitgoWallet);
                        });
                        Utils.setListViewHeightBasedOnChildren(historyListView);
                        if (showProgress) {
                            new Handler().post(() -> mainContentsScrollView.scrollTo(0, 0));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "ex", e);
                    mActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                BitgoUser entMe = Global.ent.getMe();
                double finalAssetsWorth = assetsWorth;
                mActivity.runOnUiThread(() -> {
                    progressView.setVisibility(View.GONE);
                    mainContentsLayout.setVisibility(View.VISIBLE);
                    overlayInfoCardView.setVisibility(View.VISIBLE);
                    View view = getView();
                    if (view == null) {
                        return;
                    }
                    Button address = view.findViewById(R.id.addressTextButton);
                    Utils.makeButtonCopyable(address, mActivity);
                    TextView owner = view.findViewById(R.id.ownerText);
                    balanceInDollarsText.setText(String.format(Locale.US, "%s" + NBSP + "USD", Utils.toMoneyFormat(finalAssetsWorth)));
                    address.setText(mBitgoWallet.getAddress());
                    owner.setText(String.format("%s's safe%s", entMe.name, Global.isTest() ? " (testnet)" : ""));
                    adapter = new BalancesAdapter(mActivity, 0, balances);
                    balancesListView.setAdapter(adapter);
                    Utils.setListViewHeightBasedOnChildren(balancesListView);
                    ImageButton shareButton = view.findViewById(R.id.shareButton);
                    shareButton.setOnClickListener(v -> {
                        String shareBody = AddressChecker.checkedAddress(mBitgoWallet.getAddress());
//                        String shareBody = String.format("This is %s’s Ethereum wallet address: %s", Global.ent.getMe().name, AddressChecker.checkedAddress(mBitgoWallet.getAddress()));
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Wallet Ethereum address");
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                        startActivity(Intent.createChooser(sharingIntent, "Share address..."));
                    });

                });
            }
        }.start();
    }
}
