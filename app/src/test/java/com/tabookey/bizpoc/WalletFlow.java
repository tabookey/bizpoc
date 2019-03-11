package com.tabookey.bizpoc;

import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.BitgoEnterprise;

import org.junit.Test;

import java.util.List;

import static com.tabookey.bizpoc.impl.Utils.toJson;

/**
 * command line to go throuh the wallet APIs
 */
public class WalletFlow {


    String accDrorGmail = "v2x0ea2d66f027a54a3f9566934e67008ff4823e016e2ddbdb05e562d48db2d6b02";

//    String accessKey = "v2x7fa63b4f6b6b17c821f9b95a6313efa04fb29ecc7705f9dce774d4d6fd94109d";
    String accessKey = "v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8";

    @Test public void run1() {

        IBitgoEnterprise ent = new BitgoEnterprise(accessKey, true);
//        System.out.println( toJson(ent.getInfo()));
        System.out.println( "me: "+toJson(ent.getMe()) );
        IBitgoWallet w = ent.getWallets("teth").get(0);
        System.out.println("wallet balance: "+ w.getBalance());
        List<Transfer> transfers = w.getTransfers();
        for ( Transfer t : transfers)
            System.out.println( "tx: "+toJson(t));
        List<PendingApproval> pendingApprovals = w.getPendingApprovals();
        for ( PendingApproval pa: pendingApprovals )
            System.out.println( "pa: "+toJson(pa));

        PendingApproval pa= pendingApprovals.get(0);

//        w.rejectPending(pa, "123456");
    }

    String fullAccessKey = "v2xe3de01b2a3394785d315b0723523f77ddab9114480ba96bd50828d5974c86ef3";

    //attempt to do a send operation
    @Test public void sendCoin() {
        IBitgoEnterprise ent = new BitgoEnterprise(fullAccessKey, true);
        IBitgoWallet w = ent.getWallets("teth").get(0);
        SendRequest req = new SendRequest() {{
            amount="10000000000000000";
            otp="000000";
            coin="teth";
            recpipientAddress = "0xd21934ed8eaf27a67f0a70042af50a1d6d195e81";
        }};

        w.sendMoney(req);

    }

    @Test public void run2() {
        String xpub = "xpub661MyMwAqRbcG6stvcFc3Wee5sqwXvK5NzGnkeRtu8JuUebKbQCuq7zroyJ4TSWd9VuenwwirViTwtrtdHRn9B7HeeBzqVdnrWRCerJhTRe";
        String ethAddress ="0x2c84d8c838935f4c1ab884403daf424759f65f43";
//assertEquals(
//        Base58.encode(new Address(ethAddress).toUint160().getValue().toByteArray()), "");
//        assertEquals(Numeric.toHexString(Base58.decode(xpub.replace("xpub",""))),ethAddress);
    }

    //TODO: parser/decoder from eth address and xpub
}