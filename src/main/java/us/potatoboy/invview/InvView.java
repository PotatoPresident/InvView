package us.potatoboy.invview;

import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.LogManager;

import java.io.File;

public class InvView implements ModInitializer {
    private static MinecraftServer minecraftServer;
    public static boolean isTrinkets = false;
    public static boolean isLuckPerms = false;
    public static boolean isOrigins = false;

    @Override
    public void onInitialize() {
        isTrinkets = FabricLoader.getInstance().isModLoaded("trinkets");
        isLuckPerms = FabricLoader.getInstance().isModLoaded("luckperms");
        isOrigins = FabricLoader.getInstance().isModLoaded("origins");

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {

            LiteralCommandNode<ServerCommandSource> viewNode = CommandManager
                    .literal("view")
                    .requires(Permissions.require("invview.command.root", 2))
                    .build();

            LiteralCommandNode<ServerCommandSource> invNode = CommandManager
                    .literal("inv")
                    .requires(Permissions.require("invview.command.inv", 2))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .executes(ViewCommand::inv))
                    .build();

            LiteralCommandNode<ServerCommandSource> echestNode = CommandManager
                    .literal("echest")
                    .requires(Permissions.require("invview.command.echest", 2))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .executes(ViewCommand::eChest))
                    .build();

            /*
            LiteralCommandNode<ServerCommandSource> trinketNode = CommandManager
                    .literal("trinket")
                    .requires(Permissions.require("invview.command.trinket", 2))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .executes(ViewCommand::trinkets))
                    .build();

            LiteralCommandNode<ServerCommandSource> originNode = CommandManager
                    .literal("origin-inv")
                    .requires(Permissions.require("invview.command.origin", 2))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .executes(ViewCommand::origin))
                    .build();
             */


            dispatcher.getRoot().addChild(viewNode);
            viewNode.addChild(invNode);
            viewNode.addChild(echestNode);
            /*
            if (isTrinkets) {
                viewNode.addChild(trinketNode);
            }
            if (isOrigins) {
                viewNode.addChild(originNode);
            }
             */
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onLogicalServerStarting);
    }

    private void onLogicalServerStarting(MinecraftServer server) {
        minecraftServer = server;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static void savePlayerData(ServerPlayerEntity player) {
        File playerDataDir = minecraftServer.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        try {
            NbtCompound compoundTag = player.writeNbt(new NbtCompound());
            File file = File.createTempFile(player.getUuidAsString() + "-", ".dat", playerDataDir);
            NbtIo.writeCompressed(compoundTag, file);
            File file2 = new File(playerDataDir, player.getUuidAsString() + ".dat");
            File file3 = new File(playerDataDir, player.getUuidAsString() + ".dat_old");
            Util.backupAndReplace(file2, file, file3);
        } catch (Exception var6) {
            LogManager.getLogger().warn("Failed to save player data for {}", player.getName().getString());
        }
    }
}
