package com.tabookey.bizpoc.impl;

import android.os.Looper;
import android.os.NetworkOnMainThreadException;

import com.tabookey.logs.Log;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.EnterpriseInfo;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.TokenInfo;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedEnterprise implements IBitgoEnterprise {
    private final BitgoEnterprise networkEnterprise;
    private TokenInfo baseCoin;
    private EnterpriseInfo info;
    private ArrayList<BitgoUser> users;
    private BitgoUser userMe;

    private HashMap<String, TokenInfo> allTokensInfo;
    private CachedWallet theWallet;
    private Map<String, Double> allExchangeRates;

    @Override
    public TokenInfo getToken(String token) {
        return getTokens().get(token);
    }

    public CachedEnterprise(BitgoEnterprise networkEnterprise) {
        this.networkEnterprise = networkEnterprise;
        baseCoin = networkEnterprise.getBaseCoin();
    }

    private void init() {
        if (Looper.getMainLooper().isCurrentThread()){
            throw new NetworkOnMainThreadException();
        }
        if (info != null)
            return;
        info = networkEnterprise.getInfo();
        userMe = networkEnterprise.getMe();
        users = new ArrayList<>(networkEnterprise.getUsers());
        allTokensInfo = new HashMap<>(networkEnterprise.getTokens());
        update(null);

        getCachedWallet();
    }

    private CachedWallet getCachedWallet() {
        if (theWallet == null)
            theWallet = new CachedWallet(networkEnterprise.getMergedWallets().get(0));
        return theWallet;
    }

    /**
     * update all items that might change over time:
     * - exchange rates.
     */
    public void update(Runnable onChange) {
        HashMap<String, Double> newExchangeRates = new HashMap<>(networkEnterprise.getAllExchangeRates());
        if (allExchangeRates != null && newExchangeRates.toString().equals(allExchangeRates.toString()))
            return;

        Log.d("cache", "OLD: " + allExchangeRates);
        Log.d("cache", "NEW: " + newExchangeRates);
        allExchangeRates = newExchangeRates;

        if (onChange != null)
            onChange.run();
    }


    @Override
    public Map<String, TokenInfo> getTokens() {
        init();
        return allTokensInfo;
    }

    //TODO: read tokens, decimals from:  https://test.bitgo.com/api/v1/client/constants
    @Override
    public EnterpriseInfo getInfo() {
        init();
        return info;
    }

    @Override
    public BitgoUser getMe() {
        init();
        return userMe;
    }

    /**
     * return market value of all known currencies and tokens.
     *
     * @return map of token=>usd-value
     */
    public Map<String, Double> getAllExchangeRates() {
        init();
        return allExchangeRates;
    }

    @Override
    public ExchangeRate getMarketData(String coin) {
        init();
        return new ExchangeRate(getAllExchangeRates().get(coin));
    }

    public List<IBitgoWallet> getMergedWallets() {
        init();
        return Collections.singletonList(getCachedWallet());
    }

    @Override
    public TokenInfo getBaseCoin() {
        return networkEnterprise.getBaseCoin();
    }

    @NotNull
    public String coinName() {
        return baseCoin.coin;
    }

    @Override
    public List<IBitgoWallet> getWallets(String coin) {
        init();
        return Collections.singletonList(getCachedWallet());
    }

    public BitgoUser getUserById(String id, boolean withFullName) {
        for (BitgoUser u : getUsers()) {
            if (u.id.equals(id)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<BitgoUser> getUsers() {
        init();
        return users;
    }
}
