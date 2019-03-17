package com.tabookey.bizpoc.impl;

import org.junit.Assert;
import org.junit.Test;

import static com.tabookey.bizpoc.impl.Utils.fromJson;

public class BitgoEnterpriseTest {


    @Test
    public void testMergedData() {
        MergedWalletsData data = fromJson(getWalletsMerged, MergedWalletsData.class);
        Assert.assertNotNull(data.wallets);
    }

    //return value of /api/v2/wallets/merged?coin
    String getWalletsMerged = "{\n" +
            "  \"wallets\": [\n" +
            "    {\n" +
            "      \"id\": \"5c6d85e6d6f55fe103855738d2cb2419\",\n" +
            "      \"version\": 2,\n" +
            "      \"label\": \"MyWallet\",\n" +
            "      \"users\": [\n" +
            "        {\n" +
            "          \"user\": \"5c6d69b45a5a2dd30399ebe324f370c6\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"view\",\n" +
            "            \"spend\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"user\": \"5c6d6f2ba8235cda035568a40dfd9c49\",\n" +
            "          \"permissions\": [\n" +
            "            \"spend\",\n" +
            "            \"view\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"user\": \"5c6e92ff9fa9d31504b13a6b9bf28837\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"spend\",\n" +
            "            \"view\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"user\": \"5c74febe8333daf703f2b7eebfcff4f7\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"spend\",\n" +
            "            \"view\"\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"coin\": \"teth\",\n" +
            "      \"tokens\": {\n" +
            "        \"terc\": {\n" +
            "          \"balanceString\": \"39\",\n" +
            "          \"confirmedBalanceString\": \"39\",\n" +
            "          \"spendableBalanceString\": \"39\",\n" +
            "          \"transferCount\": 3\n" +
            "        },\n" +
            "        \"tbst\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"schz\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tcat\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tfmf\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        }\n" +
            "      },\n" +
            "      \"enterprise\": \"5c6d855ba7764fc10348d15d42c96b22\",\n" +
            "      \"balanceString\": \"3299747224000000000\",\n" +
            "      \"spendableBalanceString\": \"3299747224000000000\",\n" +
            "      \"isCold\": false,\n" +
            "      \"startDate\": \"2019-02-20T16:52:54.000Z\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"5c750e3eedc37fad03b135d4fa288a13\",\n" +
            "      \"version\": 2,\n" +
            "      \"label\": \"second wallet\",\n" +
            "      \"users\": [\n" +
            "        {\n" +
            "          \"user\": \"5c6d69b45a5a2dd30399ebe324f370c6\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"view\",\n" +
            "            \"spend\"\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"coin\": \"teth\",\n" +
            "      \"tokens\": {\n" +
            "        \"terc\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tbst\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"schz\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tcat\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tfmf\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        }\n" +
            "      },\n" +
            "      \"enterprise\": \"5c6d855ba7764fc10348d15d42c96b22\",\n" +
            "      \"balanceString\": \"0\",\n" +
            "      \"spendableBalanceString\": \"0\",\n" +
            "      \"isCold\": false,\n" +
            "      \"startDate\": \"2019-02-26T10:00:30.000Z\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"5c782759a203d7c003496d41b3521cbe\",\n" +
            "      \"version\": 2,\n" +
            "      \"label\": \"newtest\",\n" +
            "      \"users\": [\n" +
            "        {\n" +
            "          \"user\": \"5c6d69b45a5a2dd30399ebe324f370c6\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"view\",\n" +
            "            \"spend\"\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"coin\": \"teth\",\n" +
            "      \"tokens\": {\n" +
            "        \"terc\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tbst\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"schz\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tcat\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tfmf\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        }\n" +
            "      },\n" +
            "      \"enterprise\": \"5c6d855ba7764fc10348d15d42c96b22\",\n" +
            "      \"balanceString\": \"0\",\n" +
            "      \"spendableBalanceString\": \"0\",\n" +
            "      \"isCold\": false,\n" +
            "      \"startDate\": \"2019-02-28T18:24:25.000Z\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"5c869eecb7b033d603b430afbba85850\",\n" +
            "      \"version\": 2,\n" +
            "      \"label\": \"wallettest3\",\n" +
            "      \"users\": [\n" +
            "        {\n" +
            "          \"user\": \"5c6d69b45a5a2dd30399ebe324f370c6\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"view\",\n" +
            "            \"spend\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"user\": \"5c6d6f2ba8235cda035568a40dfd9c49\",\n" +
            "          \"permissions\": [\n" +
            "            \"admin\",\n" +
            "            \"spend\",\n" +
            "            \"view\"\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"coin\": \"teth\",\n" +
            "      \"tokens\": {\n" +
            "        \"terc\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tbst\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"schz\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tcat\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        },\n" +
            "        \"tfmf\": {\n" +
            "          \"balanceString\": \"0\",\n" +
            "          \"confirmedBalanceString\": \"0\",\n" +
            "          \"spendableBalanceString\": \"0\",\n" +
            "          \"transferCount\": 0\n" +
            "        }\n" +
            "      },\n" +
            "      \"enterprise\": \"5c6d855ba7764fc10348d15d42c96b22\",\n" +
            "      \"balanceString\": \"666530765432201251\",\n" +
            "      \"spendableBalanceString\": \"666530765432201251\",\n" +
            "      \"isCold\": false,\n" +
            "      \"startDate\": \"2019-03-11T17:46:20.000Z\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

}