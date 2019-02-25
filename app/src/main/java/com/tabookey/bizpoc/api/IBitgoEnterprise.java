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

    List<IBitgoWallet> getWallets();

    //return enterprise users (NOTE: "role" is relevant only when called from wallet)
    List<BitgoUser> getUsers();

}
