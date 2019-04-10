package com.tabookey.bizpoc.utils;

import com.tabookey.bizpoc.crypto.Crypto;

import org.junit.Test;

import static org.junit.Assert.*;

public class CryptoTest {

    @Test
    public void test_fromBase64() {
        assertArrayEquals(new byte[]{0},  Crypto.fromBase64("AA"));
        assertArrayEquals(new byte[]{0},  Crypto.fromBase64("AA=="));
        assertArrayEquals("hello".getBytes(), Crypto.fromBase64("aGVsbG8="));
    }

    @Test
    public void test_toBase64() {
        assertEquals("aGVsbG8=", Crypto.toBase64("hello".getBytes()));
        assertEquals("AA==",  Crypto.toBase64(new byte[]{0}));
    }

    //encrypted data built in sjcl in bizpoc-qr.html
    String password = "password";
    String orig = "my secret text";
    String enc = "{\"iv\":\"rafNW54Dgz0jyKpVi0N3LQ==\",\"v\":1,\"iter\":1000,\"ks\":128,\"ts\":64,\"mode\":\"ccm\",\"adata\":\"\",\"cipher\":\"aes\",\"salt\":\"xO8jBPNQbwI=\",\"ct\":\"8ZwCH2d0q86oWekVwH1YoZGENbA9gw==\"}";

    @Test
    public void test_decrypt() throws Exception {
        assertEquals(Crypto.decrypt(enc, password), orig);
    }

    @Test
    public void test_scrypt() throws Exception {
        assertEquals( "asd" ,Crypto.scrypt("password", "salt") );
    }



}