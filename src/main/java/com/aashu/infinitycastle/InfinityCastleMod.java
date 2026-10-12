package com.aashu.infinitycastle;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class InfinityCastleMod implements ModInitializer {
    public static final String ID = "infinitycastle";
    public static final ResourceKey<Level> CASTLE =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(ID, "castle"));
    public static Block PORTAL;

    @Override
    public void onInitialize() {
        Identifier portalId = Identifier.fromNamespaceAndPath(ID, "castle_portal");
        PORTAL = Registry.register(BuiltInRegistries.BLOCK, portalId,
                new CastlePortalBlock(BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, portalId))
                        .noCollision().noOcclusion().strength(0.2f).lightLevel(state -> 11)));

        // build the castle into every newly generated chunk of the castle dimension
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (generated && level.dimension() == CASTLE) CastleGen.generate(chunk);
        });

        // portal: standing inside a portal block sends you through
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                BlockPos bp = p.blockPosition();
                Level lv = p.level();
                if (lv.getBlockState(bp).is(PORTAL) || lv.getBlockState(bp.above()).is(PORTAL)) {
                    Portals.use(server, p);
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                dispatcher.register(Commands.literal("infinitycastle")
                        .then(Commands.literal("portal").executes(ctx -> {
                            Portals.spawnFrame(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("enter").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            Portals.enter(ctx.getSource().getServer(), p);
                            return 1;
                        }))));
    }
}
