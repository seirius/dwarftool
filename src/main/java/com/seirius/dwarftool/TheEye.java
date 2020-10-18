package com.seirius.dwarftool;

import com.seirius.dwarftool.controllers.ChunkController;
import com.seirius.dwarftool.controllers.MapController;
import com.seirius.dwarftool.controllers.PlayerIconController;
import com.seirius.dwarftool.controllers.PlayerListController;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class TheEye {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MAP = "/";
    private static final int PORT = 8080;

    public static void init() {
        try {
            LOGGER.info("Server starting");
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext(PlayerListController.PATH,  new PlayerListController(DwarfsForge.serverWorld));
            server.createContext(PlayerIconController.PATH,  new PlayerIconController(DwarfsForge.serverWorld));
            server.createContext(MapController.PATH, new MapController());
            server.createContext(ChunkController.PATH, new ChunkController());
            server.setExecutor(null);
            server.start();
            LOGGER.info("Server started");
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

}
