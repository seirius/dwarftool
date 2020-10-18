package com.seirius.dwarftool.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seirius.dwarftool.DwarfsForge;
import com.seirius.dwarftool.util.Controller;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.util.HashMap;

public class ChunkController implements HttpHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PATH = "/api/chunk";

    private static final String X = "x";
    private static final String Z = "z";

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            HashMap<String, String> params = Controller.queryToMap(httpExchange.getRequestURI().getQuery());
            String sX = params.get(X);
            if (sX == null) {
                throw new HttpException("X not present");
            }
            int x = Integer.parseInt(sX);
            String sZ = params.get(Z);
            if (sZ == null) {
                throw new HttpException("Z not present");
            }
            int z = Integer.parseInt(sZ);
            byte[] response = new ObjectMapper().writeValueAsBytes(DwarfsForge.getChunkData(x, z));
            Controller.prepareResponse(httpExchange);
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response);
            outputStream.close();
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            httpExchange.close();
        }
    }
}
