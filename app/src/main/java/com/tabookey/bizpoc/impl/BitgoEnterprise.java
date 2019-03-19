package com.tabookey.bizpoc.impl;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.R;
import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.EnterpriseInfo;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoEnterprise;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.TokenInfo;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tabookey.bizpoc.impl.Utils.fromJson;
import static com.tabookey.bizpoc.impl.Utils.toJson;

public class BitgoEnterprise implements IBitgoEnterprise {
    private final boolean testNetwork;
    HttpReq http;
    private EnterpriseInfo info;
    private ArrayList<BitgoUser> users;
    private HashMap<String,List<IBitgoWallet>> wallets = new HashMap<>();
    private BitgoUser userMe;

    String[] validTokens = { "terc", "tbst" }; //teth always added
    private HashMap<String, TokenInfo> allTokensInfo;

    public BitgoEnterprise(String accessKey, boolean test) {
        this.testNetwork=test;
        Global.http = http = new HttpReq(accessKey, true);
    }


    static class EntResp {
        public EnterpriseInfo[] enterprises;
    }

    static class BitgoConstants {
        public int ttl;
        public Constants constants;

        static class Constants {
            public String bitgoEthAddress;
            public CoinInfo teth;
            public CoinInfo eth;
        }

        static class CoinInfo {
            public TokenInfo[] tokens;
        }

    }

    static class BitgoToken {

        public String name, logo, fullDisplay, shortDisplay;
    }

    @Override
    public Map<String, TokenInfo> getTokens() {

        if (allTokensInfo == null) {
            allTokensInfo = new HashMap<>();
            BitgoConstants bitgoConstants = http.get("/api/v1/client/constants", BitgoConstants.class);
            BitgoConstants.CoinInfo allcoins = testNetwork ? bitgoConstants.constants.teth : bitgoConstants.constants.eth;

            for ( TokenInfo info : allcoins.tokens ) {
                allTokensInfo.put(info.type, info);
            }

            //bitgo_tokens generated with (NOTE: devTokens is for debug, ercTokens for release
            /*
            export TOK=devTokens; (echo ethKeycardImage=null; curl --silent  'https://test.bitgo.com/js/bitgo-client.07ffdecf50d07643.js' | sed -n "/$TOK.=/,/^$/p"; echo "console.log(JSON.stringify($TOK))"  )|node > bitgo_tokens_testnet.txt
            export TOK=ercTokens; (echo prodErcTokenBaseConfig=null; curl --silent  'https://test.bitgo.com/js/bitgo-client.07ffdecf50d07643.js' | sed -n "/$TOK.=/,/^$/p"; echo "console.log(JSON.stringify($TOK))"  )|node > bitgo_tokens.txt

            NOTE: real production URL: https://www.bitgo.com/js/bitgo-client.5a72c7a810b67c8d.js (but generates the same tokens lsit)
            */

            InputStream is = Global.applicationContext.getResources().openRawResource(
                    testNetwork ? R.raw.bitgo_tokens_testnet : R.raw.bitgo_tokens);
            try {
                String json = IOUtils.toString(is, "UTF-8");

                BitgoToken[] bitgoTokens = fromJson(json, new BitgoToken[0].getClass());
                for ( BitgoToken t : bitgoTokens) {
                    TokenInfo tokenInfo = allTokensInfo.get(t.name);
                    if ( tokenInfo!=null ) {
                        tokenInfo.logo = "https://www.bitgo.com/img/"+t.logo;
                        tokenInfo.fullDisplay = t.fullDisplay;
                        tokenInfo.shortDisplay = t.shortDisplay;
                    }
                }
            } catch (Exception e) {
                Log.w("TAG", "getTokens: failed  o read logo, extra info" );
            }


        }
        TokenInfo eth = new TokenInfo("", "eth", "eth", "", 18, "Ethereum", "https://cdn.iconscout.com/icon/free/png-256/ethereum-3-569581.png", "Ethereum", "Ether");
        TokenInfo teth = new TokenInfo("", "teth", "eth", "", 18, "tEthereum", "https://cdn.iconscout.com/icon/free/png-256/ethereum-3-569581.png", "tEthereum", "tEther");
        allTokensInfo.put("teth", teth);
        return Collections.unmodifiableMap(allTokensInfo);
    }


    //TODO: read tokens, decimals from:  https://test.bitgo.com/api/v1/client/constants
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
            public OtpDevice[] otpDevices;
            public String timezone;
        }

        static class Enterprise {
            public String id;
            public ArrayList<String> permissions;
        }

        static class UserName {
            public String first, last, full;
        }
        static class OtpDevice {
            public BitgoUser.OtpType type;
            public String label;
            public boolean verified;
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
            ArrayList<BitgoUser.OtpType> otpTypes = new ArrayList<>();
            for (UserMeResp.OtpDevice otp : u.otpDevices ) {
                otpTypes.add(otp.type);
            }
            userMe.otpTypes = otpTypes;
        }
        return userMe;
    }

    @Override
    public ExchangeRate getMarketData(String coin) {
        JsonNode node = http.get("/api/v2/" + coin + "/market/latest", JsonNode.class).get("latest").get("currencies").get("USD").get("24h_avg");
        ExchangeRate e = new ExchangeRate(node.asDouble());
        return e;
    }

    public List<IBitgoWallet> getMergedWallets() {
        String coin = testNetwork ? "teth" : "eth";
        //must specify at least one coin name, to get back all tokens.
        MergedWalletsData data = http.get("/api/v2/wallets/merged?coin="+coin+"&enterprise=" + getInfo().id, MergedWalletsData.class);

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

        Log.w("TAG", "getTokens: " + toJson(getTokens()));
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
