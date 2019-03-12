package com.tabookey.bizpoc.impl;

import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * run javascript code inside a webview.
 */
public class WebViewExecutor {
    static String TAG = "webv";

    private final WebView webview;
    private final HttpReq http;

    public WebViewExecutor(Context ctx, HttpReq http) {
        this.webview = new WebView(ctx);
        this.http = http;
    }

    /**
     * execute javascript code inside a webview.
     * Note that the code is asynchronous. all input parameters are read from the app object,
     * and all output are also written into methods of it.
     *
     * @param path    URL to load into the webview. must be a resource under assets
     * @param appData data object. accessible in the javascript code as "app"
     *                (methods must be decorated with @JavascriptInterface)
     */
    public void exec(String path, Object appData) {

        webview.setWebViewClient(new MyWebviewClient(appData));
        webview.getSettings().setJavaScriptEnabled(true);

        webview.loadUrl(path);
    }

    final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US);

    class MyWebviewClient extends WebViewClient {
        private final Object appData;

        public MyWebviewClient(Object appData) {
            this.appData = appData;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

            if (request.getMethod().equals("OPTIONS"))
                return new WebResourceResponse(
                        null, "UTF-8", 200, "Ok", createHeaders(), null);

            try {
                if (!request.getUrl().toString().startsWith("http")) {
                    String file = request.getUrl().toString();

                    InputStream is = webview.getResources().getAssets().open(file);
                    String mime = file.indexOf(".js") > 0 ? "application/json" : "text/html";
                    return new WebResourceResponse(
                            mime, "UTF-8", 200, "Ok", createHeaders(), is);
                } else {
                    return callWithOkHttp(http.getClient(), request);
                }
            } catch (IOException e) {
                Log.w(TAG, "shouldInterceptRequest: " + request.getMethod() + " " + request.getUrl(), e);
                return null;
            }

        }

        private HashMap<String, String> createHeaders() {
            String dateString = formatter.format(new Date());

            return new HashMap<String, String>() {{
                put("Connection", "close");
                put("Date", dateString + " GMT");
                put("Access-Control-Allow-Origin", "*"/* your domain here */);
                put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
                put("Access-Control-Max-Age", "600");
                put("Access-Control-Allow-Credentials", "true");
                put("Access-Control-Allow-Headers", "*");
            }};
        }

        private synchronized WebResourceResponse callWithOkHttp(OkHttpClient client, WebResourceRequest request) throws IOException {
            Request.Builder reqbuilder = new Request.Builder()
                    .url(request.getUrl().toString());
//            if ( request.getUrl().toString().contains("wallet")) {
//
//                reqbuilder.url("https://test.bitgo.com/api/v1/user/session" );
//                Log.d(TAG, "callWithOkHttp: forceurl");
//            }

            Log.d(TAG, ">> " + request.getMethod() + " " + request.getUrl());
            request.getRequestHeaders().forEach((key, value) -> {
                if ("Authorization, HMAC, Auth-Timestamp, BitGo-Auth-Version".toLowerCase().contains(key.toLowerCase()) ) {
                    reqbuilder.addHeader(key, value);
                    Log.d(TAG, ">> " + key + ": " + value);
                } else {
//                    Log.d(TAG, "SKIPPED >> " + key + ": " + value);
                }
            });
            if (!request.getMethod().equals("GET")) {

                MediaType mediatype = MediaType.parse("application/json");
                reqbuilder.method(request.getMethod(), RequestBody.create(mediatype, request.getRequestHeaders().get("x-body")));
            }
            Response resp = client.newCall(reqbuilder.build()).execute();

            Log.d(TAG, "<< " + resp.code() + " " + resp.message());
            HashMap<String, String> headers = new HashMap<>();
            resp.headers().toMultimap().forEach((key, val) -> {
                if (val.size() != 1)
                    Log.w(TAG, "ignore multi-value response header: " + key + ": " + val);
                else
                    Log.d(TAG, "<< " + key + ": " + val);
                headers.put(key, val.get(0));
            });
            Log.d(TAG, "resp=" + resp.peekBody(100000).string());
            return new WebResourceResponse("application/json", "UTF-8", 200, "Ok", headers, resp.body().byteStream());
        }

        /**/

    }

}

