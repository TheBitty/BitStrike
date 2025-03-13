package com.bitstrike.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for JSON operations.
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Convert an object to a JSON string.
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Convert a JSON string to an object.
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    
    /**
     * Parse a JSON string to a JsonObject.
     */
    public static JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
    
    /**
     * Format a JSON string with pretty printing.
     */
    public static String prettyPrint(String json) {
        JsonObject jsonObject = parseJson(json);
        return gson.toJson(jsonObject);
    }
} 