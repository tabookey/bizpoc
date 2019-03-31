package com.tabookey.bizpoc.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tabookey.bizpoc.Approval;

import java.util.Date;

/**
 * represent past (completed) transfer operation
 */
public class Transfer {
    public String txid, valueString, coin, usd, remoteAddress, comment;
    public Date date;

    @JsonIgnore
    public TokenInfo token;

    @JsonIgnore
    public Approval.State state;

    public Transfer(String txid, String valueString, String coin, String usd, Date date, String remoteAddress, String comment, TokenInfo token, Approval.State state) {
        this.txid = txid;
        this.state = state;
        this.valueString = valueString;
        this.coin = coin;
        this.usd = usd;
        this.date = date;
        this.remoteAddress = remoteAddress;
        this.comment = comment;
        this.token = token;
    }
    //TODO: add other transfer items (e.g. approve time, approvers)?
}
