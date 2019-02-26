package com.tabookey.bizpoc.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    static ObjectMapper sJson = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static String toJson(Object obj) {
        try {
            return sJson.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T>  T fromJson(String json, Class<T> cls) {
        try {
            return sJson.readValue(json, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static double weiStringToEtherDouble(String weiString){
        BigDecimal bigIntBalance = new BigDecimal(weiString);
        BigDecimal divide = bigIntBalance.divide(new BigDecimal(Math.pow(10, 18)), 10, RoundingMode.HALF_UP);
        return divide.doubleValue();
    }


}
