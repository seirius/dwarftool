package com.seirius.dwarftool.util;

import net.minecraft.block.Blocks;

import java.util.Arrays;
import java.util.List;

public class Allowed {

    public static final List<String> BLOCKS = Arrays.asList(
            Blocks.DIRT.getDefaultState().toString(),
            Blocks.STONE.getDefaultState().toString(),
            Blocks.COBBLESTONE.getDefaultState().toString(),
            Blocks.WATER.getDefaultState().toString(),
            Blocks.SAND.getDefaultState().toString()
    );

}
