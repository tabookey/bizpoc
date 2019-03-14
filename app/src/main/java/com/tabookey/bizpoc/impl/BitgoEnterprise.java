package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.EnterpriseInfo;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BitgoEnterprise implements IBitgoEnterprise {
    private final boolean testNetwork;
    HttpReq http;
    private EnterpriseInfo info;
    private ArrayList<BitgoUser> users;
    private HashMap<String,List<IBitgoWallet>> wallets = new HashMap<>();
    private BitgoUser userMe;

    String[] validTokens = { "terc", "tbst" }; //teth always added
    public BitgoEnterprise(String accessKey, boolean test) {
        this.testNetwork=test;
        Global.http = http = new HttpReq(accessKey, true);
    }


    static class EntResp {
        public EnterpriseInfo[] enterprises;
    }

    @Override
    public EnterpriseInfo getInfo() {
        if ( info==null ) {
            EntResp res = http.get("/api/v1/enterprise", EntResp.class);
            info = res.enterprises[0];
        }
        return info;
    }

    static class UserMeResp {
        public User user;
        static class User {
            public String id, username;
            public UserName name;
            public Enterprise[] enterprises;
        }

        static class Enterprise {
            public String id;
            public ArrayList<String> permissions;
        }

        static class UserName {
            public String first, last, full;
        }
    }

    @Override
    public BitgoUser getMe() {
        if ( userMe==null ) {
            UserMeResp.User u = http.get("/api/v2/user/me", UserMeResp.class).user;

            boolean isAdmin = false;
            for (UserMeResp.Enterprise e : u.enterprises) {
                if (e.id.equals(getInfo().id) && e.permissions.contains("admin"))
                    isAdmin = true;
            }

            userMe = new BitgoUser(u.id, u.username, u.name.full, isAdmin, Collections.emptyList());
        }
        return userMe;
    }

    @Override
    public ExchangeRate getMarketData() {
        JsonNode node = http.get("/api/v2/teth/market/latest", JsonNode.class).get("latest").get("currencies").get("USD").get("24h_avg");
        ExchangeRate e = new ExchangeRate(node.asDouble());
        return e;
    }

    public List<IBitgoWallet> getMergedWallets() {
        String coin = testNetwork ? "teth" : "eth";
        //must specify at least one coin name, to get back all tokens.
        MergedWalletsData data = http.get("/api/v2/wallets/merged?coin="+coin+"&enterprise=" + info.id, MergedWalletsData.class);

        ArrayList<IBitgoWallet> ret = new ArrayList<>();
        for( MergedWalletsData.WalletData walletData : data.wallets) {
            ret.add(new MergedWallet(this,walletData));
        }
        return ret;
    }

    @Override
    public List<IBitgoWallet> getWallets(String coin) {
        if ( wallets.get(coin)!=null )
            return wallets.get(coin);

        JsonNode node = http.get("/api/v2/"+coin+"/wallet", JsonNode.class).get("wallets");

        ArrayList<IBitgoWallet> cointWallets = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            Wallet w = new Wallet(this, node.get(i), coin);
            cointWallets.add(w);
        }
        wallets.put(coin, cointWallets);
        return wallets.get(coin);
    }

    public static class UserResp {
        public String id, username;
        public boolean isActive, verified;
        public Email email;

        public static class Email {
            public String email, verified;
        }
    }
    public static class EntUsersResp {
        public UserResp[] adminUsers;
        public UserResp[] nonAdminUsers;
    }

    public BitgoUser getUserById(String id) {
        for (BitgoUser u : getUsers()) {
            if (u.id.equals(id))
                return u;
        }
        return null;
    }

    @Override
    public List<BitgoUser> getUsers() {
        if ( users==null ) {
            ArrayList<BitgoUser> newusers = new ArrayList<>();

            String ent_id = getInfo().id;
            EntUsersResp resp = http.get("/api/v1/enterprise/" + ent_id + "/user", EntUsersResp.class);

            for (UserResp u : resp.adminUsers) {
                BitgoUser user = new BitgoUser(u.id, u.email.email, u.username, true, null);
                newusers.add(user);
            }
            for (UserResp u : resp.nonAdminUsers) {
                BitgoUser user = new BitgoUser(u.id, u.email.email, u.username, false, null);
                newusers.add(user);
            }
            users = newusers;
        }

        return users;
    }
}
