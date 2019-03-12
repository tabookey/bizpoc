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

    public String sendCoins(IBitgoWallet wallet, String dest, String amount, String otp, String passphrase) {
        AppObject appObject = new AppObject(wallet,dest,amount,otp,passphrase);
        exec("www/sender.html", appObject);
        synchronized (appObject) {
            try {
                appObject.wait(60);
            } catch (InterruptedException e) {
            }
        }
        if ( appObject.result!=null )
            return appObject.result;
        if ( appObject.error!=null )
            throw new RuntimeException(appObject.error);

        throw new RuntimeException("timed-out: "+appObject.status);
    }

    public static class AppObject {
        IBitgoWallet wallet;
        final String amount, dest, otp, walletPassphrase;
        private String error, result, status;

        AppObject(IBitgoWallet wallet, String dest, String amount, String otp, String walletPassphrase) {
            this.wallet = wallet;
            this.amount = amount;
            this.dest = dest;
            this.otp = otp;
            this.walletPassphrase = walletPassphrase;
        }

        @JavascriptInterface
        public boolean getIsTest() { return Global.isTest(); }

        @JavascriptInterface
        public String getCoin() { return wallet.getCoin(); }

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
        public String getWalletPassphrase() { return walletPassphrase; }

        @JavascriptInterface
        public String getAccessKey() {
            return Global.getAccessToken();
        }

        @JavascriptInterface
        public String getWalletId() { return wallet.getId(); }

        public synchronized void setError(String s) {
            this.error = s;
            Log.e(TAG, "setError: "+ s );
            notifyAll();
        }

        public void setStatus(String status) {
            this.status = status;
            Log.d(TAG, "setStatus: "+status);
        }

        public synchronized void setResult(String res) {
            Log.d(TAG, "setResult: "+res);
            this.result = res;
            notifyAll();
        }

    }


}
