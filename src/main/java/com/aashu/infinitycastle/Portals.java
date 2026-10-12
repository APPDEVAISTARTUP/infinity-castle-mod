package com.aashu.infinitycastle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Teleporting is done through vanilla /execute + /tp so it does not depend on internal teleport APIs. */
public final class Portals {
    private record Saved(String dim, double x, double y, double z, Direction facing) {}

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Saved> BACK = new HashMap<>();
    private static final String CASTLE_DIM = InfinityCastleMod.ID + ":castle";

    private Portals() {}

    public static void use(MinecraftServer server, ServerPlayer p) {
        long now = System.currentTimeMillis();
        Long last = COOLDOWN.get(p.getUUID());
        if (last != null && now - last < 3000) return;
        if (p.level().dimension() == InfinityCastleMod.CASTLE) leave(server, p); else enter(server, p);
    }

    private static String dimName(Level level) {
        if (level.dimension() == Level.NETHER) return "minecraft:the_nether";
        if (level.dimension() == Level.END) return "minecraft:the_end";
        return "minecraft:overworld";
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    public static void enter(MinecraftServer server, ServerPlayer p) {
        ServerLevel cw = server.getLevel(InfinityCastleMod.CASTLE);
        if (cw == null) {
            p.sendSystemMessage(Component.literal("Infinity Castle dimension load nahi hui. Mod sahi se installed hai?"));
            return;
        }
        if (p.level().dimension() != InfinityCastleMod.CASTLE) {
            BACK.put(p.getUUID(), new Saved(dimName(p.level()), p.getX(), p.getY(), p.getZ(), p.getDirection()));
        }
        cw.getChunk(-1, 0);
        cw.getChunk(0, 0);
        cw.getChunk(0, 1);
        build(cw, new BlockPos(0, 137, 14), Direction.Axis.X); // return portal inside the tower hall
        COOLDOWN.put(p.getUUID(), System.currentTimeMillis());
        run(server, String.format(Locale.ROOT, "execute in %s run tp %s 0.5 136 10.5 0 0", CASTLE_DIM, p.getStringUUID()));
    }

    private static void leave(MinecraftServer server, ServerPlayer p) {
        Saved s = BACK.get(p.getUUID());
        COOLDOWN.put(p.getUUID(), System.currentTimeMillis());
        if (s != null) {
            double x = s.x() - s.facing().getStepX() * 2.0;
            double z = s.z() - s.facing().getStepZ() * 2.0;
            run(server, String.format(Locale.ROOT, "execute in %s run tp %s %.2f %.2f %.2f", s.dim(), p.getStringUUID(), x, s.y(), z));
        } else {
            run(server, "execute in minecraft:overworld positioned 0.5 0 0.5 positioned over motion_blocking run tp "
                    + p.getStringUUID() + " ~ ~ ~");
        }
    }

    /** /infinitycastle portal : frame in front of the player. */
    public static void spawnFrame(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel w)) return;
        Direction f = p.getDirection();
        Direction.Axis axis = f.getClockWise().getAxis();
        build(w, p.blockPosition().relative(f, 3), axis);
    }

    /** base = bottom-left block of the 2x3 interior. Frame is 4x5. */
    public static void build(ServerLevel w, BlockPos base, Direction.Axis axis) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState portal = InfinityCastleMod.PORTAL.defaultBlockState().setValue(CastlePortalBlock.AXIS, axis);
        for (int a = -1; a <= 2; a++) {
            for (int y = -1; y <= 3; y++) {
                BlockPos pos = base.relative(along, a).above(y);
                boolean edge = a == -1 || a == 2 || y == -1 || y == 3;
                boolean corner = (a == -1 || a == 2) && (y == -1 || y == 3);
                if (corner) w.setBlockAndUpdate(pos, Blocks.GOLD_BLOCK.defaultBlockState());
                else if (edge) w.setBlockAndUpdate(pos, Blocks.RED_NETHER_BRICKS.defaultBlockState());
                else w.setBlockAndUpdate(pos, portal);
            }
        }
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false);
        w.setBlockAndUpdate(base.relative(along, -1).above(4), lantern);
        w.setBlockAndUpdate(base.relative(along, 2).above(4), lantern);
    }
}
