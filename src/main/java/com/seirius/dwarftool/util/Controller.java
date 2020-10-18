package com.seirius.dwarftool.util;


import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.Map;

public class Controller {

    public static void prepareResponse(HttpExchange httpExchange) {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD");
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
    }

    public static HashMap<String, String> queryToMap(String query) {
        HashMap<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

}
