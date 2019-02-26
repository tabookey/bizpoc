package com.tabookey.bizpoc.api;

/**
 * represent past (completed) transfer operation
 */
public class Transfer {
    public String txid, valueString, coin, usd, createdDate, confirmedDate, remoteAddress;
    //TODO: add other transfer items (e.g. approve time, approvers)?
}
