package com.seirius.dwarftool;

import java.util.List;

public class DwarfsWorld {

    public List<Chunk> chunks;

    public static class Chunk {
        public DwarfVector3 position;
        public List<BlockData> blocks;
    }

}
