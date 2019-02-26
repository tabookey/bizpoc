package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.EnterpriseInfo;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BitgoEnterprise implements IBitgoEnterprise {
    HttpReq http;
    private EnterpriseInfo info;
    private ArrayList<BitgoUser> users;

    public BitgoEnterprise(String accessKey, boolean test) {
        http = new HttpReq(accessKey, true);
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
        UserMeResp.User u = http.get("/api/v2/user/me", UserMeResp.class).user;

        boolean isAdmin = false;
        for (UserMeResp.Enterprise e : u.enterprises ) {
            if ( e.id.equals(getInfo().id) && e.permissions.contains("admin") )
                isAdmin=true;
        }

        BitgoUser user = new BitgoUser(u.id, u.username, u.name.full, isAdmin, Collections.emptyList());

        return user;
    }

    @Override
    public ExchangeRate getMarketData() {
        JsonNode node = http.get("/api/v2/teth/market/latest", JsonNode.class).get("latest").get("currencies").get("USD").get("24h_avg");
        ExchangeRate e = new ExchangeRate(node.asDouble());
        return e;
    }

    @Override
    public List<IBitgoWallet> getWallets(String coin) {
        JsonNode node = http.get("/api/v2/"+coin+"/wallet", JsonNode.class).get("wallets");

        ArrayList<IBitgoWallet> wallets = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            Wallet w = new Wallet(this, node.get(i), coin);
            wallets.add(w);
        }
        return wallets;
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
