package com.tabookey.bizpoc;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.SendRequest;

import androidx.annotation.RequiresApi;

public class TestSendActivity extends AppCompatActivity {
    private static final String TAG = "TAG";
    private TextView log;

    public void log(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        Global.setApiKey("v2xe3de01b2a3394785d315b0723523f77ddab9114480ba96bd50828d5974c86ef3");
        IBitgoEnterprise ent = Global.ent;
        log( "me: "+ent.getMe().name);
        IBitgoWallet w = ent.getWallets("teth").get(3);
        log( "wallet id="+w.getLabel()+": "+w.getId());

        String dest = "0xd21934eD8eAf27a67f0A70042Af50A1D6d195E81";
        SendRequest req = new SendRequest(dest, "comment", "1122334455667788",
                "0000000", "asd/asd-ASD");

        w.sendCoins(req, (type, msg) -> log("== "+type+": "+msg) );
    }
}
