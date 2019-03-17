package com.tabookey.bizpoc.impl;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.SendRequest;

import static com.tabookey.bizpoc.impl.Utils.fromJson;

public class CoinSender extends WebViewExecutor {

    public CoinSender(Context ctx, HttpReq http) {
        super(ctx, http);
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

        if (appObject.error!=null )
            throw new RuntimeException(appObject.error);

        if (appObject.result == null) {
            //report timeout and last known status
            throw new RuntimeException("timed-out: " + appObject.status);
        }

        return fromJson(appObject.result, JsonNode.class).get("pendingApproval").get("id").asText();

    }

    public static class AppObject {
        private final IBitgoWallet.StatusCB cb;
        private final SendRequest req;
        IBitgoWallet wallet;
        private String error, result, status;

        AppObject(IBitgoWallet wallet, SendRequest req, IBitgoWallet.StatusCB cb) {
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
            return req.coin;
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
                case "state" : return;  //not final - don't notify..
                case "error" : this.error = status; break;
                case "result" : this.result = status; break;

            }
            synchronized (this) {
                notifyAll();
            }
        }

    }


}
