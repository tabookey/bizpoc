package com.tabookey.bizpoc.impl;

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
            public String user, ip, walletId, type, coin;
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


    List<Transfer> getAuditRejected(int limit) {
        if (limit == 0){
            limit = 1000;
        }
        AuditResp resp = ent.http.get("/api/v2/auditlog?limit=" + limit + "&type=rejectTransaction&coin=%s&walletId=%s", AuditResp.class,
                ent.baseCoin.coin, this.getId());

        return Arrays.asList(resp.logs).stream().map(log -> new Transfer(
                null, log.data.amount, log.coin, null, log.date, log.getRecipient(), null, ent.getToken(log.coin)
        )).collect(Collectors.toList());
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
            Transfer tx = new Transfer(t.txid, t.valueString, t.coin, t.usd, t.date, null, t.comment, ent.getToken(t.coin));
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
    public String sendCoins(SendRequest req, StatusCB cb) {
        if (!getCoins().contains(req.coin))
            throw new IllegalArgumentException("Unsopported coin \"" + req.coin + "\": not one of " + getCoins());
        return getCoinWallet(req.coin).sendCoins(req, cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return ethWallet.checkPassphrase(passphrase);
    }

    @Override
    public List<PendingApproval> getPendingApprovals() {

        ArrayList<PendingApproval> ret = new ArrayList();
        for (String c : coins) {
            ret.addAll(getCoinWallet(c).getPendingApprovals());
        }
        ret.sort((a, b) -> a.createDate.compareTo(b.createDate));
        return ret;
    }

    @Override
    public void rejectPending(PendingApproval approval) {
        ethWallet.rejectPending(approval);
    }
}
