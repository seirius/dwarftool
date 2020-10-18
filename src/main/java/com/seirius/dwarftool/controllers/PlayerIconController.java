package com.seirius.dwarftool.controllers;

import com.seirius.dwarftool.util.Controller;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.util.UUID;

public class PlayerIconController implements HttpHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PATH = "/api/players/icon/";

    private final ServerWorld world;

    public PlayerIconController(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String path = httpExchange.getRequestURI().getPath();
            String playerUuid = path.substring(PATH.length());
            final PlayerEntity playerEntity = world.getPlayerByUuid(UUID.fromString(playerUuid));

//            skinManager.loadSkin(profiles.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            System.out.println(playerEntity);
            String url = playerEntity.getGameProfile().getName();
            Controller.prepareResponse(httpExchange);
            httpExchange.sendResponseHeaders(200, url.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(url.getBytes());
            outputStream.close();
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
             httpExchange.close();
        }
    }

}
