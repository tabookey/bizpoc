package com.tabookey.bizpoc;

import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.BitgoEnterprise;

import org.junit.Test;

import java.util.List;

import static com.tabookey.bizpoc.impl.Utils.toJson;
import static org.junit.Assert.assertEquals;

/**
 * command line to go throuh the wallet APIs
 */
public class WalletFlow {



    String accessKey = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    @Test public void run1() {

        IBitgoEnterprise ent = new BitgoEnterprise(accessKey, true);
//        System.out.println( toJson(ent.getInfo()));
        System.out.println( toJson(ent.getMe()) );
        IBitgoWallet w = ent.getWallets("terc").get(0);
        System.out.println("wallet balance="+ w.getBalance());
        List<Transfer> transfers = w.getTransfers();
        for ( Transfer t : transfers)
            System.out.println( toJson(t));
        System.out.println( toJson(w.getPendingApprovals() ));

    }
//    @Test public void run2() {}
}