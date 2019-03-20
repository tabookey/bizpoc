package com.tabookey.bizpoc.impl;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * single-coin wallet.
 * This is the low-level coin API.
 */
class Wallet implements IBitgoWallet {
    private final BitgoEnterprise ent;
    private final String balanceString;
    private final CoinSender coinSender;
    private final List<String> hiddenUsers = Arrays.asList(
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
        this.balanceString = walletData.balanceString;
        this.address = walletData.coinSpecific.baseAddress;
        this.guardians = new ArrayList<>();
        for (WalletUser user : walletData.users) {
            if (user.user.equals(ent.getMe().id))
                continue;
            BitgoUser g = new BitgoUser(ent.getUserById(user.user), Arrays.asList(user.permissions));
            if ( hiddenUsers.contains(g.email)) {
                Log.w("wallet", "hidden guardian:"+ g.email);
                continue;
            }
            guardians.add(g);
        }
        coinSender = new CoinSender(Global.applicationContext, ent.http);
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

    public BitgoUser getUserById(String id) {
        for (BitgoUser u : ent.getUsers()) {
            if (u.id.equals(id))
                return u;
        }
        return null;
    }

    @Override
    public String getBalance(String coin) {
        if (!coin.equals(this.coin))
            throw new RuntimeException("invalid coin " + coin + ". Wallet only has " + this.coin);
        return balanceString;
    }

    @Override
    public String getAddress() {
        return address;
    }

    static class TransferResp {
        public Trans[] transfers;

        static class Trans {
            public String txid, coin, valueString, usd, comment;
            public Date date;
            public Entry[] entries;
        }

        static class Entry {
            public String address, valueString;
        }
    }

    @Override
    public List<Transfer> getTransfers() {
        TransferResp resp = ent.http.get("/api/v2/" + coin + "/wallet/" + id + "/transfer", TransferResp.class);
        ArrayList<Transfer> xfers = new ArrayList<>();
        for (TransferResp.Trans t : resp.transfers) {
            Transfer tx = new Transfer(t.txid, t.valueString, t.coin, t.usd, t.date, null, t.comment, ent.getTokens().get(t.coin));
            //entries have the add/sub of each transaction "participant".
            // on ethereum there are exactly 2 such participants. one is our wallet, so we're
            // looking for the other one, with its value different (actually, negative) of ours.
            for (TransferResp.Entry e : t.entries) {
                if (!e.valueString.equals(tx.valueString)) {
                    tx.remoteAddress = e.address;
                    break;
                }
            }
            xfers.add(tx);
        }
        return xfers;
    }

    @Override
    public String sendCoins(SendRequest req, StatusCB cb) {
        return coinSender.sendCoins(this, req, cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return coinSender.checkPassphrase(this,passphrase,null);
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
        PendingApprovalResp resp = ent.http.get("/api/v2/" + coin + "/pendingapprovals", PendingApprovalResp.class);
        ArrayList<PendingApproval> ret = new ArrayList<>();
        for (PendingApprovalResp.PendingApproval r : resp.pendingApprovals) {
            if (r.info == null
                    || r.info.transactionRequest == null
                    || r.info.transactionRequest.recipients == null) {
                continue;
            }
            PendingApproval p = new PendingApproval();
            p.id = r.id;
            p.createDate = r.createDate;
            p.coin = r.coin;
            p.creator = getUserById(r.creator);
            p.recipientAddr = r.info.transactionRequest.recipients[0].address;
            p.amount = r.info.transactionRequest.recipients[0].amount;
            p.comment = r.info.transactionRequest.comment;
            p.token = Global.ent.getTokens().get(p.coin);
            if (r.resolvers != null) {
                ArrayList<BitgoUser> approvedBy = new ArrayList<BitgoUser>();
                for (PendingApprovalResp.Resolver rs : r.resolvers) {
                    approvedBy.add(getUserById(rs.user));
                }
                p.approvedByUsers = approvedBy;
            }
            ret.add(p);
        }
        return ret;
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
