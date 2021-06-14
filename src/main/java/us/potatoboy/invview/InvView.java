package us.potatoboy.invview;

import com.mojang.brigadier.tree.LiteralCommandNode;
import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import us.potatoboy.invview.data.OfflineTrinketsComponent;

import java.util.UUID;

public class InvView implements ModInitializer {
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
    }

    public static void savePlayerInventory(UUID uuid, PlayerInventory inventory) {
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(uuid).copy();
        playerTag.put("Inventory", inventory.writeNbt(new NbtList()));
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }

    public static void savePlayerEnderChest(UUID uuid, EnderChestInventory inventory) {
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(uuid).copy();
        playerTag.put("EnderItems", inventory.toNbtList());
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }

    public static void savePlayerTrinkets(UUID uuid, OfflineTrinketsComponent trinkets) {
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(uuid).copy();
        if (!playerTag.contains("cardinal_components", NbtElement.COMPOUND_TYPE))
            playerTag.put("cardinal_components", new NbtCompound());
        NbtCompound componentsTag = playerTag.getCompound("cardinal_components");
        componentsTag.put("trinkets:trinkets", trinkets.writeToNbt(new NbtCompound()));
        OfflineDataCache.INSTANCE.save(uuid, playerTag);
    }
}
