package com.tabookey.bizpoc.api;

public class SendRequest {
    public String coin;
    public String amount;
    public String recipientAddress;
    public String otp;
    public String walletPassphrase; //while the passphrase is "static" per wallet, we don't want to keep it in memory,
    public String comment;
    public String type;
    //so the application should extract it into memory only when actually sending coins.

    public SendRequest(String coin, String type, String amount, String recipientAddress, String otp, String walletPassphrase, String comment) {
        this.coin = coin;
        this.type = type;
        this.amount = amount;
        this.recipientAddress = recipientAddress;
        this.otp = otp;
        this.walletPassphrase = walletPassphrase;
        this.comment = comment;
    }
}
