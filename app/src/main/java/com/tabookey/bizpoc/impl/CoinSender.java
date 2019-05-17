package com.tabookey.bizpoc.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import com.tabookey.logs.Log;
import android.webkit.JavascriptInterface;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.utils.FakeSafetynetHelper;
import com.tabookey.bizpoc.utils.SafetyNetHelper;
import com.tabookey.bizpoc.utils.SafetynetHelperInterface;

import static com.tabookey.bizpoc.impl.Utils.fromJson;

public class CoinSender extends WebViewExecutor {

    public CoinSender(Context ctx, HttpReq http) {
        super(ctx, http, getSafetynetHelper());
    }

    static SafetynetHelperInterface getSafetynetHelper() {
        if (Global.getFakeSafetynet()) {
            return new FakeSafetynetHelper();
        } else {
            return new SafetyNetHelper();
        }
    }

    public String sendCoins(IBitgoWallet wallet, SendRequest req, IBitgoWallet.StatusCB cb) {
        AppObject appObject = new AppObject(wallet, req, cb);
        exec("www/sender.html", appObject);

        //max time to wait for final result
        synchronized (appObject) {
            try {
                appObject.wait(40 * 1000);
            } catch (InterruptedException e) {
            }
        }

        if (appObject.error != null)
            throw new RuntimeException(appObject.error);

        if (appObject.result == null) {
            //report timeout and last known status
            throw new RuntimeException("timed-out: " + appObject.status);
        }

        JsonNode node = fromJson(appObject.result, JsonNode.class);
        if (node.has("pendingApproval")) {
            return node.get("pendingApproval").get("id").asText();
        } else {
            //special case: configured to pass transactions without approval
            return node.get("transfer").get("id").asText();
        }

    }

    public boolean checkPassphrase(IBitgoWallet wallet, String passphrase, IBitgoWallet.StatusCB cb) {
        String coin = wallet.getCoins().get(0);
        TokenInfo tokenInfo = Global.ent.getTokens().get(coin);
        SendRequest req = new SendRequest(tokenInfo, null, null, null, passphrase, null);

        AppObject appObject = new AppObject(wallet, req, cb);
        exec("www/verify.html", appObject);

        //max time to wait for final result
        synchronized (appObject) {
            try {
                appObject.wait(40 * 1000);
            } catch (InterruptedException e) {
            }
        }

        if (appObject.error != null)
            return false;

        if (appObject.result == null) {
            //report timeout and last known status
            throw new RuntimeException("timed-out: " + appObject.status);
        }
        return true;
    }

    public static class AppObject {
        private final IBitgoWallet.StatusCB cb;
        private final SendRequest req;
        IBitgoWallet wallet;
        private String error, result, status;

        public AppObject(IBitgoWallet wallet, SendRequest req, IBitgoWallet.StatusCB cb) {
            this.wallet = wallet;
            this.req = req;
            this.cb = cb;
        }

        @JavascriptInterface
        public boolean getIsTest() {
            return Global.isTest();
        }

        @JavascriptInterface
        public String getCoin() {
            return req.tokenInfo.getTokenCode();
        }

        @JavascriptInterface
        public String getAmount() {
            return req.amount;
        }

        @JavascriptInterface
        public String getDest() {
            return req.recipientAddress;
        }

        @JavascriptInterface
        public String getOtp() {
            return req.otp;
        }

        @JavascriptInterface
        public String getComment() {
            return req.comment;
        }

        @JavascriptInterface
        public String getWalletPassphrase() {
            return req.walletPassphrase;
        }

        @JavascriptInterface
        public String getAccessToken() {
            return Global.getAccessToken();
        }

        @JavascriptInterface
        public String getWalletId() {
            return wallet.getId();
        }

        @JavascriptInterface
        public void setStatus(String type, String status) {
            this.status = status;
            if (cb != null) cb.onStatus(type, status);
            else
                Log.d(TAG, "setStatus " + type + ": " + status);

            switch (type) {
                case "state":
                    return;  //not final - don't notify..
                case "error":
                    this.error = status;
                    break;
                case "result":
                    this.result = status;
                    break;

            }
            synchronized (this) {
                notifyAll();
            }
        }

    }


}
