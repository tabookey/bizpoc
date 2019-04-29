package com.tabookey.bizpoc.impl;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.BuildConfig;

import okhttp3.CertificatePinner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.tabookey.bizpoc.impl.Utils.fromJson;
import static com.tabookey.bizpoc.impl.Utils.toJson;

public class HttpReq {
    public static final String TAG = "http";

    static boolean debug= BuildConfig.DEBUG;

    //note that we have one proxy, but it can determine actual server based on "x-origin" header.

    static String testUrl = "https://bizpoc.ddns.tabookey.com:8090";
    static String prodUrl = "https://bizpoc.ddns.tabookey.com";

    private final String accessKey;
    String host;
    public HttpReq( String accessKey, boolean test) {
        this.accessKey = accessKey;
        this.host = test ? testUrl : prodUrl;
    }

    public String getHost() {
        return host;
    }

    //unfortunately, production bitgo uses a multi-domain certificate, which can't be pinned
    // (changes too often), so there's no reason to pin the test certificate either..
    // probably its an LB certificate of their service provider.
    //from: https://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html
    CertificatePinner certPinner = new CertificatePinner.Builder()
            .add("www.google.com", "sha256/aaa=")
            .build();

    private OkHttpClient client = new OkHttpClient.Builder()
            .build();

    public OkHttpClient getClient() { return client; }

    public String get(String api, Object... params) {
        if ( params.length>0 )
            api = String.format(api, params);
        return sendRequest(api, null,null);
    }

    public <T> T get(String api, Class<T> cls, Object... params) {
        if ( params.length>0 )
            api = String.format(api, params);
        return fromJson(sendRequest(api,null,null), cls);
    }

    public <T> T put(String api, Object data, Class<T> cls) {
        return fromJson(sendRequest(api,data,"PUT"), cls);
    }

    public <T> T post(String api, Object data, Class<T> cls) {
        return fromJson(sendRequest(api,data,"POST"), cls);
    }

    public String sendRequest(String api, Object data, String method ) {
        Request.Builder bld = new Request.Builder();

        bld.url(host+api);

        if ( data!=null ) {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), toJson(data));
            bld.method(method==null ? "PUT":method, body);
        }

        BitgoHmac.addRequestHeaders(bld, accessKey);
        try {
            Request request = bld.build();
            if ( debug ) {
                Log.d(TAG, ">" + request.method() + " " + request.url());
                for ( String s : request.headers().names() ) {
                    Log.d(TAG, "> "+s+": "+request.header(s));
                }
            }
            Response res = client.newCall(request).execute();
            BitgoHmac.verifyResponse(res, accessKey);
            String str = res.body().string();
            if ( debug )
                Log.d(TAG, "< "+str);
            if ( str.contains("\"error\"") ) {
                JsonNode errdecs = fromJson(str, JsonNode.class);
                throw new BitgoError(errdecs.get("name").asText(), errdecs.get("error").asText());
            }
            return str;
        } catch (Exception e) {
            Log.e(TAG, "sendRequest: ex", e);
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    private static int fakeCount = 0;
    public static String sendRequestNotBitgo(String api, Object data, String method ) {
        if (api.equals("checkYubikeyExists")){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (++fakeCount < 0) {
                return "{\"result\":\"sdfg\"}";
            }
            return "{\"result\":\"ok\"}";


        }
        if (api.equals("getEncodedCredentials")){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "{\n" +
                    "  \"enc\": {\n" +
                    "    \"iv\": \"pz0laXGaH510/8FQWYMVEg==\",\n" +
                    "    \"v\": 1,\n" +
                    "    \"iter\": 1000,\n" +
                    "    \"ks\": 128,\n" +
                    "    \"ts\": 64,\n" +
                    "    \"mode\": \"ccm\",\n" +
                    "    \"adata\": \"\",\n" +
                    "    \"cipher\": \"aes\",\n" +
                    "    \"salt\": \"MEO2U83OdB4=\",\n" +
                    "    \"ct\": \"4QG8N6LT5U1lXAOQWcx1LeBgUos/mqJw3RfvU3PC8+IP+zGywgDC8ReHb9t313JIzfrRS3bMiEl82T0h85ynBZETv4ALaNRGskRyfbOOYaYT0LMx8aXTA4QOwNQ0W1unpimqkB+B18SQQ0mV6RFk8bN6aDPLVH3pAdprNXk=\"\n" +
                    "  },\n" +
                    "  \"scryptOptions\": {\n" +
                    "    \"salt\": \"salt\",\n" +
                    "    \"N\": 8192,\n" +
                    "    \"r\": 64,\n" +
                    "    \"p\": 4,\n" +
                    "    \"dkLen\": 32\n" +
                    "  }\n" +
                    "}";
            // Decrypts to: {"prod":false,"token":"v2xtoken","password":"passphrase"}
        }
        Request.Builder bld = new Request.Builder();

        bld.url(api);
        if ( data!=null ) {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), toJson(data));
            bld.method(method==null ? "PUT":method, body);
        }

        try {
            Request request = bld.build();
            if ( debug ) {
                Log.d(TAG, ">" + request.method() + " " + request.url());
                for ( String s : request.headers().names() ) {
                    Log.d(TAG, "> "+s+": "+request.header(s));
                }
            }
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();
            Response res = client.newCall(request).execute();
            String str = res.body().string();
            if ( debug )
                Log.d(TAG, "< "+str);
            if ( str.contains("\"error\"") ) {
                JsonNode errdecs = fromJson(str, JsonNode.class);
                throw new BitgoError(errdecs.get("name").asText(), errdecs.get("error").asText());
            }
            return str;
        } catch (Exception e) {
            Log.e(TAG, "sendRequest: ex", e);
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

}
