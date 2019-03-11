package com.tabookey.bizpoc;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tabookey.bizpoc.impl.HttpReq;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.RequiresApi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebvActivity extends AppCompatActivity {
    private static final String TAG = "TAG";

//    export WID=5c6d85e6d6f55fe103855738d2cb2419
//    export spendertok=v2xeac0db21649aedbc1fb77c577277c5034a0b6658abc9b408de0581374e0fbf75
//    export spender3=v2x3dd8c1007ef140e2de51d065d637f772c20e92d197e0c583c003f9c0e954ace3 #expired
//    export spender5=v2xa278dc706d1093903eb43c51308e2debacb86c1cfd9c54b3d07e55bdf1ffa818 #unlimited
//    export spender6=v2xf615bc0cc52425cff7b7243001378e79b62c0c5e637bef2d837db016df4a9ed8 #unlimited

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webv);

        sendCoins("0x7149173Ed76363649675C3D0684cd4Bac5A1006d", "45678000000", "teth", "000000", "asd/asd-ASD", true);
    }

    public static class AppObject {
        final String amount, dest, coin, otp, walletPassphrase;
        private final boolean testNetwork;

        AppObject(String dest, String ammount, String coin, String otp, String walletPassphrase, boolean testNetwork) {
            this.amount = ammount;
            this.dest = dest;
            this.coin = coin;
            this.otp = otp;
            this.walletPassphrase = walletPassphrase;
            this.testNetwork = testNetwork;
        }

        @JavascriptInterface
        public boolean getIsTest() {
            return true;    //false for production
        }

        @JavascriptInterface
        public String getCoin() {
            return coin;
        }

        @JavascriptInterface
        public String getAmount() {
            return amount;
        }

        @JavascriptInterface
        public String getDest() {
            return dest;
        }

        @JavascriptInterface
        public String getOtp() {
            return otp;
        }

        @JavascriptInterface
        public String getWalletPassphrase() { return walletPassphrase; }

        @JavascriptInterface
        public String getAccessKey() {
            return "v2xe3de01b2a3394785d315b0723523f77ddab9114480ba96bd50828d5974c86ef3";
//        return Global.getAccessKey();
        }

        @JavascriptInterface
        public String getWalletId() {
            return "5c6d85e6d6f55fe103855738d2cb2419";
        }

        @JavascriptInterface
        public void log(String msg) {
            Log.d("TAG", "log: " + msg);
        }
    }

    void sendCoins(String dest, String amount, String coin, String otp, String walletPassphrase, boolean testNetwork) {
        AppObject appData = new AppObject(dest, amount, coin, otp, walletPassphrase, testNetwork);
        WebView webv = findViewById(R.id.webview);
        webv.setWebViewClient(new MyWebviewClient(appData));
        webv.getSettings().setJavaScriptEnabled(true);

        webv.addJavascriptInterface(appData, "app");

//        try {
//            InputStream is = getResources().getAssets().open("www/sender.html");
//            int size = is.available();
//            byte[] buf = new byte[size];
//            is.read(buf);
//            webv.loadDataWithBaseURL("https://test.bitgo.com", new String(buf), "test/html", "UTF-8", null);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        webv.loadUrl("file:///android_asset/www/sender.html");
        webv.loadUrl(prefix + "www/sender.html");
    }

    static String prefix = "https://local.tabookey.com/";

    //from:https://stackoverflow.com/questions/17272612/android-webview-disable-cors
    private class MyWebviewClient extends WebViewClient {

        private final AppObject appData;
        private final HttpReq http;

        public MyWebviewClient(AppObject appData) {
            this.appData = appData;
            http = new HttpReq(appData.getAccessKey(), appData.testNetwork);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Date date = new Date();
            final String dateString = formatter.format(date);
            Map<String, String> headers = new HashMap<String, String>() {{
                put("Connection", "close");
                put("Date", dateString + " GMT");
                put("Access-Control-Allow-Origin", "*"/* your domain here */);
                put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
                put("Access-Control-Max-Age", "600");
                put("Access-Control-Allow-Credentials", "true");
                put("Access-Control-Allow-Headers", "*");
                put("Via", "1.1 vegur");
            }};

            if (request.getMethod().equals("OPTIONS"))
                return new WebResourceResponse(
                        null, "UTF-8", 200, "Ok", headers, null);

            InputStream is;
            String mime;

            try {
                if (request.getUrl().toString().startsWith(prefix)) {
                    String file = request.getUrl().toString().replace(prefix, "");
                    Log.d(TAG, "shouldInterceptRequest: URL: " + request.getUrl() + "\nfile: " + file);

                    is = getResources().getAssets().open(file);
                    mime = file.indexOf(".js") > 0 ? "application/json" : "text/html";
                } else {
                    //this "shortcut" doesn't work, since the javascript lib performs extra HMAC test
                    //String ret = http.get(request.getUrl().toString().replaceAll("^.*bitgo.com", ""));

                    return callWithOkHttp(http.getClient(), request);
                }
            } catch (IOException e) {
                Log.e(TAG, "shouldInterceptRequest: " + request.getMethod() + " " + request.getUrl(), e);
                return null;
//                throw new RuntimeException(e);
            }
            return new WebResourceResponse(
                    mime, "UTF-8", 200, "Ok", headers, is);

        }

        private synchronized WebResourceResponse callWithOkHttp(OkHttpClient client, WebResourceRequest request) throws IOException {
            Request.Builder reqbuilder = new Request.Builder()
                    .url(request.getUrl().toString());
//            if ( request.getUrl().toString().contains("wallet")) {
//
//                reqbuilder.url("https://test.bitgo.com/api/v1/user/session" );
//                Log.d(TAG, "callWithOkHttp: forceurl");
//            }

            Log.d(TAG, ">> "+request.getMethod()+" "+request.getUrl());
            request.getRequestHeaders().forEach((key, value) -> {
                if ( "Authorization, HMAC, Auth-Timestamp, BitGo-Auth-Version".toLowerCase().indexOf(key.toLowerCase())>=0 ) {
                    reqbuilder.addHeader(key, value);
                    Log.d(TAG, ">> " + key + ": " + value);
                } else {
                    Log.d(TAG, "SKIPPED >> " + key + ": " + value);
                }
            });
            if ( !request.getMethod().equals("GET") ) {

                MediaType mediatype = MediaType.parse("application/json");
                reqbuilder.method(request.getMethod(), RequestBody.create(mediatype, request.getRequestHeaders().get("x-body")));
            }
            Response resp = client.newCall(reqbuilder.build()).execute();

            //wrap up into okhttp request (and response

            Log.d(TAG, "<< "+resp.code()+" "+resp.message());
            HashMap<String, String> headers = new HashMap<>();
            resp.headers().toMultimap().forEach((key, val) -> {
                if (val.size() != 1)
                    Log.w(TAG, "ignore multi-value response header: " + key + ": " + val);
                else
                    Log.d(TAG, "<< "+key+": "+val);
                headers.put(key, val.get(0));
            });
            Log.d(TAG, "resp="+resp.peekBody(100000).string());
            return new WebResourceResponse( "application/json", "UTF-8", 200, "Ok", headers, resp.body().byteStream());
        }

        /**/

    }

    static final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US);

    WebResourceResponse buildOptionsResponse() {
        Date date = new Date();
        final String dateString = formatter.format(date);

        Map<String, String> headers = new HashMap<String, String>() {{
            put("Connection", "close");
            put("Content-Type", "text/plain");
            put("Date", dateString + " GMT");
            put("Access-Control-Allow-Origin", "*"/* your domain here */);
            put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            put("Access-Control-Max-Age", "600");
            put("Access-Control-Allow-Credentials", "true");
            put("Access-Control-Allow-Headers", "*");
            put("Via", "1.1 vegur");
        }};

        return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", headers, null);
    }
}
