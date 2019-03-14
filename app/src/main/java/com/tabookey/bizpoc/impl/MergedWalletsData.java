package com.tabookey.bizpoc.impl;

import java.util.HashMap;

class MergedWalletsData {
    WalletData wallets[];

    static class WalletData {
        public String id, label, coin, balanceString;
//        public Wallet.WalletUser[] users;
        public HashMap<String, TokenData> tokens;
    }

    static class TokenData {
        public String balanceString;
        public int transferCount;
    }
}
