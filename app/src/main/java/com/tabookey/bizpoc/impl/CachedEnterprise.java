package com.tabookey.bizpoc.impl;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.EnterpriseInfo;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.TokenInfo;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedEnterprise implements IBitgoEnterprise {
    private final BitgoEnterprise networkEnterprise;
    private final Runnable ratesChange;
    private final Runnable walletChange;
    TokenInfo baseCoin;
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

    public CachedEnterprise(BitgoEnterprise networkEnterprise, Runnable walletChange, Runnable ratesChange) {
        this.ratesChange =ratesChange;
        this.walletChange = walletChange;
        this.networkEnterprise = networkEnterprise;
        baseCoin = networkEnterprise.getBaseCoin();
        info = networkEnterprise.getInfo();
        userMe = networkEnterprise.getMe();
        users = new ArrayList<>(networkEnterprise.getUsers());
        allTokensInfo = new HashMap<>(networkEnterprise.getTokens());
        IBitgoWallet netwallet = networkEnterprise.getMergedWallets().get(0);

        theWallet = new CachedWallet(netwallet, walletChange);
        update();
    }

    /**
     * update all items that might change over time:
     * - exchange rates.
     */
    public void update() {
        HashMap<String, Double> newExchangeRates = new HashMap<>(networkEnterprise.getAllExchangeRates());
        if ( newExchangeRates.equals(allExchangeRates))
            return;
        ratesChange.run();
    }


    @Override
    public Map<String, TokenInfo> getTokens() {

        return allTokensInfo;
    }

    //TODO: read tokens, decimals from:  https://test.bitgo.com/api/v1/client/constants
    @Override
    public EnterpriseInfo getInfo() {
        return info;
    }

    @Override
    public BitgoUser getMe() {

        return userMe;
    }

    /**
     * return market value of all known currencies and tokens.
     * @return map of token=>usd-value
     */
    public Map<String,Double> getAllExchangeRates() {
        return allExchangeRates;
    }

    @Override
    public ExchangeRate getMarketData(String coin) {

        return new ExchangeRate(getAllExchangeRates().get(coin));
    }

    public List<IBitgoWallet> getMergedWallets() {
        return Arrays.asList(theWallet);
    }

    @NotNull
    public String coinName() {
        return baseCoin.coin;
    }

    @Override
    public List<IBitgoWallet> getWallets(String coin) {
        return Arrays.asList(theWallet);
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
        return users;
    }
}
