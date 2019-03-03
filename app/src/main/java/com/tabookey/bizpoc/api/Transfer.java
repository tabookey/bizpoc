package com.tabookey.bizpoc.api;

/**
 * represent past (completed) transfer operation
 */
public class Transfer {
    public String txid, valueString, coin, usd, date, remoteAddress, comment;

    public Transfer(String txid, String valueString, String coin, String usd, String date, String remoteAddress, String comment) {
        this.txid = txid;
        this.valueString = valueString;
        this.coin = coin;
        this.usd = usd;
        this.date = date;
        this.remoteAddress = remoteAddress;
        this.comment = comment;
    }
    //TODO: add other transfer items (e.g. approve time, approvers)?
}
