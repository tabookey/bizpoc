package com.tabookey.bizpoc.impl;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;

import java.io.IOException;

public class CoinSender extends WebViewExecutor {

    public CoinSender(Context ctx, HttpReq http) {
        super(ctx, http);
    }

    public String sendCoins(IBitgoWallet wallet, String dest, String amount, String otp, String passphrase, IBitgoWallet.StatusCB cb) {
        AppObject appObject = new AppObject(wallet, dest, amount, otp, passphrase, cb);
        exec("www/sender.html", appObject);

        //max time to wait for final result
        synchronized (appObject) {
            try {
                appObject.wait(30 * 1000);
            } catch (InterruptedException e) {
            }
        }

        if (appObject.result == null) {
            //report timeout and last known status
            throw new RuntimeException("timed-out: " + appObject.status);
        }

        if ( appObject.result.toLowerCase().contains("error"))
            throw new RuntimeException(appObject.result);

        return appObject.result;
    }

    public static class AppObject {
        private final IBitgoWallet.StatusCB cb;
        IBitgoWallet wallet;
        final String amount, dest, otp, walletPassphrase;
        private String error, result, status;

        AppObject(IBitgoWallet wallet, String dest, String amount, String otp, String walletPassphrase, IBitgoWallet.StatusCB cb) {
            this.wallet = wallet;
            this.amount = amount;
            this.dest = dest;
            this.otp = otp;
            this.walletPassphrase = walletPassphrase;
            this.cb = cb;
        }

        @JavascriptInterface
        public boolean getIsTest() {
            return Global.isTest();
        }

        @JavascriptInterface
        public String getCoin() {
            return wallet.getCoin();
        }

        @JavascriptInterface
        public String getAmount() {
            return amount;
        }

        @JavascriptInterface
        public String getDest() {
            return dest;
        }

        @JavascriptInterface
        public String getOtp() {
            return otp;
        }

        @JavascriptInterface
        public String getWalletPassphrase() {
            return walletPassphrase;
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
                Log.d(TAG, "setStatus "+type+": " + status);

            if ( !type.equals("state") ) {
                result = status;
                synchronized (this) {
                    notifyAll();
                }
            }
        }

    }


}
