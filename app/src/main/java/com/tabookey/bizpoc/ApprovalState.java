package com.tabookey.bizpoc;

public enum ApprovalState {
    WAITING,
    // So far, incoming transfers are also "approved"
    APPROVED,
    DECLINED,
    CANCELLED
}
