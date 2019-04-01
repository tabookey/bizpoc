package com.tabookey.bizpoc.impl;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

import static com.tabookey.bizpoc.impl.BitgoHmac.calculateHMACSubject;
import static com.tabookey.bizpoc.impl.BitgoHmac.calculateRequestHMAC;
import static com.tabookey.bizpoc.impl.BitgoHmac.calculateRequestHeaders;
import static com.tabookey.bizpoc.impl.BitgoHmac.fromHex;
import static com.tabookey.bizpoc.impl.BitgoHmac.toHex;
import static com.tabookey.bizpoc.impl.BitgoHmac.verifyResponse;
import static org.junit.Assert.*;

public class BitgoHmacTest {

    @Test
    public void test_req_body() throws IOException {
        Request.Builder bld = new Request.Builder();
        String content = "{\"this\":false}";
        bld.url("http://url").method("PUT", RequestBody.create( MediaType.parse("application/json"), content));


        Buffer s = new Buffer();
        bld.build().body().writeTo(s);
        assertEquals(content, s.readUtf8());
    }
    static class GetRequest {
        static String requrl="https://test.bitgo.com/api/v2/teth/wallet";
        static long reqTimestamp = 1553680781437l;
        static String req_hmac = "3e95c3f67c0b5462f439d904c870c066abdc4350228039182e24214e5889f1d1";
        static String reqToken = "v2xf4fe8849788c60cc06c83f799c59b9b9712e4ba394e63ba50458f6a0593f72e8";
        static String[] reqheaders= {"Authorization: Bearer d1b770b72a96a7b639958ea94934164e44a03ea686ae44a7161f08dba2708c30",
                "HMAC: 3e95c3f67c0b5462f439d904c870c066abdc4350228039182e24214e5889f1d1",
                "Auth-Timestamp: 1553680781437",
                "BitGo-Auth-Version: 2.0"
        };
    }

    static long respTimestamp=1553680782788l;
    static String respHmac = "0f9f084a6ab3e355d2b1d2029993febba2147d27bcd9f10febe6b34fb9810017";
    static String[] respHeaders= {
            "hmac: 0f9f084a6ab3e355d2b1d2029993febba2147d27bcd9f10febe6b34fb9810017",
            "timestamp: 1553680782788",
    };
    static String respBody="{\"coin\":\"teth\",\"wallets\":[{\"id\":\"5c742328f84f79e00390259a56fe350f\",\"users\":[{\"user\":\"5c7421def84f79e00390186170462bd7\",\"permissions\":[\"admin\",\"view\",\"spend\"]},{\"user\":\"5c6d69b45a5a2dd30399ebe324f370c6\",\"permissions\":[\"admin\",\"spend\",\"view\"]}],\"coin\":\"teth\",\"label\":\"MyWallet\",\"m\":2,\"n\":3,\"keys\":[\"5c74230a9fa6b9b6038fb605abc1d21d\",\"5c74230bc7ab23b103ad0578d675b531\",\"5c742251e60320b8037de709856aa31a\"],\"keySignatures\":{},\"enterprise\":\"5c742251e60320b8037de70b853f06a1\",\"tags\":[\"5c742328f84f79e00390259a56fe350f\",\"5c742251e60320b8037de70b853f06a1\"],\"disableTransactionNotifications\":false,\"freeze\":{},\"deleted\":false,\"approvalsRequired\":1,\"isCold\":false,\"coinSpecific\":{\"deployedInBlock\":false,\"deployTxHash\":\"0x236a8f379de3165449e283fdb71b9bd1b828158cbee9b94b79358810d6d050cf\",\"lastChainIndex\":{\"0\":0,\"1\":-1},\"baseAddress\":\"0xf41cc36b2747d59672d0e96b3e0f99094d7c19a5\",\"feeAddress\":\"0x9b8a477b5b071e3a0da3e07ffc827d278570f81e\",\"pendingChainInitialization\":false,\"creationFailure\":[],\"tokenFlushThresholds\":{},\"lowPriorityFeeAddress\":\"0xc67c8a6fea2b7d8e647f978f2d93ce48bd1ebfd5\"},\"admin\":{\"policy\":{\"_id\":\"5c74292c9fa6b9b6038ff303\",\"label\":\"default\",\"__v\":0,\"rules\":[{\"ruleId\":\"Euu3tct5D3M39czJXzjJUqBFAtx\",\"lockDate\":\"2019-02-27T17:43:08.406Z\",\"type\":\"allTx\",\"action\":{\"type\":\"getApproval\",\"userIds\":[]},\"limit\":{\"excludeTags\":[],\"groupTags\":[]},\"list\":{\"items\":[]}}],\"date\":\"2019-02-25T17:43:08.408Z\",\"latest\":true,\"version\":1,\"uniqueId\":\"5c742329f84f79e0039025a1\"}},\"clientFlags\":[],\"allowBackupKeySigning\":false,\"recoverable\":true,\"balanceString\":\"90000000000000000\",\"confirmedBalanceString\":\"90000000000000000\",\"spendableBalanceString\":\"90000000000000000\",\"startDate\":\"2019-02-25T17:17:28.000Z\"},{\"id\":\"5c76a32beafc63a50483d14a9cfc2585\",\"users\":[{\"user\":\"5c6e92ff9fa9d31504b13a6b9bf28837\",\"permissions\":[\"admin\",\"view\",\"spend\"]},{\"user\":\"5c7421def84f79e00390186170462bd7\",\"permissions\":[\"admin\",\"spend\",\"view\"]}],\"coin\":\"teth\",\"label\":\"TanyaRules\",\"m\":2,\"n\":3,\"keys\":[\"5c76a30b6d86abf7032c979bc376d281\",\"5c76a30ceafc63a50483d01c2768221b\",\"5c76a0fb5d0704a3038bca6515e877eb\"],\"keySignatures\":{},\"enterprise\":\"5c76a0fb5d0704a3038bca67b673e90f\",\"tags\":[\"5c76a32beafc63a50483d14a9cfc2585\",\"5c76a0fb5d0704a3038bca67b673e90f\"],\"disableTransactionNotifications\":false,\"freeze\":{},\"deleted\":false,\"approvalsRequired\":1,\"isCold\":false,\"coinSpecific\":{\"deployedInBlock\":false,\"deployTxHash\":\"0xe540cab50aecd7be180809f9df77b7ca9ea3b07dadcada41fe643a00f6c8baca\",\"lastChainIndex\":{\"0\":0,\"1\":-1},\"baseAddress\":\"0x04428f946911916de13f27de85107632d3807af2\",\"feeAddress\":\"0xb912dcefd5e8374a0edb45e17af1a9d3e4883162\",\"pendingChainInitialization\":false,\"creationFailure\":[],\"tokenFlushThresholds\":{},\"lowPriorityFeeAddress\":\"0xb6a2cd61c6fefcb8dedce98c9f7c2613542f4c01\"},\"admin\":{},\"clientFlags\":[],\"allowBackupKeySigning\":false,\"recoverable\":true,\"balanceString\":\"490000000000000000\",\"confirmedBalanceString\":\"490000000000000000\",\"spendableBalanceString\":\"490000000000000000\",\"startDate\":\"2019-02-27T14:48:11.000Z\"}]}";

