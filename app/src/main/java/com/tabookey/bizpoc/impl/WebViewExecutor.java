package com.tabookey.bizpoc.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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

import androidx.annotation.RequiresApi;
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

    private WebView webview;
    private final HttpReq http;

    void runOnUiThread(Runnable run) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            run.run();
        else
            new Handler(Looper.getMainLooper()).post(run);
    }

    public WebViewExecutor(Context ctx, HttpReq http) {
        this.http = http;
        runOnUiThread(() -> {
            webview = new WebView(ctx);
        });
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("JavascriptInterface")
    public void exec(String path, Object appData) {

        runOnUiThread(() -> {
            webview.setWebViewClient(new MyWebviewClient());
            String webViewPackageName = WebView.getCurrentWebViewPackage().packageName;
            Log.d(TAG, "webview pkg: "+ webViewPackageName);
            if ( !webViewPackageName.equals("com.android.chrome"))
                throw new RuntimeException("Invalid webview package: "+webViewPackageName);
            webview.addJavascriptInterface(appData, "app");
            webview.getSettings().setJavaScriptEnabled(true);
            webview.loadUrl("file:///android_asset/" + path);
        });
    }

    final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US);

    class MyWebviewClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

            if (request.getUrl().toString().contains("android"))
                return null;

            Log.d(TAG, "shouldInterceptRequest: " + request.getMethod() + " " + request.getUrl());
            if (request.getMethod().equals("OPTIONS"))
                return new WebResourceResponse(
                        null, "UTF-8", 200, "Ok", createHeaders(), null);
            try {
                return callWithOkHttp(http.getClient(), request);

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

            Log.d(TAG, ">> " + request.getMethod() + " " + request.getUrl());
            request.getRequestHeaders().forEach((key, value) -> {
                if ("Authorization, HMAC, Auth-Timestamp, BitGo-Auth-Version".toLowerCase().contains(key.toLowerCase())) {
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
            Log.d(TAG, "<< body: " + resp.peekBody(100000).string());
            return new WebResourceResponse("application/json", "UTF-8", resp.code(), resp.message(), headers, resp.body().byteStream());
        }

        /**/

    }

}

