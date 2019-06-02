package com.tabookey.bizpoc.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tabookey.bizpoc.Approval;
import com.tabookey.bizpoc.ApprovalState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import androidx.annotation.Nullable;

/**
 * represent past (completed) transfer operation
 */
public class Transfer  implements Serializable {
    public String id, txid, valueString, coin, usd, remoteAddress, comment, pendingApproval;
    public Date date;

    @JsonIgnore
    public TokenInfo token;

    @JsonIgnore
    public ApprovalState state;

    @JsonIgnore
    public String cancelledBy;

    @JsonIgnore
    public ArrayList<String> approvals;

    public Transfer(String id, String txid, String valueString, String coin, String usd, Date date, String remoteAddress, String comment, String pendingApproval, TokenInfo token, ApprovalState state, String cancelledBy, ArrayList<String> approvals) {
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
        this.cancelledBy = cancelledBy;
        this.approvals = approvals;
        this.pendingApproval = pendingApproval;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Transfer))
            return false;
        return Objects.equals(((Transfer) obj).id, this.id);
    }


    //TODO: add other transfer items (e.g. approve time, approvers)?
}
