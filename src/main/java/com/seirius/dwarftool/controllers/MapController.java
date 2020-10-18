package com.seirius.dwarftool.controllers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.seirius.dwarftool.ChunkStorage;
import com.seirius.dwarftool.DwarfTool;
import com.seirius.dwarftool.DwarfsForge;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MapController implements HttpHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PATH = "/api/map/";

    public static int CHUNK_SIZE = 16;

    public static int MAX_ZOOM = 12;

    private final static LoadingCache<String, byte[]> IMAGE_MAP_CACHE = CacheBuilder.newBuilder()
            .maximumSize(3000)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, byte[]>() {
                        @Override
                        public byte[] load(String key) throws Exception {
                            int[] data = Arrays.stream(key.split(":")).mapToInt(Integer::parseInt).toArray();
                            return MapController.getChunkImageAsBytes(DwarfsForge.serverWorld, data[0], data[1], data[2]);
                        }
                    }
            );

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String path = httpExchange.getRequestURI().getPath();
            String params = path.substring(PATH.length()).replace(".png", "");
            String[] zoomXZ = params.split("/");
            int zoom = Integer.parseInt(zoomXZ[0]);
            int x = Integer.parseInt(zoomXZ[1]);
            int z = Integer.parseInt(zoomXZ[2]);
            byte[] image = IMAGE_MAP_CACHE.get(getKey(x, z, MapController.transformZoom(zoom)));
            httpExchange.getResponseHeaders().set("Content-Type", "image/png");
            httpExchange.sendResponseHeaders(200, image.length);
            OutputStream output = httpExchange.getResponseBody();
            output.write(image);
            output.close();
            httpExchange.close();
        } catch (Exception e) {
            if (e instanceof IOException) {
                return;
            }
            LOGGER.error(e);
        }
    }

    public static int transformZoom(int incommingZoom) {
        return MAX_ZOOM - incommingZoom + 1;
    }

    public static BufferedImage getChunkImage(ServerWorld world, int x, int z, int zoom) {
        int zoomedSize = CHUNK_SIZE * zoom;
        int[] pixels = new int[zoomedSize * zoomedSize];

        int worldStartZ = z * zoomedSize;
        int worldStartX = x * zoomedSize;

        int minZ = z * zoomedSize;
        int minX = x * zoomedSize;
        int currentChunkX = resolveCoorForChunk(minX) / CHUNK_SIZE;
        int currentChunkZ = resolveCoorForChunk(minZ) / CHUNK_SIZE;

        ChunkStorage chunkStorage = new ChunkStorage(world);

        IChunk chunk = chunkStorage.getChunk(minX, minZ);
        for (int imageZ = 0; imageZ < zoomedSize; imageZ++) {
            int worldZ = imageZ + worldStartZ;
            for (int imageX = 0; imageX < zoomedSize; imageX++) {
                int worldX = imageX + worldStartX;

                int newCurrentX = resolveCoorForChunk(worldX) / CHUNK_SIZE;
                int newCurrentZ = resolveCoorForChunk(worldZ) / CHUNK_SIZE;
                if (newCurrentX != currentChunkX || newCurrentZ != currentChunkZ) {
                    currentChunkX = newCurrentX;
                    currentChunkZ = newCurrentZ;
                    chunk = chunkStorage.getChunk(worldX, worldZ);
                }
                int topY = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                BlockState blockState = chunk.getBlockState(new BlockPos(worldX, topY, worldZ));
                int color = blockState.getMaterial().getColor().colorValue;
                float colorFactor = getColorFactor(topY);
                pixels[imageZ * zoomedSize + imageX] = manipulateColor(color, colorFactor);
            }
        }

        BufferedImage pixelImage = new BufferedImage(zoomedSize, zoomedSize, BufferedImage.TYPE_INT_RGB);
        pixelImage.setRGB(0, 0, zoomedSize, zoomedSize, pixels, 0, zoomedSize);

        return MapController.resizeImage(pixelImage, 160, 160);
    }

    private static int resolveCoorForChunk(int coor) {
        return coor < 0 ? ++coor : coor;
    }

    public static byte[] getChunkImageAsBytes(ServerWorld world, int x, int z, int zoom) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(getChunkImage(world, x, z, zoom), "png", baos);
        baos.flush();
        byte[] byteArray = baos.toByteArray();
        baos.flush();
        return byteArray;
    }

    private static BufferedImage resizeImage(BufferedImage buf, int width, int height) {
        final BufferedImage bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = bufImage.createGraphics();
        g2.drawImage(buf, 0, 0, width, height, null);
        g2.dispose();
        return bufImage;
    }

    public static float getColorFactor(int y) {
        float oceanLevel = 62f;
        float rawFactor = oceanLevel / y;
        if (rawFactor < 1) {
            return 2 - rawFactor;
        } else {
            return (rawFactor - 2) * -1;
        }
    }

    public static int manipulateColor(int color, float factor) {
        Color col = new Color(color);
        int r, g, b;
        if (factor <= 1) {
            r = Math.round(col.getRed() * factor);
            g = Math.round(col.getGreen() * factor);
            b = Math.round(col.getBlue() * factor);
        } else {
            float difFactor = factor - 1;
            int red = col.getRed(), green = col.getGreen(), blue = col.getBlue();
            r = Math.round((255 - red) * difFactor + red);
            g = Math.round((255 - green) * difFactor + green);
            b = Math.round((255 - blue) * difFactor + blue);
        }

        int rgb = r;
        rgb = (rgb << 8) + g;
        rgb = (rgb << 8) + b;
        return rgb;
    }

    public static String getKey(int x, int z, int zoom) {
        return String.format("%d:%d:%d", x, z, zoom);
    }

}
