package com.tabookey.bizpoc.impl;

import com.tabookey.bizpoc.Approval;
import com.tabookey.bizpoc.ApprovalState;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * merged wallet - represent all tokens supported by this wallet.
 */
class MergedWallet implements IBitgoWallet {

    private final BitgoEnterprise ent;
    private final HashMap<String, String> balances = new HashMap<>();

    //coin-specific wallets
    private final HashMap<String, Wallet> wallets = new HashMap<>();
    private final Wallet ethWallet;
    MergedWalletsData.WalletData data;
    ArrayList<String> coins;

    public MergedWallet(BitgoEnterprise ent, MergedWalletsData.WalletData walletData) {
        this.data = walletData;
        this.ent = ent;
        this.ethWallet = getCoinWallet(data.coin);

        coins = new ArrayList<>();
        coins.add(data.coin);
        balances.put(data.coin, data.balanceString);

        for (String tokenName : walletData.tokens.keySet()) {
            MergedWalletsData.TokenData token = walletData.tokens.get(tokenName);
            if (token.balanceString.equals("0") && token.transferCount == 0)
                continue;

            coins.add(tokenName);
            balances.put(tokenName, token.balanceString);
        }
    }


    private Wallet getCoinWallet(String coin) {

        Wallet w = wallets.get(coin);
        if (w == null) {
            List<IBitgoWallet> wlist = ent.getWallets(coin);
            for (IBitgoWallet w1 : wlist) {
                if (w1.getId().equals(data.id)) {
                    return (Wallet) w1;
                }
            }
        }
        throw new RuntimeException("wallet not found?");
    }

    @Override
    public String getId() {
        return data.id;
    }

    @Override
    public List<String> getCoins() {
        return coins;
    }

    @Override
    public String getLabel() {
        return data.label;
    }

    @Override
    public List<BitgoUser> getGuardians() {
        return ethWallet.getGuardians();
    }

    @Override
    public String getBalance(String coin) {
        return balances.get(coin);
    }

    @Override
    public String getAddress() {
        return ethWallet.getAddress();
    }

    static class AuditResp {
        public Log[] logs;

        static class Log {
            public String id, user, ip, walletId, type, coin, target;
            public Date date;
            public Data data;

            public String getRecipient() {
                try {
                    return data.recipients[0].address;
                } catch (Exception e) {
                    return null;
                }
            }

        }

        static class Data {
            public String amount;
            public Recipient[] recipients;
        }

        static class Recipient {
            public String address;
        }

    }


    private List<Transfer> getAuditRejected(int limit) {
        if (limit == 0) {
            limit = 1000;
        }
        StringBuilder coinParams = new StringBuilder();
        for (String coin :
                getCoins()) {
            coinParams.append("coin=").append(coin).append("&");
        }
        AuditResp resp = getAuditWithParams(limit, coinParams, "rejectTransaction");
        AuditResp appr = getAuditWithParams(limit, coinParams, "approveTransaction");
        return Arrays.stream(resp.logs).map(log -> {
            ArrayList<String> approvals = new ArrayList<>();
            for (AuditResp.Log apprLog : appr.logs) {
                if (apprLog.target.equals(log.target)) {
                    approvals.add(apprLog.user);
                }
            }
            return new Transfer(log.id,
                    null, log.data.amount, log.coin, null, log.date, log.getRecipient(), null, ent.getToken(log.coin),
                    log.user.equals(Global.ent.getMe().id) ? ApprovalState.CANCELLED : ApprovalState.DECLINED, log.user, approvals);
        }).collect(Collectors.toList());
    }

    private AuditResp getAuditWithParams(int limit, StringBuilder coinParams, String type) {
        return ent.http.get("/api/v2/auditlog?limit=%d&type=" + type + "&%swalletId=%s", AuditResp.class,
                limit, coinParams, this.getId());
    }


    @Override
    public List<Transfer> getTransfers(int limit) {

        ArrayList<Transfer> ret = new ArrayList<>();


        String request = "/api/v2/" + Global.ent.coinName() + "/wallet/" + ethWallet.id + "/transfer?allTokens=true";
        // TODO: use requests library to work with URL parameters.
        if (limit != 0) {
            request += "&limit=" + limit;
        }
        Wallet.TransferResp resp = ent.http.get(request, Wallet.TransferResp.class);
        ArrayList<Transfer> xfers = new ArrayList<>();
        for (Wallet.TransferResp.Trans t : resp.transfers) {
            Transfer tx = new Transfer(t.id, t.txid, t.valueString, t.coin, t.usd, t.date, null, t.comment, ent.getToken(t.coin), ApprovalState.APPROVED, null, new ArrayList<>());
            // entries have the add/sub of each transaction "participant".
            // on ethereum there are exactly 2 such participants. one is our wallet, so we're
            // looking for the other one, with its value different (actually, negative) of ours.
            for (Wallet.TransferResp.Entry e : t.entries) {
                if (!e.valueString.equals(tx.valueString)) {
                    tx.remoteAddress = e.address;
                    break;
                }
            }
            xfers.add(tx);
        }
        ret.addAll(xfers);
        ret.addAll(getAuditRejected(limit));

        ret.sort((a, b) -> b.date.compareTo(a.date));
        if (limit != 0 && ret.size() > limit) {
            return ret.subList(0, limit);
        }
        return ret;
    }

    @Override
    public void update(Runnable onChange) {
    }

    @Override
    public String sendCoins(SendRequest req, StatusCB cb) {
        if (!getCoins().contains(req.tokenInfo.coin))
            throw new IllegalArgumentException("Unsopported coin \"" + req.tokenInfo.coin + "\": not one of " + getCoins());
        return getCoinWallet(req.tokenInfo.coin).sendCoins(req, cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return ethWallet.checkPassphrase(passphrase);
    }

    public BitgoUser getUserById(String id) {
        for (BitgoUser u : ent.getUsers()) {
            if (u.id.equals(id))
                return u;
        }
        return null;
    }

    @Override
    public List<PendingApproval> getPendingApprovals() {
        Wallet.PendingApprovalResp resp = ent.http.get("/api/v2/" + data.coin + "/pendingapprovals?allTokens=true", Wallet.PendingApprovalResp.class);
        ArrayList<PendingApproval> ret = new ArrayList<>();
        for (Wallet.PendingApprovalResp.PendingApproval r : resp.pendingApprovals) {
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
                for (Wallet.PendingApprovalResp.Resolver rs : r.resolvers) {
                    approvedBy.add(getUserById(rs.user));
                }
                p.approvedByUsers = approvedBy;
            }
            ret.add(p);
        }

        ret.sort((a, b) -> a.createDate.compareTo(b.createDate));
        return ret;
    }

    @Override
    public void rejectPending(PendingApproval approval) {
        Wallet wallet = getCoinWallet(approval.token.getTokenCode());
        wallet.rejectPending(approval);
    }
}
