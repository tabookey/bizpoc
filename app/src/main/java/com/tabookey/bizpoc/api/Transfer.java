package com.tabookey.bizpoc.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tabookey.bizpoc.Approval;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.Nullable;

/**
 * represent past (completed) transfer operation
 */
public class Transfer {
    public String id, txid, valueString, coin, usd, remoteAddress, comment;
    public Date date;

    @JsonIgnore
    public TokenInfo token;

    @JsonIgnore
    public Approval.State state;

    public Transfer(String id, String txid, String valueString, String coin, String usd, Date date, String remoteAddress, String comment, TokenInfo token, Approval.State state) {
        this.id = id;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if ( !(obj instanceof Transfer) )
            return false;
        return Objects.equals(((Transfer) obj).id, this.id);
    }


    //TODO: add other transfer items (e.g. approve time, approvers)?
}
