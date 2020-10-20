package com.seirius.dwarftool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.seirius.dwarftool.util.Allowed;
import com.seirius.dwarftool.util.ChunkPosition;
import com.seirius.dwarftool.util.DwarfsMolecule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DwarfsForge {

    public static ObjectMapper objectMapper;
    public static ServerWorld serverWorld;

    public static final int CHUNK_SIZE = 16;

    private static final String PATH_NAME = "path";

    public static void init() {
        if (objectMapper == null) {
            DwarfsForge.objectMapper = new ObjectMapper();
        }
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("dwarftool")
                        .then(Commands
                                .literal(PATH_NAME)
                                .then(
                                        Commands
                                            .argument(PATH_NAME, StringArgumentType.greedyString())
                                            .executes(DwarfsForge::commandExecute)
                                )
                        )
        );
        dispatcher.register(
                Commands.literal("dwarftool")
                        .then(
                                Commands
                                        .literal("export")
                                        .then(
                                                Commands
                                                        .argument(PATH_NAME, StringArgumentType.greedyString())
                                                        .executes(DwarfsForge::exporter)
                                        )
                        )
        );
        dispatcher.register(
                Commands.literal("dwarftool")
                        .then(
                                Commands
                                        .literal("import")
                                        .then(
                                                Commands
                                                        .argument(PATH_NAME, StringArgumentType.greedyString())
                                                        .executes(DwarfsForge::importer)
                                        )
                        )
        );
    }

    public static int commandExecute(CommandContext<CommandSource> context) {
        String incommingPath = context.getArgument(PATH_NAME, String.class);
        Path path = Paths.get(incommingPath);
        if (Files.exists(path)) {
            try {
                Entity entity = context.getSource().getEntity();
                if (entity != null) {
                    Vector3d playerPosition = entity.getPositionVec();
                    int y = (int) playerPosition.y;
                    Chunk chunk = serverWorld.getChunk(entity.chunkCoordX, entity.chunkCoordZ);
                    DwarfsData data = readData(path);
                    forge(chunk.getPos().asBlockPos(), data, y);
                    serverWorld.updateEntity(entity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        } else {
            return 1;
        }
    }

    public static int exporter(CommandContext<CommandSource> context) {
        try {
            String incommingPath = context.getArgument(PATH_NAME, String.class);
            Entity entity = context.getSource().getEntity();
            if (entity != null) {
                Chunk chunk = serverWorld.getChunk(entity.chunkCoordX, entity.chunkCoordZ);
                DwarfsData data = getChunkData(chunk);
                writeData(data, incommingPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static ChunkPosition toChunkPosition(int blockX, int blockZ) {
        return new ChunkPosition(blockX / CHUNK_SIZE - 1, blockZ / CHUNK_SIZE);
    }

    public static DwarfsData getChunkData(int x, int z) {
        ChunkPosition chunkPosition = toChunkPosition(x, z);
        return getChunkData(serverWorld.getChunk(chunkPosition.x, chunkPosition.z));
    }

    public static DwarfsData getChunkData(Chunk chunk) {
        return getChunkData(chunk, 0);
    }

    public static DwarfsData getChunkData(Chunk chunk, int minHeight) {
        DwarfsData data = new DwarfsData();
        data.blocks = new ArrayList<>();
        ChunkPos chunkPos = chunk.getPos();
        data.setChunkPosition(chunkPos.x, chunkPos.z);
        int top = 0;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int newTop = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, x, z);
                if (top < newTop) {
                    top = newTop;
                }
            }
        }
        for (int y = minHeight; y < top; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState blockState = chunk.getBlockState(blockPos);
                    String blockString = blockState.toString();
                    if (Allowed.BLOCKS.contains(blockString)) {
                        BlockData blockData = new BlockData();
                        blockData.position = new DwarfVector3(x, y, z);
                        blockData.type = blockString;
                        data.blocks.add(blockData);
                    }
                }
            }
        }
        return data;
    }

    public static DwarfsMolecule getMolecule(int x, int z, int radius) {
        return getMolecule(x, z, radius, 0);
    }

    public static DwarfsMolecule getMolecule(int x, int z, int radius, int minHeight) {
        DwarfsMolecule dwarfsMolecule = new DwarfsMolecule();
        dwarfsMolecule.chunks = new ArrayList<>();
        ChunkPosition chunkPosition = toChunkPosition(x, z);
        for (int chunkX = chunkPosition.x - radius; chunkX <= chunkPosition.x + radius; chunkX++) {
            for (int chunkZ = chunkPosition.z - radius; chunkZ <= chunkPosition.z + radius; chunkZ++) {
                dwarfsMolecule.chunks.add(getChunkData(serverWorld.getChunk(chunkX, chunkZ), minHeight));
            }
        }
        return dwarfsMolecule;
    }

    public static int importer(CommandContext<CommandSource> context) {
        String incommingPath = context.getArgument(PATH_NAME, String.class);
        Path path = Paths.get(incommingPath);
        if (Files.exists(path)) {
            try {
                Entity entity = context.getSource().getEntity();
                if (entity != null) {
                    Chunk chunk = serverWorld.getChunk(entity.chunkCoordX, entity.chunkCoordZ);
                    DwarfsData data = readData(path);
                    forge(chunk.getPos().asBlockPos(), data, 0);
                    serverWorld.updateEntity(entity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        } else {
            return 1;
        }
    }

    public static DwarfsData readData(Path path) throws IOException {
        return objectMapper
                .readValue(
                        Files.readAllBytes(path),
                        DwarfsData.class
                );
    }

    public static void writeData(DwarfsData data, String incommingPath) throws IOException {
        Files.write(Paths.get(incommingPath), objectMapper.writeValueAsBytes(data));
    }

    public static void forge(BlockPos origin, DwarfsData data, int height) {
        try {
            System.out.println(origin);
            data.blocks.forEach(block -> {
                BlockState blockState = block.type.equals("dirt") ? Blocks.DIRT.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
                BlockPos pos = new BlockPos(block.position.x + origin.getX(), block.position.y + height, block.position.z + origin.getZ());
                serverWorld.setBlockState(pos, blockState);
            });
        } catch (Exception e) {
            System.err.println("DWARF'S FORGE FAILED");
            e.printStackTrace();
        }
    }

}
