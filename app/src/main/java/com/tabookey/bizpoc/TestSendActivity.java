package com.tabookey.bizpoc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.tabookey.logs.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

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

        setDebugTools();

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

    private void setDebugTools() {
        ToggleButton fakeSafetynetToggle = findViewById(R.id.fakeSafetynetToggle);
        fakeSafetynetToggle.setChecked(Global.getFakeSafetynet());
        fakeSafetynetToggle.setOnClickListener(v -> Global.setFakeSafetynet(fakeSafetynetToggle.isChecked()));


        String[] environments = {"Production", "Debug", "Custom"};
        //Creating the ArrayAdapter instance having the country list
        ArrayAdapter aa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, environments);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner

        Spinner spin = findViewById(R.id.spinner);
        View customProvisioningView = findViewById(R.id.customProvisioningView);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Global.setEnvironment(position);
                if (position == 2) {
                    customProvisioningView.setVisibility(View.VISIBLE);
                }
                else {
                    customProvisioningView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spin.setAdapter(aa);

        EditText provisioningUrlEditText = findViewById(R.id.provisioningUrlEditText);
        Button saveUrlButton = findViewById(R.id.saveUrlButton);
        saveUrlButton.setOnClickListener(view -> Global.setTestProvisionServer(provisioningUrlEditText.getText().toString()));
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
