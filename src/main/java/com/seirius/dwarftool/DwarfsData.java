package com.seirius.dwarftool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seirius.dwarftool.util.ChunkPosition;

import java.util.List;

public class DwarfsData {

    public List<BlockData> blocks;
    public ChunkPosition chunkPosition;

    @JsonIgnore
    public void setChunkPosition(int x, int z) {
        chunkPosition = new ChunkPosition(x, z);
    }

}
