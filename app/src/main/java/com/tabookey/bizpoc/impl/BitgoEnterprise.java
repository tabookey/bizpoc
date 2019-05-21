package com.tabookey.bizpoc.impl;

import com.tabookey.logs.Log;

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
import org.jetbrains.annotations.NotNull;

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
    final TokenInfo baseCoin;
    HttpReq http;
    private String entid;
    private HashMap<String, BitgoUser> users = new HashMap<>();
    private HashMap<String,List<IBitgoWallet>> wallets = new HashMap<>();
    private BitgoUser userMe;

    private HashMap<String, TokenInfo> allTokensInfo;

    public BitgoEnterprise(String accessKey, boolean test) {
        this.testNetwork=test;
        Global.http = http = new HttpReq(accessKey, test);
        if ( testNetwork ) {
            baseCoin = new TokenInfo("teth", "teth", "Kovan", "", 18, "tEthereum", "https://cdn.iconscout.com/icon/free/png-256/ethereum-3-569581.png", "tEthereum", "tEther");
        }
        else {
            baseCoin = new TokenInfo("eth", "eth", "mainnet", "", 18, "Ethereum", "https://cdn.iconscout.com/icon/free/png-256/ethereum-3-569581.png", "Ethereum", "Ether");
        }
    }

    public TokenInfo getBaseCoin() {
        return baseCoin;
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
    public TokenInfo getToken(String token) {
        return getTokens().get(token);
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

            allTokensInfo.put(baseCoin.coin, baseCoin );
        }

        return Collections.unmodifiableMap(allTokensInfo);
    }


    //TODO: read tokens, decimals from:  https://test.bitgo.com/api/v1/client/constants
    @Override
    public String getEntId() {
        getMe();
        return entid;
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

    public String getFullName(String userid) {
        UserMeResp.User u = http.get("/api/v2/user/"+userid, UserMeResp.class).user;
        return u.name==null ? null : u.name.full;
    }

    @Override
    public BitgoUser getMe() {
        if ( userMe==null ) {
            UserMeResp.User u = http.get("/api/v2/user/me", UserMeResp.class).user;

            entid = u.enterprises[0].id;

            userMe = new BitgoUser(u.id, u.username, u.name.full, Collections.emptyList());
            ArrayList<BitgoUser.OtpType> otpTypes = new ArrayList<>();
            for (UserMeResp.OtpDevice otp : u.otpDevices ) {
                otpTypes.add(otp.type);
            }
            userMe.otpTypes = otpTypes;
            users.put(u.id, userMe);
        }
        return userMe;
    }

    /**
     * return market value of all known currencies and tokens.
     * @return map of token=>usd-value
     */
    public Map<String,Double> getAllExchangeRates() {
        HashMap<String,Double> ret = new HashMap<>();
        String currency="USD";
        String fieldName = "24h_avg"; //"last", "prevDayLow", "prevDayHigh" and more..
        JsonNode data = http.get("/api/v2/market/latest?allTokens=true&coin="+coinName(), JsonNode.class).get("marketData").get(0);
        ret.put(data.get("coin").asText(), data.get("currencies").get(currency).get(fieldName).asDouble());

        getTokens().keySet().forEach(token->{
            try {
                JsonNode tokenData = data.get("tokens").get(token.toUpperCase());
                double val = tokenData.get("currencies").get(currency).get(fieldName).asDouble();
                ret.put(token, val);
            } catch (Exception ignore) {
                //ignore tokens without value
            }
        });

        return ret;
    }

    @Override
    public ExchangeRate getMarketData(String coin) {
        JsonNode node = http.get("/api/v2/" + coin + "/market/latest", JsonNode.class).get("latest").get("currencies").get("USD").get("24h_avg");
        ExchangeRate e = new ExchangeRate(node.asDouble());
        return e;
    }

    @Override
    public void update(Runnable onChange) {
    }

    public List<IBitgoWallet> getMergedWallets() {
        String coin = coinName();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-safetynet", Global.getSafetynetResponseJwt());
        //must specify at least one coin name, to get back all tokens.
        MergedWalletsData data = http.get("/api/v2/wallets/merged?coin="+coin+"&enterprise=" + getEntId(), MergedWalletsData.class, headers);

        ArrayList<IBitgoWallet> ret = new ArrayList<>();
        for( MergedWalletsData.WalletData walletData : data.wallets) {
            ret.add(new MergedWallet(this,walletData));
        }
        return ret;
    }

    @NotNull
    public String coinName() {
        return testNetwork ? "teth" : "eth";
    }

    @Override
    public List<IBitgoWallet> getWallets(String coin) {
        if ( wallets.get(coin)!=null )
            return wallets.get(coin);

        Log.w("TAG", "getTokens: " + toJson(getTokens()));
        JsonNode node = http.get("/api/v2/"+coin+"/wallet", JsonNode.class).get("wallets");

        ArrayList<IBitgoWallet> cointWallets = new ArrayList<>();
        //we only handle a SINGLE wallet per user..
        Wallet w = new Wallet(this, node.get(0), coin);
        cointWallets.add(w);
        wallets.put(coin, cointWallets);
        return wallets.get(coin);
    }

    public static class UserByIdResp {
        public UserInfo user;
    }

    public static class UserInfo {
        public String id, username;
        public boolean isActive, verified;
        public Email email;
        public UserMeResp.UserName name;

        public static class Email {
            public String email, verified;
        }
    }
    public static class EntUsersResp {
        public UserInfo[] adminUsers;
        public UserInfo[] nonAdminUsers;
    }

    public BitgoUser getUserById(String id, boolean withFullName) {
        if( users.containsKey(id))
            return users.get(id);

        UserByIdResp resp = http.get("/api/v2/user/"+id, UserByIdResp.class);
        BitgoUser buser = null;
        UserInfo user = resp.user;
        if ( user !=null )
            buser = new BitgoUser(user.id, user.email.email, user.name.full);

        synchronized (users) {
            users.put(id, buser);
        }
        return buser;
    }

//    @Override
//    public List<BitgoUser> getUsers() {
//        if ( users==null ) {
//            ArrayList<BitgoUser> newusers = new ArrayList<>();
//
//            String ent_id = getEntId();
//            EntUsersResp resp = http.get("/api/v1/enterprise/" + ent_id + "/user", EntUsersResp.class);
//
//            for (UserInfo u : resp.adminUsers) {
//                BitgoUser user = new BitgoUser(u.id, u.email.email, u.username, null);
//                newusers.add(user);
//            }
//            for (UserInfo u : resp.nonAdminUsers) {
//                BitgoUser user = new BitgoUser(u.id, u.email.email, u.username, null);
//                newusers.add(user);
//            }
//            users = newusers;
//        }
//
//        return users;
//    }
}
