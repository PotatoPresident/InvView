package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.InventoryPower;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.util.Tristate;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.world.dimension.DimensionType;
import us.potatoboy.invview.gui.SavingPlayerDataGui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ViewCommand {
    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();

    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                for (int i = 0; i < player.getInventory().size(); i++) {
                    gui.setSlotRedirect(i, new Slot(requestedPlayer.getInventory(), i, 0, 0));
                }

                gui.open();
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
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X3, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                for (int i = 0; i < requestedEchest.size(); i++) {
                    gui.setSlotRedirect(i, new Slot(requestedEchest, i, 0, 0));
                }

                gui.open();
            }
        });

        return 1;
    }

    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
        TrinketComponent requestedComponent = TrinketsApi.getTrinketComponent(requestedPlayer).get();

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X2, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                int index = 0;
                for (Map<String, TrinketInventory> group : requestedComponent.getInventory().values()) {
                    for (TrinketInventory inventory : group.values()) {
                        for (int i = 0; i < inventory.size(); i++) {
                            gui.setSlotRedirect(index, new Slot(inventory, i, 0, 0));
                            index += 1;
                        }
                    }
                }

                gui.open();
            }
        });

        return 1;
    }

    public static int origin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                List<InventoryPower> inventories = PowerHolderComponent.getPowers(requestedPlayer, InventoryPower.class);
                if (inventories.isEmpty()) {
                    context.getSource().sendError(new LiteralText("Requested player has no inventory power"));
                } else {
                    SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, player, requestedPlayer);
                    gui.setTitle(requestedPlayer.getName());
                    int index = 0;
                    for (InventoryPower inventory : inventories) {
                        for (int i = 0; i < inventory.size(); i++) {
                            gui.setSlotRedirect(index, new Slot(inventory, index, 0, 0));
                            index += 1;
                        }
                    }

                    gui.open();
                }
            }
        });

        return 1;
    }

    private static ServerPlayerEntity getRequestedPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        GameProfile requestedProfile = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerManager().createPlayer(requestedProfile);
            NbtCompound compound = minecraftServer.getPlayerManager().loadPlayerData(requestedPlayer);
            if (compound != null) {
                ServerWorld world = minecraftServer.getWorld(
                        DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, compound.get("Dimension"))).result().get()
                );

                if (world != null) {
                    requestedPlayer.setWorld(world);
                }
            }
        }

        return requestedPlayer;
    }

    private static CompletableFuture<Boolean> isProtected(ServerPlayerEntity playerEntity) {
        if (!InvView.isLuckPerms) return CompletableFuture.completedFuture(false);

        return LuckPermsProvider.get().getUserManager().loadUser(playerEntity.getUuid())
                .thenApplyAsync(user -> {
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData(user.getQueryOptions());
                    Tristate tristate = permissionData.checkPermission("invview.protected");
                    if (tristate.equals(Tristate.UNDEFINED)) {
                        return false;
                    }

                    return tristate.asBoolean();
                });
    }
}
