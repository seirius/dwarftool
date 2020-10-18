package com.seirius.dwarftool;

import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;

import java.util.HashMap;

public class ChunkStorage {

    private final HashMap<String, IChunk> chunks = new HashMap<>();

    private final ServerWorld world;

    public ChunkStorage(ServerWorld world) {
        this.world = world;
    }

    public IChunk getChunk(int worldX, int worldZ) {
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;
        String key = ChunkStorage.getKey(chunkX, chunkZ);
        IChunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.SURFACE);
            chunks.put(key, chunk);
        }
        return chunk;
    }

    public static String getKey(int chunkX, int chunkZ) {
        return String.format("%d:%d", chunkX, chunkZ);
    }

}
