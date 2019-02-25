package com.tabookey.bizpoc;

import java.util.List;

/**
 * interface of a single enterprise.
 */
public interface BitgoEnterprise {

    //current enterprise ID
    String getEnterprise();

    //return the user object representing the current user
    BitgoUser getMe();

    List<BitgoWallet> getWallets();

    //return enterprise users (NOTE: "role" is relevant only when called from wallet)
    List<BitgoUser> getUsers();
}
