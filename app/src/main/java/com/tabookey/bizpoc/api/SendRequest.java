package com.tabookey.bizpoc.api;

public class SendRequest {
    public TokenInfo tokenInfo;
    public String amount;
    public String recipientAddress;
    public String otp;
    public String walletPassphrase; //while the passphrase is "static" per wallet, we don't want to keep it in memory,
    public String comment;
    //so the application should extract it into memory only when actually sending coins.

    public SendRequest(TokenInfo tokenInfo, String amount, String recipientAddress, String otp, String walletPassphrase, String comment) {
        this.tokenInfo = tokenInfo;
        this.amount = amount;
        this.recipientAddress = recipientAddress;
        this.otp = otp;
        this.walletPassphrase = walletPassphrase;
        this.comment = comment;
    }
}
