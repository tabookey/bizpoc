package com.tabookey.bizpoc.api;

import java.util.List;

/**
 * interface of a single enterprise.
 */
public interface IBitgoEnterprise {

    //current enterprise ID
    EnterpriseInfo getInfo();

    //return the user object representing the current user
    BitgoUser getMe();

    ExchangeRate getMarketData();

    List<IBitgoWallet> getWallets(String coin);

    //return enterprise users (NOTE: "role" are not set on enterprise-level, only on wallet level)
    List<BitgoUser> getUsers();

    //return specific user by ID, or "null" if not found.
    BitgoUser getUserById(String id);

    //return a list of wallets - each wallet object for ether and all tokens available on that wallet address
    List<IBitgoWallet> getMergedWallets();
}