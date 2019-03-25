package com.tabookey.bizpoc.api;

public class TokenInfo {
    public String type; //the token TLA
    public String coin; //always "eth" (or teth
    public String network; //mainnet or Kovan
    public String tokenContractAddress;
    public int decimalPlaces;
    public String name;

    //items copied from bitgo-client. not sure how do they differ from name and type, above.
    public String logo, fullDisplay, shortDisplay;

    public TokenInfo() {
    }

    /**
     * @return {@link TokenInfo::coin} for native token, {@link TokenInfo::type} for tokens
     */
    public String getTokenCode() {
        if (type.length() == 0) {
            return coin;
        }
        return type
    }

    public TokenInfo(String type, String coin, String network, String tokenContractAddress, int decimalPlaces, String name, String logo, String fullDisplay, String shortDisplay) {
        this.type = type;
        this.coin = coin;
        this.network = network;
        this.tokenContractAddress = tokenContractAddress;
        this.decimalPlaces = decimalPlaces;
        this.name = name;
        this.logo = logo;
        this.fullDisplay = fullDisplay;
        this.shortDisplay = shortDisplay;
    }
}
