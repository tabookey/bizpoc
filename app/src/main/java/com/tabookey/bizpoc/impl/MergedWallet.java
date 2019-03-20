package com.tabookey.bizpoc.impl;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
        this.ent=ent;
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
        if ( w==null ) {
            List<IBitgoWallet> wlist = ent.getWallets(coin);
            for ( IBitgoWallet w1 :wlist ) {
                if ( w1.getId().equals(data.id) ) {
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

    @Override
    public List<Transfer> getTransfers() {

        ArrayList<Transfer> ret = new ArrayList();
        for ( String c : coins ) {
            ret.addAll(getCoinWallet(c).getTransfers());
        }
        ret.sort((a, b) -> a.date.compareTo(b.date));
        return ret;
    }

    @Override
    public String sendCoins(SendRequest req, StatusCB cb) {
        if ( !getCoins().contains(req.coin))
            throw new IllegalArgumentException("Unsopported coin \""+req.coin+"\": not one of "+getCoins());
        return getCoinWallet(req.coin).sendCoins(req,cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return ethWallet.checkPassphrase(passphrase);
    }

    @Override
    public List<PendingApproval> getPendingApprovals() {

        ArrayList<PendingApproval> ret = new ArrayList();
        for ( String c : coins ) {
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
