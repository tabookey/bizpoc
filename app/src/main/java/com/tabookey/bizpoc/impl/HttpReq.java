package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import okhttp3.CertificatePinner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.tabookey.bizpoc.impl.Utils.fromJson;
import static com.tabookey.bizpoc.impl.Utils.toJson;

public class HttpReq {

    boolean debug=true;

    static String testUrl = "https://test.bitgo.com";
//    static String testUrl = "https://relay1.duckdns.org";
    static String prodUrl = "https://www.bitgo.com";

    private final String accessKey;
    String host;
    public HttpReq( String accessKey, boolean test) {
        this.accessKey = accessKey;
        this.host = test ? testUrl : prodUrl;
    }

    public String getHost() {
        return host;
    }

    //from: https://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html
    CertificatePinner certPinner = new CertificatePinner.Builder()
            .add("*.bitgo.com", "sha256/rlM24Z6HZfQm71GKuqEO25xb7XP3AXTAU5kvJdkR9TI=")
            .build();

    private OkHttpClient client = new OkHttpClient.Builder()
            .certificatePinner(certPinner).build();

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
        Request.Builder bld = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessKey);

        bld.url(host+api);
        if ( data!=null ) {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), toJson(data));
            bld.method(method==null ? "PUT":method, body);
        }

        try {
            Request request = bld.build();
            if ( debug )
                System.err.println( "> "+request.method()+" "+ request.url());
            Response res = client.newCall(request).execute();
            String str = res.body().string();
            if ( debug )
                System.err.println( "< "+str);
            if ( str.contains("\"error\"") ) {
                JsonNode errdecs = fromJson(str, JsonNode.class);
                throw new BitgoError(errdecs.get("name").asText(), errdecs.get("error").asText());
            }
            return str;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
