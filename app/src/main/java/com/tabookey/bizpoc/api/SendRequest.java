package com.tabookey.bizpoc.api;

public class SendRequest {
    public String recipientAddress;
    public String comment;
    public String amount;
    public String otp;
    public String walletPassphrase; //while the passphrase is "static" per wallet, we don't want to keep it in memory,
        //so the application should extract it into memory only when actually sending coins.

    public SendRequest(String recipientAddress, String comment, String amount, String otp, String walletPassphrase) {
        this.recipientAddress = recipientAddress;
        this.comment = comment;
        this.amount = amount;
        this.otp = otp;
		this.walletPassphrase = walletPassphrase;
    }
}
