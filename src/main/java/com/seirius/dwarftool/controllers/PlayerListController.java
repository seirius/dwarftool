package com.seirius.dwarftool.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seirius.dwarftool.util.Controller;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerListController implements HttpHandler {

    public static final String PATH = "/api/players";

    private final ServerWorld world;

    public PlayerListController(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            List<ServerPlayerEntity> players = world.getPlayers();
            List<HashMap<String, Object>> playerList = new ArrayList<>();
            for (ServerPlayerEntity player : players) {
                HashMap<String, Object> playerData = new HashMap<>();
                String playerName = player.getDisplayName().getString();
                String uuid = player.getUniqueID().toString();
                playerData.put("name", playerName);
                Vector3d position = player.getPositionVec();
                HashMap<String, Double> parsedPosition = new HashMap<>();
                parsedPosition.put("x", position.x);
                parsedPosition.put("y", position.y);
                parsedPosition.put("z", position.z);
                playerData.put("position", parsedPosition);
                playerData.put("uuid", uuid);
                playerData.put("avatar", String.format("https://mc-heads.net/avatar/%s/32.png", uuid));
                playerList.add(playerData);
            }

            byte[] response = new ObjectMapper().writeValueAsBytes(playerList);
            Controller.prepareResponse(httpExchange);
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpExchange.close();
        }
    }
}
