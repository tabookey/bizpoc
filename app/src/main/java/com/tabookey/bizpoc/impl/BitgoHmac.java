package com.tabookey.bizpoc.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class BitgoHmac {

    public static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * add Hmac-related request headers to request.
     * add (or replaces) HMAC, auth-timestamp, bitgo-auth-version, authorization)
     *
     * @param bld   build request to update.
     * @param token - accessToken to use.
     */
    public static void addRequestHeaders(Request.Builder bld, String token) {
        Request req = bld.build();

        //TODO: somehow, our calculation of requests with a query string fail the HMAC check.
        if ( isOldAuthRequest(req)) {
            bld.header("Authorization", "Bearer " + token);
            return;
        }
        Buffer bodyBuffer = new Buffer();
        String bodyString;
        try {
            RequestBody body = bld.build().body();
            if ( body==null )
                bodyString="";
            else {
                body.writeTo(bodyBuffer);
                bodyString = bodyBuffer.readUtf8();
            }
        } catch (IOException e) {
            throw new RuntimeException("unexpected (in memory url)");
        }

        long timestamp = new Date().getTime();
        HashMap<String, String> headers = calculateRequestHeaders(req.url().toString(), bodyString, timestamp, token);
        for (String k : headers.keySet())
            bld.header(k, headers.get(k));
    }

    /**
     * validate the response from the server
     *
     * @param resp  the received response
     * @param token access token
     */
    public static void verifyResponse(Response resp, String token) throws IOException {

        String body = resp.peekBody(Integer.MAX_VALUE).string();
        if ( body.contains("\"error\":\"unauthorized\""))
            throw new RuntimeException(body);

        if ( isOldAuthRequest(resp.request()))
            return;

        long resp_timestamp = Long.parseLong(resp.header("timestamp","0"));
        long req_timestamp = Long.parseLong(resp.request().header("auth-timestamp"));
//        if ( resp_timestamp<req_timestamp ) {
//            throw new RuntimeException("invalid response: timestamp prior request timestamp");
//        }
        verifyResponse(resp.request().url().toString(), resp.code(),
                body, resp_timestamp, token, resp.header("HMAC"));
    }

    public static boolean force_oldAuth=false;

    //some requests (with query string) fail the HMAC calculation. so for now, we skip HMAC calculation for them
    private static boolean isOldAuthRequest(Request req) {
        if ( force_oldAuth)
            return true;
        return req.url().toString().contains("?");
    }

    public static HashMap<String, String> calculateRequestHeaders(String url, String text, long timestamp, String token) {
        if (!token.startsWith("v2x"))
            throw new IllegalArgumentException("invalid access token: " + token);

        String hmac = calculateRequestHMAC(url, text, timestamp, token);
        String tokenHash;
        try {
            tokenHash = toHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, String> ret = new HashMap<>();
        ret.put("Auth-Timestamp", String.valueOf(timestamp));
        ret.put("HMAC", hmac);
        ret.put("Authorization", "Bearer " + tokenHash);
        ret.put("BitGo-Auth-Version", "2.0");
        return ret;
    }

    static void verifyResponse(String url, int statusCode, String text, long timestamp, String token, String hmac) {

        String signatureSubject = calculateHMACSubject(url, text, timestamp, statusCode);

        // calculate the HMAC
        String expectedHmac = calculateHMAC(token, signatureSubject);

        // verify the HMAC and timestamp
        if (!expectedHmac.equals(hmac))
            throw new RuntimeException("HMAC validation failed:\n" +
                    "expectedHmac: " + expectedHmac + "\n" +
                    "signatureSubject: " + signatureSubject);
    }

    static String calculateHMACSubject(String url, String text, long timestamp, int statusCode) {
        try {
            String urlQuery = new URL(url).getPath();
            if (statusCode > 0)
                return String.format("%s|%s|%s|%s", timestamp, urlQuery, statusCode, text);
            else
                return String.format("%s|%s|%s", timestamp, urlQuery, text);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static String calculateRequestHMAC(String urlPath, String text, long timestamp, String token) {
        String signatureSubject = calculateHMACSubject(urlPath, text, timestamp, 0);

        return calculateHMAC(token, signatureSubject);
    }

    static String calculateHMAC(String token, String signatureSubject) {
        try {
            byte[] keybytes = token.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(keybytes, HMAC_SHA256);
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            hmac.init(secretKey);
            hmac.update(signatureSubject.getBytes());
            return toHex(hmac.doFinal());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] fromHex(String token) {
        byte[] b = new byte[token.length() / 2];
        for (int i = 0; i < token.length(); i += 2) {
            b[i / 2] = (byte) Integer.parseInt(token.substring(i, i + 2), 16);
        }
        return b;
    }

    static String toHex(byte[] bytes) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            b.append(String.format("%02x", bytes[i] & 255));
        }
        return b.toString();
    }


}
