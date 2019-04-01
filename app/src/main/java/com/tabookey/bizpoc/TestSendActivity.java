package com.tabookey.bizpoc;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.api.Transfer;

import java.util.List;

public class TestSendActivity extends AppCompatActivity {
    private static final String TAG = "TAG";
    private TextView log;

    public void log(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("LOG", msg );
                log.append(msg + "\n");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webv);

        log = findViewById(R.id.textlog);
        new Thread() {
            @Override
            public void run() {
                try {

                    sendfromwallet();
                } catch (Throwable e) {
                    log("exception: " + e);
                    Log.e(TAG, "ex: ",e );
                }
            }
        }.start();
    }

    private void sendfromwallet() {
        log("starting:");
//        Global.setAccessToken("v2xe3de01b2a3394785d315b0723523f77ddab9114480ba96bd50828d5974c86ef3"); //dror? too many wallets...
        Global.setAccessToken("v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8" );

        //tomer:
//        Global.setAccessToken("v2xa3b02b7a841fd4a39bd565e8e672f7a530f2c38ee42f5879c39ef8d6386a7871"); //tomer
        IBitgoEnterprise ent = Global.ent;
        Log.d(TAG, "rates: "+ent.getAllExchangeRates());
        log( "me: "+ent.getMe().name);

            List<IBitgoWallet> allw = ent.getMergedWallets();
        IBitgoWallet w = allw.get(0);

//        IBitgoWallet w = ent.getWallets("teth").get(3);
        log( "wallet id="+w.getLabel() );
        log( "coins: "+w.getCoins());
        for ( String s : w.getCoins() )
            log( "balance "+s+" - "+w.getBalance(s));

        w.getGuardians().forEach(user->log("guardian: "+user.email+(user.hasPerm(BitgoUser.Perm.admin)?" admin":"")));

        List<Transfer> transfers = w.getTransfers(0);
        log( "transfers: "+transfers.size());
        transfers.forEach(t->log(t.coin+" "+t.valueString+"="+t.usd+" "+t.state));
        List<PendingApproval> pending = w.getPendingApprovals();
        log( "pending: "+pending.size());
        pending.forEach(p->{
            log("- "+p.createDate+" "+p.coin+" "+ p.amount+" "+p.comment);
        });
        if ( this==null ) return;
        String dest = "0xd21934eD8eAf27a67f0A70042Af50A1D6d195E81";
        TokenInfo tokenInfo = Global.ent.getTokens().get("teth");
        SendRequest req = new SendRequest(tokenInfo, "", dest,
                "0000000", "asd/asd-ASD", "comment");

        w.sendCoins(req, (type, msg) -> log("== "+type+": "+msg) );
    }
}
