package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.util.Tristate;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import us.potatoboy.invview.gui.EnderChestScreenHandler;
import us.potatoboy.invview.gui.PlayerInventoryScreenHandler;
import us.potatoboy.invview.gui.TrinketScreenHandler;

import java.util.concurrent.CompletableFuture;

public class ViewCommand {
    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();

    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
                return;
            } else {
                NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                        new PlayerInventoryScreenHandler(syncId, player.inventory, requestedPlayer.inventory),
                        requestedPlayer.getDisplayName()
                );

                player.openHandledScreen(screenHandlerFactory);
            }
        });

        return 1;
    }

    public static int eChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
        EnderChestInventory requestedEchest = requestedPlayer.getEnderChestInventory();

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
                return;
            } else {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                        new EnderChestScreenHandler(syncId, player.inventory, requestedEchest, 3, requestedPlayer),
                        requestedPlayer.getDisplayName()
                ));
            }
        });

        return 1;
    }

    /*
    public static int mountInv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (requestedPlayer.getVehicle() != null && requestedPlayer.getVehicle() instanceof HorseBaseEntity) {
            HorseBaseEntity mount = (HorseBaseEntity) requestedPlayer.getVehicle();

            //mount.openInventory(player);
            SimpleInventory inventory = ((HorseInventoryAccess)mount).getItems();
            //player.openHorseInventory(mount, inventory);



            if (player.currentScreenHandler != player.playerScreenHandler) {
                player.closeHandledScreen();
            }

            player.incrementScreenHandlerSyncId();
            player.networkHandler.sendPacket(new OpenHorseScreenS2CPacket(player.screenHandlerSyncId, inventory.size(), 8));
            player.currentScreenHandler = new MountScreenHandler(player.screenHandlerSyncId, player.inventory, inventory);
            player.currentScreenHandler.addListener(player);


            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player1) ->
                new MountScreenHandler(player.currentScreenHandler.syncId, player.inventory, inventory),
                requestedPlayer.getDisplayName()
            ));
        }
        return 1;
    }
    */

    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
                return;
            } else {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player1) ->
                        new TrinketScreenHandler(syncId, player.inventory, requestedPlayer),
                        requestedPlayer.getDisplayName()
                ));
            }
        });

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

    private static CompletableFuture<Boolean> isProtected(ServerPlayerEntity playerEntity) {
        if (!InvView.isLuckPerms) return CompletableFuture.completedFuture(playerEntity.hasPermissionLevel(3));

        return LuckPermsProvider.get().getUserManager().loadUser(playerEntity.getUuid())
                .thenApplyAsync(user -> {
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData(user.getQueryOptions());
                    Tristate tristate = permissionData.checkPermission("invview.protected");
                    if (tristate.equals(Tristate.UNDEFINED)) {
                        return playerEntity.hasPermissionLevel(3);
                    }

                    return tristate.asBoolean();
                });
    }
}
