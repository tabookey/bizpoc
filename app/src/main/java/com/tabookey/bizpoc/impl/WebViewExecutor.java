package com.tabookey.bizpoc.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.tabookey.logs.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.utils.SafetyNetResponse;
import com.tabookey.bizpoc.utils.SafetynetHelperInterface;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
    private final Context ctx;

    private WebView webview;
    private final HttpReq http;

    SafetynetHelperInterface mSafetyNetHelper;

    void runOnUiThread(Runnable run) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            run.run();
        else
            new Handler(Looper.getMainLooper()).post(run);
    }

    public WebViewExecutor(Context ctx, HttpReq http, SafetynetHelperInterface safetyNetHelper) {
        this.http = http;
        this.ctx = ctx;
        this.mSafetyNetHelper = safetyNetHelper;
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

        runOnUiThread(() -> {
            initWebView(appData, true);
            webview.loadUrl("file:///android_asset/" + path);
        });
    }

    @SuppressLint("JavascriptInterface")
    void initWebView(Object appData, boolean intercept) {
        if (webview == null)
            webview = new WebView(ctx);

        if (intercept)
            webview.setWebViewClient(new MyWebviewClient());
        String webViewPackageName = WebView.getCurrentWebViewPackage().packageName;
        Log.d(TAG, "webview pkg: " + webViewPackageName);
        if (!webViewPackageName.equals("com.android.chrome"))
            throw new RuntimeException("Invalid webview package: " + webViewPackageName);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(appData, "app");
    }

    public void loadData(String html, Object appData) {
        runOnUiThread(() -> {
            initWebView(appData, false);
            webview.loadData(html, "application/html", "UTF-8");
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
            String url = request.getUrl().toString();
            //use the host defined in http - it can be a proxy...
            url = url.replaceAll("https://\\w+.bitgo.com", http.getHost());
            Request.Builder reqbuilder = new Request.Builder().url(url);

            Log.d(TAG, ">> " + request.getMethod() + " " + request.getUrl());
            String path = request.getUrl().getPath();
            if (path == null){
                throw new RuntimeException("Path cannot be null");
            }
            if (path.contains("/key/")) {
                String hmac = request.getRequestHeaders().get("HMAC");
                addFreshSafetynetHeader(reqbuilder, hmac);
            } else if (path.contains("/wallet/")){
                String key = "x-safetynet";
                String value = Global.getSafetynetResponseJwt();
                Log.d(TAG, ">> " + key + ": " + value);
                reqbuilder.addHeader(key, value);
            }
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

            String message = resp.message();
            if (message == null || message.trim().length() == 0)
                message = "no-status";        //BUG in www.bitgo.com (which actually return proper value on test.bitgo.com
            Log.d(TAG, "<< " + resp.code() + " " + message);
            HashMap<String, String> headers = new HashMap<>();
            resp.headers().toMultimap().forEach((key, val) -> {
                if (val.size() != 1)
                    Log.w(TAG, "ignore multi-value response header: " + key + ": " + val);
                else
                    Log.d(TAG, "<< " + key + ": " + val);
                headers.put(key, val.get(0));
            });
            Log.d(TAG, "<< body: " + resp.peekBody(100000).string());
            return new WebResourceResponse("application/json", "UTF-8", resp.code(), message, headers, resp.body().byteStream());
        }

        private void addFreshSafetynetHeader(Request.Builder reqbuilder, String hmac) throws IOException {
            AtomicReference<SafetyNetResponse> safetyNetResponse = new AtomicReference<>();
            CountDownLatch s = new CountDownLatch(1);
            mSafetyNetHelper.sendSafetyNetRequest(Global.mainActivity, hmac.getBytes(), response -> {
                safetyNetResponse.set(response);
                s.countDown();
            }, exception -> {
                exception.printStackTrace();
                s.countDown();
            });
            try {
                s.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new IOException("Failed to get SafetyNet");
            }

            if (safetyNetResponse.get() == null) {
                throw new IOException("Failed to get SafetyNet");
            }

            String key = "x-safetynet";
            String value = safetyNetResponse.get().getJwsResult();
            Log.d(TAG, ">> " + key + ": " + value);
            reqbuilder.addHeader(key, value);
        }
    }

}

