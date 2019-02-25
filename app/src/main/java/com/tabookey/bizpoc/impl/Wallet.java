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
    public List<Transfer> getTransfers() {
        TransferResp resp = ent.http.get("/api/v2/teth/wallet/" + id + "/transfer", TransferResp.class);
        ArrayList<Transfer> xfers = new ArrayList<>();
        xfers.addAll(Arrays.asList(resp.transfers));
        return xfers;
    }

    @Override
    public void sendMoney(SendRequest req) {
        throw new NoSuchMethodError("no money transfer yet");
    }

    static class PendingApprovalResp {
        
    }
    @Override
    public List<PendingApproval> getPendingApprovals() {
        PendingApprovalResp pr = ent.http.get("/api/v2/teth/pendingapprovals", PendingApprovalResp.class);
        return null;
    }

    @Override
    public void approvePending(PendingApproval approval) {

    }

    @Override
    public void rejectPending(PendingApproval approval) {

    }
}
