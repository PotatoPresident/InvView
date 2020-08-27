package us.potatoboy.invview;

import com.google.gson.internal.$Gson$Preconditions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.Collection;

public class ViewCommand {
    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = EntityArgumentType.getPlayer(context, "target");
        PlayerInventory requestedInventory = requestedPlayer.inventory;
        CombinedInventory combinedInventory = new CombinedInventory(requestedInventory.main, requestedInventory.armor, requestedInventory.offHand);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) -> {
            return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X5, syncId, player.inventory, combinedInventory, 5);
        }, requestedPlayer.getDisplayName()));
        return 1;
    }

    public static int echest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = EntityArgumentType.getPlayer(context, "target");
        EnderChestInventory requestedEchest = requestedPlayer.getEnderChestInventory();

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) -> {
            return GenericContainerScreenHandler.createGeneric9x3(syncId, player.inventory, requestedEchest);
        }, requestedPlayer.getDisplayName()));
        return 1;
    }
}
