package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.List;

class Wallet implements IBitgoWallet {
    private final BitgoEnterprise ent;
    private final String balanceString;
    private final CoinSender coinSender;
    String coin, id, label;
    int approvalsRequired;
    ArrayList<BitgoUser> users;
    String address;

    Wallet(BitgoEnterprise ent, JsonNode node, String coin) {

        this.ent = ent;
        this.coin = coin;
        this.id = node.get("id").asText();
        this.label = node.get("label").asText();
        this.approvalsRequired = node.get("approvalsRequired").asInt();
        this.balanceString = node.get("balanceString").asText();
        this.address = node.get("coinSpecific").get("baseAddress").asText();
        coinSender = new CoinSender(Global.applicationContext, ent.http);
    }

    @Override
    public String getCoin() {
        return coin;
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
    public List<BitgoUser> getUsers() {
        return users;
    }

    public BitgoUser getUserById(String id) {
        for ( BitgoUser u : ent.getUsers() ) {
            if ( u.id.equals(id))
                return u;
        }
        return null;
    }

    @Override
    public String getBalance() {
        return balanceString;
    }

    @Override
    public String getAddress() {
        return address;
    }

    static class TransferResp {
        public Trans[] transfers;
        static class Trans {
            public String txid, coin, valueString, usd, date, comment;
            public Entry[] entries;
        }
        static class Entry {
            public String address, valueString;
        }
    }

    @Override
    public List<Transfer> getTransfers() {
        TransferResp resp = ent.http.get("/api/v2/"+coin+"/wallet/" + id + "/transfer", TransferResp.class);
        ArrayList<Transfer> xfers = new ArrayList<>();
        for ( TransferResp.Trans t : resp.transfers ) {
            Transfer tx = new Transfer(t.txid, t.valueString, t.coin, t.usd, t.date, null, t.comment);
/*
            tx.txid = t.txid;
            tx.coin = t.coin;
            tx.valueString = t.valueString;
            tx.usd = t.usd;
            tx.date = t.date;
*/
            //entries have the add/sub of each transaction "participant".
            // on ethereum there are exactly 2 such participants. one is our wallet, so we're
            // looking for the other one, with its value different (actually, negative) of ours.
            for ( TransferResp.Entry e : t.entries) {
                if ( !e.valueString.equals(tx.valueString)) {
                    tx.remoteAddress = e.address;
                    break;
                }
            }
            xfers.add(tx);
        }
        return xfers;
    }

    @Override
    public void sendCoins(SendRequest req) {
        coinSender.sendCoins(this, req.recipientAddress, req.amount, req.otp, req.walletPassphrase);
    }


    static class PendingApprovalResp {

        public PendingApproval[] pendingApprovals;
        static class PendingApproval {
            public String id, coin, creator, createDate;
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
        PendingApprovalResp resp = ent.http.get("/api/v2/"+coin+"/pendingapprovals", PendingApprovalResp.class);
        ArrayList<PendingApproval> ret = new ArrayList<>();
        for ( PendingApprovalResp.PendingApproval r : resp.pendingApprovals ) {
            PendingApproval p = new PendingApproval();
            p.id = r.id;
            p.createDate = r.createDate;
            p.coin = r.coin;
            p.creator = getUserById(r.creator);
            p.recipientAddr = r.info.transactionRequest.recipients[0].address;
            p.amount = r.info.transactionRequest.recipients[0].amount;
            if ( r.resolvers != null ) {
                ArrayList<BitgoUser> approvedBy = new ArrayList<BitgoUser>();
                for (PendingApprovalResp.Resolver rs : r.resolvers ) {
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
    public void rejectPending(PendingApproval approval, String otp) {
        ChangeState change = new ChangeState("rejected", otp, null);
        PendingApprovalResp resp = ent.http.put("/api/v2/"+coin+"/pendingapprovals/"+approval.id, change, PendingApprovalResp.class);
    }
}
