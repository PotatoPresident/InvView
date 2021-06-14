package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.OfflineNameCache;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.data.OfflineTrinketsComponent;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class InvView implements ModInitializer {
    private static MinecraftServer minecraftServer;
    public static boolean isTrinkets = false;
    public static boolean isLuckPerms = false;

    @Override
    public void onInitialize() {
        isTrinkets = FabricLoader.getInstance().isModLoaded("trinkets");
        isLuckPerms = FabricLoader.getInstance().isModLoaded("luckperms");

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

            LiteralCommandNode<ServerCommandSource> trinketNode = CommandManager
                    .literal("trinket")
                    .requires(Permissions.require("invview.command.trinket", 2))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .executes(ViewCommand::trinkets))
                    .build();


            dispatcher.getRoot().addChild(viewNode);
            viewNode.addChild(invNode);
            viewNode.addChild(echestNode);
            if (isTrinkets) {
                viewNode.addChild(trinketNode);
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onLogicalServerStarting);
    }

    public static PlayerInventory getPlayerInventory(UUID playerId) {
        ServerPlayerEntity player = minecraftServer.getPlayerManager().getPlayer(playerId);
        if (player != null)
            return player.getInventory();
        NbtCompound playerTag = loadPlayer(playerId);
        NbtList inventoryTag = playerTag.getList("Inventory", NbtElement.COMPOUND_TYPE);
        PlayerInventory inventory = new PlayerInventory(null);
        inventory.readNbt(inventoryTag);
        return inventory;
    }

    public static EnderChestInventory getPlayerEnderChest(UUID playerId) {
        ServerPlayerEntity player = minecraftServer.getPlayerManager().getPlayer(playerId);
        if (player != null)
            return player.getEnderChestInventory();
        NbtCompound playerTag = loadPlayer(playerId);
        NbtList echestTag = playerTag.getList("EnderItems", NbtElement.COMPOUND_TYPE);
        EnderChestInventory echest = new EnderChestInventory();
        echest.readNbtList(echestTag);
        return echest;
    }

    public static TrinketComponent getPlayerTrinkets(UUID playerId) {
        ServerPlayerEntity player = minecraftServer.getPlayerManager().getPlayer(playerId);
        if (player != null)
            return TrinketsApi.getTrinketComponent(player).get();
        NbtCompound playerTag = loadPlayer(playerId);
        NbtCompound trinketsTag = playerTag.getCompound("cardinal_components").getCompound("trinkets:trinkets");
        OfflineTrinketsComponent trinkets = new OfflineTrinketsComponent(EntityType.PLAYER);
        trinkets.readFromNbt(trinketsTag);
        return trinkets;
    }

    private void onLogicalServerStarting(MinecraftServer server) {
        minecraftServer = server;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static NbtCompound loadPlayer(UUID playerId) {
        if (minecraftServer.getPlayerManager().getPlayer(playerId) != null) return null;

        if (OfflineDataCache.INSTANCE.get(playerId) == null) {
            // Try to create that player
            ServerPlayerEntity player = minecraftServer.getPlayerManager().createPlayer(new GameProfile(playerId, OfflineNameCache.INSTANCE.getNameFromUUID(playerId)));
            return player.writeNbt(new NbtCompound());
        }

        return OfflineDataCache.INSTANCE.reload(playerId).copy();
    }

    public static void savePlayerInventory(UUID uuid, PlayerInventory inventory) {
        NbtCompound playerTag = loadPlayer(uuid);
        playerTag.put("Inventory", inventory.writeNbt(new NbtList()));
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }

    public static void savePlayerEnderChest(UUID uuid, EnderChestInventory inventory) {
        NbtCompound playerTag = loadPlayer(uuid);
        playerTag.put("EnderItems", inventory.toNbtList());
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }

    public static void savePlayerTrinkets(UUID uuid, OfflineTrinketsComponent trinkets) {
        NbtCompound playerTag = loadPlayer(uuid);
        if (!playerTag.contains("cardinal_components", NbtElement.COMPOUND_TYPE))
            playerTag.put("cardinal_components", new NbtCompound());
        NbtCompound componentsTag = playerTag.getCompound("cardinal_components");
        NbtCompound componentTag = new NbtCompound();
        trinkets.writeToNbt(componentTag);
        componentsTag.put("trinkets:trinkets", componentTag);
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }
}
