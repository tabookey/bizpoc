package com.tabookey.bizpoc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;

import java.util.Comparator;
import java.util.List;

public class WalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        new Thread() {
            public void run() {
                fillWindow();
            }
        }.start();
    }

    void setText(int id, String fmt, Object... args) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView v = findViewById(id);
                v.setText(String.format(fmt, args));
            }
        });
    }

    void fillWindow() {
        IBitgoWallet ethWallet = Global.ent.getWallets("teth").get(0);
        IBitgoWallet tercWallet = Global.ent.getWallets("terc").get(0);
        setText(R.id.ownerText, "Hello, %s", Global.ent.getMe().name);
        setText(R.id.balanceText, "Balance: %s ETH ($%s)\n\t%s terc ($%s)",
                ethWallet.getBalance(), "n/a",
                tercWallet.getBalance(), "n/a"
        );

        StringBuilder pendingText = new StringBuilder();
        List<PendingApproval> pending = ethWallet.getPendingApprovals();
        pending.addAll(tercWallet.getPendingApprovals());
        for (PendingApproval p : pending) {
            pendingText.append(String.format("- %s %s to %s (%s)\n", p.coin, p.amount, p.recipientAddr, p.creator.name));
        }
        setText(R.id.pendingText, "Pending Approvals:\n%s", pendingText.toString());

        StringBuilder historyText = new StringBuilder();

        List<Transfer> transfers = ethWallet.getTransfers();
        transfers.addAll(tercWallet.getTransfers());
        //TODO: sort by date.

        for ( Transfer t : transfers ) {
            historyText.append(String.format("- %s %s %s $%s to %s\n",
                    t.date, t.coin, t.valueString, t.usd, t.remoteAddress
            ));
        }
        setText(R.id.transfersText, "Past Transactions:\n%s", historyText.toString());
    }

    public void onSendMoney(View v) {

    }
}
