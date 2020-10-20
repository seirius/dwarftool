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
    private static final String RADIUS = "radius";
    private static final String MIN_HEIGHT = "minHeight";

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
            String sRadius = params.get(RADIUS);
            Object data;
            if (sRadius != null) {
                int radius = Integer.parseInt(sRadius);
                int minHeight = 0;
                String sMinHeight = params.get(MIN_HEIGHT);
                if (sMinHeight != null) {
                    minHeight = Integer.parseInt(sMinHeight);
                }
                data = DwarfsForge.getMolecule(x, z, radius, minHeight);
            } else {
                data = DwarfsForge.getChunkData(x, z);
            }
            byte[] response = new ObjectMapper().writeValueAsBytes(data);
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
