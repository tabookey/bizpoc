package com.tabookey.bizpoc.impl;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bumptech.glide.util.Preconditions.checkArgument;


public class Utils {

    public static String getDayOfMonthSuffix(Date date) {
        SimpleDateFormat formatDayOfMonth  = new SimpleDateFormat("d");
        int n = Integer.parseInt(formatDayOfMonth.format(date));
        checkArgument(n >= 1 && n <= 31, "illegal day of month: " + n);
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
    static ObjectMapper sJson = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS,true)
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

    public static double integerStringToCoinDouble(String weiString, int decimals){
        BigDecimal bigIntBalance = new BigDecimal(weiString);
        BigDecimal divide = bigIntBalance.divide(new BigDecimal(Math.pow(10, decimals)), 10, RoundingMode.HALF_UP);
        return divide.doubleValue();
    }
    public static void showErrorDialog(Activity activity, String errorMessage) {
        if (activity == null) {
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Transaction failed!");
        alertDialog.setMessage(errorMessage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


}