    //calculated by modified bitgo.min.js..
    private String req_subject = "1553680781437|/api/v2/teth/wallet|";

    static HashMap<String, String> headersToMap(String[] s) {
        HashMap<String, String> ret = new HashMap<>();
        for (String line : s) {
            String[]nameval = line.split(": ");
            ret.put(nameval[0], nameval[1]);
        }
        return ret;
    }
    /**
     * captured request:
     * >> GET https://test.bitgo.com/api/v2/teth/key/5c921f4f5ca161b003c9988917a42774
     * >> Authorization: Bearer 72dd8a021f4d82f6a9cf9b7affd10a7ce2957d27f946fa0216dbbc51e929e393
     * >> HMAC: 4d693f6c7ff428d2630980b725333e18c868d1ba6295337cbe13cd255fe7d55f
     * >> Auth-Timestamp: 1553640352463
     * >> BitGo-Auth-Version: 2.0
     */

    @Test
    public void test_calculateRequestHeaders() {

        HashMap<String, String> ret = calculateRequestHeaders(GetRequest.requrl,"", GetRequest.reqTimestamp, GetRequest.reqToken);
        assertEquals( headersToMap(GetRequest.reqheaders).toString().replace(",", ",\n"), ret.toString().replace(",", ",\n"));
    }

    @Test
    public void test_verifyResponse() {
        verifyResponse(GetRequest.requrl, 200, respBody, respTimestamp, GetRequest.reqToken, respHmac);
    }

    @Test(expected = RuntimeException.class)
    public void test_verifyResponse_old() {
        verifyResponse(GetRequest.requrl, 200, respBody, respTimestamp-1000, GetRequest.reqToken, respHmac);
    }

    @Test(expected = RuntimeException.class)
    public void test_verifyResponse_wrong_hmac() {
        verifyResponse(GetRequest.requrl, 200, respBody, respTimestamp-1000, GetRequest.reqToken, respHmac+"1");
    }

    @Test
    public void test_subject() {

        assertEquals(req_subject, calculateHMACSubject(GetRequest.requrl, "", GetRequest.reqTimestamp, 0) );
    }
    @Test
    public void test_calculateRequestHMAC() {
        String hmac = calculateRequestHMAC(
                GetRequest.requrl,"",GetRequest.reqTimestamp,GetRequest.reqToken
        );
        assertEquals(GetRequest.req_hmac, hmac);
    }


    @Test
    public void test_fromHex() {
        assertEquals(toHex(new byte[]{1, 2, (byte) 0x80, (byte) 0x9a}), toHex(fromHex("0102809a")));
    }

    @Test
    public void test_toHex() {
        assertEquals("0102809a", toHex(new byte[]{1, 2, (byte) 0x80, (byte) 0x9a}));
    }

}