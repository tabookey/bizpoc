package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.exceptions.OnErrorNotImplementedException;

class Wallet implements IBitgoWallet {
    private final BitgoEnterprise ent;
    private final String balanceString;
    String id, label;
    int approvalsRequired;
    ArrayList<BitgoUser> users;

    Wallet(BitgoEnterprise ent, JsonNode node) {

        this.ent = ent;
        this.id = node.get("id").asText();
        this.label = node.get("label").asText();
        this.approvalsRequired = node.get("approvalsRequired").asInt();
        this.balanceString = node.get("balanceString").asText();

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
    public String getBalance(String coin) {
        if (coin.contains("eth"))
            return balanceString;
        throw new RuntimeException("need to support token balance");
    }

    static class TransferResp {
        public Transfer[] transfers;
//        static class Trans {
//            public String txid, coin, valueString, usd, createdDate, confirmedDate;
//        }
    }

    @Override
    public List<Transfer> getTransfers(String coin) {
        TransferResp resp = ent.http.get("/api/v2/"+coin+"/wallet/" + id + "/transfer", TransferResp.class);
        ArrayList<Transfer> xfers = new ArrayList<>();
        xfers.addAll(Arrays.asList(resp.transfers));
        return xfers;
    }

    @Override
    public void sendMoney(SendRequest req) {
        throw new NoSuchMethodError("no money transfer yet");
    }

    static class PendingApprovalResp {

        public PendingApproval[] pendingApprovals;
        static class PendingApproval {
            public String id, coin, creator, createDate;
            public Info info;
            public String state, scope;
            public int approvalsRequired;
            public String[] userIds;
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

    }
    @Override
    public List<PendingApproval> getPendingApprovals() {
        PendingApprovalResp resp = ent.http.get("/api/v2/teth/pendingapprovals", PendingApprovalResp.class);
        ArrayList<PendingApproval> ret = new ArrayList<>();
        for ( PendingApprovalResp.PendingApproval r : resp.pendingApprovals ) {
            PendingApproval p = new PendingApproval();
            p.createDate = r.createDate;
            p.coin = r.coin;
            p.creator = getUserById(r.creator);
            p.recipientAddr = r.info.transactionRequest.recipients[0].address;
            p.amount = r.info.transactionRequest.recipients[0].amount;
            ret.add(p);
        }
        return ret;
    }

    @Override
    public void approvePending(PendingApproval approval) {

    }

    @Override
    public void rejectPending(PendingApproval approval) {

    }
}
