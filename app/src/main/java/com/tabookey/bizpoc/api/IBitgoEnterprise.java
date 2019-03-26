package com.tabookey.bizpoc.api;

import java.util.List;
import java.util.Map;

/**
 * interface of a single enterprise.
 */
public interface IBitgoEnterprise {

    //current enterprise ID
    EnterpriseInfo getInfo();

    //return the user object representing the current user
    BitgoUser getMe();

    public Map<String,Double> getAllExchangeRates();

    ExchangeRate getMarketData(String coin);

    List<IBitgoWallet> getWallets(String coin);

    //return enterprise users (NOTE: "role" are not set on enterprise-level, only on wallet level)
    List<BitgoUser> getUsers();

    //return specific user by ID, or "null" if not found.
    // @param withFullName - if true, make an extra call to fill in full name.
    BitgoUser getUserById(String id, boolean withFullName);

    //return a list of wallets - each wallet object for ether and all tokens available on that wallet address
    List<IBitgoWallet> getMergedWallets();

    TokenInfo getToken(String token);

    Map<String,TokenInfo> getTokens();
}