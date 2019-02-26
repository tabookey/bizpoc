package com.tabookey.bizpoc.impl;

import java.io.IOException;

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
    static String prodUrl = "https://www.bitgo.com";

    private final String accessKey;
    String host;
    public HttpReq( String accessKey, boolean test) {
        this.accessKey = accessKey;
        this.host = test ? testUrl : prodUrl;
//        this.host = "http://localhost:12345";
    }

    OkHttpClient client = new OkHttpClient();

    public String get(String api) {
        return sendRequest(api, null,null);
    }

    public <T> T get(String api, Class<T> cls) {
        return fromJson(sendRequest(api,null,null), cls);
    }

    public <T> T put(String api, Object data, Class<T> cls) {
        return fromJson(sendRequest(api,data,"PUT"), cls);
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
            if ( str.contains("\"error\"") )
                throw new RuntimeException(str);
            return str;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
