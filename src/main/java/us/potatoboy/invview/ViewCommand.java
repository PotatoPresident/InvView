package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ViewCommand {
    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();

    public static int Inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = GetRequestedPlayer(context);
        CombinedInv combinedInventory = new CombinedInv(requestedPlayer);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X5, syncId, player.inventory, combinedInventory, 5),
                requestedPlayer.getDisplayName()
        ));

        return 1;
    }

    public static int EChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = GetRequestedPlayer(context);
        EnderChestSavable requestedEchest = new EnderChestSavable(requestedPlayer);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                GenericContainerScreenHandler.createGeneric9x3(syncId, player.inventory, requestedEchest),
                requestedPlayer.getDisplayName()
        ));

        return 1;
    }

    private static ServerPlayerEntity GetRequestedPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        GameProfile requestedProfile = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerManager().createPlayer(requestedProfile);
            minecraftServer.getPlayerManager().loadPlayerData(requestedPlayer);
        }

        return requestedPlayer;
    }
}
