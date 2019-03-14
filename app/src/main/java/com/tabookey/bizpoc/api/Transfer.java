package com.tabookey.bizpoc.api;

import java.util.Date;

/**
 * represent past (completed) transfer operation
 */
public class Transfer {
    public String txid, valueString, coin, usd, remoteAddress, comment;
    public Date date;

    public Transfer(String txid, String valueString, String coin, String usd, Date date, String remoteAddress, String comment) {
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
