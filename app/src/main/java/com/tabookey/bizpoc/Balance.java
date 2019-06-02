package com.tabookey.bizpoc;

import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.impl.Utils;

public class Balance {
    String coinName;
    String coinBalance;
    double exchangeRate;
    TokenInfo tokenInfo;

    Balance(String coinName, String coinBalance, double exchangeRate, TokenInfo tokenInfo) {
        this.coinName = coinName;
        this.coinBalance = coinBalance;
        this.exchangeRate = exchangeRate;
        this.tokenInfo = tokenInfo;
    }

    double getValue() {
        return Utils.integerStringToCoinDouble(coinBalance, tokenInfo.decimalPlaces);
    }

    double getDollarValue() {
        return exchangeRate * getValue();
    }
}
