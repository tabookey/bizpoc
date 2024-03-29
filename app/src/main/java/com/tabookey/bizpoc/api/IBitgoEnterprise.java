package com.tabookey.bizpoc.api;

import java.util.List;
import java.util.Map;

/**
 * interface of a single enterprise.
 */
public interface IBitgoEnterprise {

    String getEntId();

    //return the user object representing the current user
    BitgoUser getMe();

    public Map<String,Double> getAllExchangeRates();

    ExchangeRate getMarketData(String coin);

    /**
     * update all items that might change over time:
     * - exchange rates.
     */
    public void update(Runnable onChange);

    List<IBitgoWallet> getWallets(String coin);

    //return specific user by ID, or "null" if not found.
    // @param withFullName - if true, make an extra call to fill in full name.
    BitgoUser getUserById(String id, boolean withFullName);

    //return a list of wallets - each wallet object for ether and all tokens available on that wallet address
    List<IBitgoWallet> getMergedWallets();

    /**
     * get the base coin (eth or teth)
     */
    TokenInfo getBaseCoin();

    /**
     * get specific token, (or even BaseCoin itself)
     * @param token - token name, or {@code getBaseCoin()}
     */
    TokenInfo getToken(String token);

    Map<String,TokenInfo> getTokens();

    String coinName();
}