package com.tabookey.bizpoc.api;

public class SendRequest {
    public String recipientAddress;
    public String comment;
    public String amount;
    public String coin; //eth or actual token.
    public String otp;

    public SendRequest(String recipientAddress, String comment, String amount, String coin, String otp) {
        this.recipientAddress = recipientAddress;
        this.comment = comment;
        this.amount = amount;
        this.coin = coin;
        this.otp = otp;
    }
}
