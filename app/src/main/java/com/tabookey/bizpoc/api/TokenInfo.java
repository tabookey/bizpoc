package com.tabookey.bizpoc.api;

public class TokenInfo {
    public String type; //the token TLA
    public String coin; //always "eth"
    public String network; //mainnet or kovan
    public String tokenContractAddress;
    public int decimalPlaces;
    public String name;

    //items copied from bitgo-client. not sure how do they differ from name and type, above.
    public String logo, fullDisplay, shortDisplay;
}
