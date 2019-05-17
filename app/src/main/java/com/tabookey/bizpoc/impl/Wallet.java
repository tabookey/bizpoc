package com.tabookey.bizpoc.impl;

import com.tabookey.logs.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.BuildConfig;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import kotlin.NotImplementedError;

/**
 * single-coin wallet.
 * This is the low-level coin API.
 */
public class Wallet implements IBitgoWallet {

    public static int balanceExtraDigits=0;

    private final BitgoEnterprise ent;
    private final String balanceString;
    private final CoinSender coinSender;
    private final List<String> hiddenUsers = Arrays.asList(
            "liraz+creator@proguard.network",
            "dror561@gmail.com",
            "kfirqa1@gmail.com"
    );
    String coin, id, label;
    int approvalsRequired;

    ArrayList<BitgoUser> guardians;
    String address;

    Wallet(BitgoEnterprise ent, JsonNode node, String coin) {

        WalletNode walletData = Utils.fromJson(Utils.toJson(node), WalletNode.class);

        this.ent = ent;
        this.coin = coin;
        this.id = walletData.id;
        this.label = walletData.label;
        this.approvalsRequired = walletData.approvalsRequired;
        this.balanceString = walletData.balanceString + balanceExtraDigits();
        this.address = walletData.coinSpecific.baseAddress;
        this.guardians = new ArrayList<>();
        for (WalletUser user : walletData.users) {
            if (user.user.equals(ent.getMe().id))
                continue;
            BitgoUser userById = ent.getUserById(user.user, true);
            if (userById == null) {
                // TODO: These are users that are not part of selected enterprise
                continue;
            }
            BitgoUser g = new BitgoUser(userById, Arrays.asList(user.permissions));
            if (hiddenUsers.contains(g.email)) {
                Log.w("wallet", "hidden guardian:" + g.email);
                continue;
            }
            guardians.add(g);
        }
//        guardians.add(new BitgoUser("", "did@approve", "Test test one"));
        coinSender = new CoinSender(Global.applicationContext, ent.http);
    }

    public static String balanceExtraDigits() {
        if ( !BuildConfig.DEBUG )
            return "";
        return "00000000000000000000".substring(0,balanceExtraDigits);
    }

    @Override
    public List<String> getCoins() {
        return Arrays.asList(coin);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public List<BitgoUser> getGuardians() {
        return guardians;
    }

    @Override
    public String getBalance(String coin) {
        if (!coin.equals(this.coin))
            throw new RuntimeException("invalid coin " + coin + ". Wallet only has " + this.coin);
        return balanceString + balanceExtraDigits();
    }

    @Override
    public String getAddress() {
        return address;
    }

    static class TransferResp {
        public Trans[] transfers;

        static class Trans {
            public String id, txid, coin, valueString, usd, comment, pendingApproval;
            public Date date;
            public Entry[] entries;
        }

        static class Entry {
            public String address, valueString;
        }
    }

    @Override
    public List<Transfer> getTransfers(int limit) {
        // TODO: there is currently no use for this method.
        throw new NotImplementedError("Use MergedWallet instead.");
    }

    @Override
    public void update(Runnable onChange) {
    }

    @Override
    public String sendCoins(SendRequest req, StatusCB cb) {
        return coinSender.sendCoins(this, req, cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return coinSender.checkPassphrase(this, passphrase, null);
    }

    static class PendingApprovalResp {

        public PendingApproval[] pendingApprovals;

        static class PendingApproval {
            public String id, coin, creator;
            public Date createDate;
            public Info info;
            public String state, scope;
            public int approvalsRequired;
            public String[] userIds;
            public Resolver[] resolvers;
        }

        static class Info {
            public String type; //transactionRequest
            public TxRequest transactionRequest;
        }

        static class TxRequest {
            public Recipient[] recipients;
            public String comment;
        }

        static class Recipient {
            public String address, amount;
        }

        static class Resolver {
            public String user, date, resolutionType;
        }
    }

    @Override
    public List<PendingApproval> getPendingApprovals() {
        throw new NotImplementedError("Use MergedWallet instead.");
    }

    static class ChangeState {
        final public String state, otp, walletPassphrase;
        public String halfSigned; //found in network log. not sure we can send it, since our token is limited..

        ChangeState(String state, String otp, String walletPassphrase) {
            this.state = state;
            this.otp = otp;
            this.walletPassphrase = walletPassphrase;
        }
    }
//    @Override
//    public void approvePending(PendingApproval approval, String otp) {
//        ChangeState change = new ChangeState("approved", otp, "asdasdsd");
//        change.halfSigned="0x123123123123";
//        PendingApprovalResp resp = ent.http.put("/api/v2/"+coin+"/pendingapprovals/"+approval.id, change, PendingApprovalResp.class);
//    }

    @Override
    public void rejectPending(PendingApproval approval) {
        ChangeState change = new ChangeState("rejected", null, null);
        PendingApprovalResp resp = ent.http.put("/api/v2/" + coin + "/pendingapprovals/" + approval.id, change, PendingApprovalResp.class);
    }

    //helper class, for parsing a coin.
    static class WalletNode {
        public String id, coin, label;
        public int approvalsRequired;
        public String balanceString;
        public CoinSpecific coinSpecific;
        public WalletUser[] users;

        static class CoinSpecific {
            public String baseAddress;
        }
    }

    static class WalletUser {
        public String user;
        public BitgoUser.Perm[] permissions;
    }

}
