package com.tabookey.bizpoc.impl;

import com.tabookey.logs.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.tabookey.bizpoc.BuildConfig;

import java.util.Map;

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

    static String PROXY_HOST = debug ? "bizpoc2.ddns.tabookey.com" : "bizpoc.ddns.tabookey.com";
    
    public static final String TEST_PROXY_URL = "https://"+PROXY_HOST+":8090";
    public static final String PROD_PROXY_URL = "https://"+PROXY_HOST;

    public static final String TEST_PROVISION_URL = "https://dprov-bizpoc.ddns.tabookey.com";
    public static final String PROD_PROVISION_URL = "https://prov-bizpoc.ddns.tabookey.com";
//    static String TEST_PROVISION_URL = "https://test.bitgo.com";

    private final String accessKey;
    private String host;

    //used by sendRequestNotBitgo
    private static OkHttpClient sclient;

    HttpReq(String accessKey, boolean test) {
        this.accessKey = accessKey;
        this.host = test ? TEST_PROXY_URL : PROD_PROXY_URL;
    }

    String getHost() {
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

    OkHttpClient getClient() { return client; }

    public <T> T get(String api, Class<T> cls, Object... params) {
        return get(api, cls, null, params);
    }

    public <T> T get(String api, Class<T> cls, Map<String, String> headers, Object... params) {
        if ( params.length>0 )
            api = String.format(api, params);
        return fromJson(sendRequest(api,null,null, headers), cls);
    }

    public <T> T put(String api, Object data, Class<T> cls) {
        return fromJson(sendRequest(api,data,"PUT"), cls);
    }

    public <T> T post(String api, Object data, Class<T> cls) {
        return fromJson(sendRequest(api,data,"POST"), cls);
    }

    private String sendRequest(String api, Object data, String method) {
        return sendRequest(api, data, method, null);
    }
    private String sendRequest(String api, Object data, String method, Map<String, String> headers) {
        Request.Builder bld = new Request.Builder();

        bld.url(host+api);

        if ( data!=null ) {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), toJson(data));
            bld.method(method==null ? "PUT":method, body);
        }

        if (headers != null) {
            for (String headerKey : headers.keySet()) {
                //noinspection ConstantConditions
                bld.addHeader(headerKey, headers.get(headerKey));
            }
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

    public static String sendRequestNotBitgo(String url, Object data, String method, Map<String, String> headers) {
        Request.Builder bld = new Request.Builder();

        bld.url(url);
        if ( data!=null ) {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), toJson(data));
            bld.method(method==null ? "PUT":method, body);
        }

        if (headers != null) {
            for (String headerKey : headers.keySet()) {
                //noinspection ConstantConditions
                bld.addHeader(headerKey, headers.get(headerKey));
            }
        }

        try {
            Request request = bld.build();
            if ( debug ) {
                Log.d(TAG, ">" + request.method() + " " + request.url());
                for ( String s : request.headers().names() ) {
                    Log.d(TAG, "> "+s+": "+request.header(s));
                }
            }
            if ( sclient==null )
                sclient = new OkHttpClient.Builder()
                    .build();
            Response res = sclient.newCall(request).execute();
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
