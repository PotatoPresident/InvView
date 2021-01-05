package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import us.potatoboy.invview.gui.EnderChestScreenHandler;
import us.potatoboy.invview.gui.PlayerInventoryScreenHandler;
import us.potatoboy.invview.gui.TrinketScreenHandler;
import us.potatoboy.invview.mixin.HorseInventoryAccess;

import java.util.ArrayList;
import java.util.List;

public class ViewCommand {
    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();

    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (isProtected(context, requestedPlayer)) {
            return -1;
        }

        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new PlayerInventoryScreenHandler(syncId, player.inventory, requestedPlayer.inventory),
                requestedPlayer.getDisplayName()
        );

        player.openHandledScreen(screenHandlerFactory);
        
        return 1;
    }

    public static int eChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
        EnderChestInventory requestedEchest = requestedPlayer.getEnderChestInventory();

        if (isProtected(context, requestedPlayer)) {
            return -1;
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new EnderChestScreenHandler(syncId, player.inventory, requestedEchest, 3, requestedPlayer),
                requestedPlayer.getDisplayName()
        ));

        return 1;
    }
    /*
    public static int mountInv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (requestedPlayer.getVehicle() != null && requestedPlayer.getVehicle() instanceof HorseBaseEntity) {
            HorseBaseEntity mount = (HorseBaseEntity) requestedPlayer.getVehicle();

            mount.openInventory(player);
            SimpleInventory inventory = ((HorseInventoryAccess)mount).getItems();
            player.openHorseInventory(mount, inventory);

            if (player.currentScreenHandler != player.playerScreenHandler) {
                player.closeCurrentScreen();
            }

            //player.openHandledScreen(new CanOpenHorseScreenHandler(player.currentScreenHandler.syncId, player.inventory, inventory, mount));
        }
        return 1;
    }
     */

    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (isProtected(context, requestedPlayer)) {
            return -1;
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player1) ->
                new TrinketScreenHandler(syncId, player.inventory, requestedPlayer),
                requestedPlayer.getDisplayName()
        ));
        return 1;
    }

    private static ServerPlayerEntity getRequestedPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        GameProfile requestedProfile = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerManager().createPlayer(requestedProfile);
            minecraftServer.getPlayerManager().loadPlayerData(requestedPlayer);
        }

        return requestedPlayer;
    }

    private static boolean isProtected(CommandContext<ServerCommandSource> context, ServerPlayerEntity requested) throws CommandSyntaxException {
        if (Permissions.check(requested, "invview.protected")) {
            context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            return true;
        }

        return false;
    }
}
