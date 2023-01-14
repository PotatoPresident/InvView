package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
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
import net.minecraft.text.Text;
import net.minecraft.world.dimension.DimensionType;
import us.potatoboy.invview.gui.SavingPlayerDataGui;
import us.potatoboy.invview.gui.UnmodifiableSlot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ViewCommand {
    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();
    public static final Set<UUID> openedProfiles = new HashSet<>();

    private static final String permProtected = "invview.protected";
    private static final String permModify = "invview.can_modify";
    private static final String msgProtected = "Requested inventory is protected";
    private static final String msgOpened = "Requested inventory is opened";

    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    synchronized (openedProfiles) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (openedProfiles.contains(requestedPlayer.getUuid())) {
            context.getSource().sendError(Text.literal(msgOpened));
            return 1;
        }

        boolean canModify = Permissions.check(context.getSource(), permModify, true);

        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(Text.literal(msgProtected));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                for (int i = 0; i < requestedPlayer.getInventory().size(); i++) {
                    gui.setSlotRedirect(i, canModify ? new Slot(requestedPlayer.getInventory(), i, 0, 0) : new UnmodifiableSlot(requestedPlayer.getInventory(), i));
                }

                gui.open();
            }
        });

        return 1;
    }
    }

    public static int eChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    synchronized (openedProfiles) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (openedProfiles.contains(requestedPlayer.getUuid())) {
            context.getSource().sendError(Text.literal(msgOpened));
            return 1;
        }

        EnderChestInventory requestedEchest = requestedPlayer.getEnderChestInventory();

        boolean canModify = Permissions.check(context.getSource(), permModify, true);

        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(Text.literal(msgProtected));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X3, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                for (int i = 0; i < requestedEchest.size(); i++) {
                    gui.setSlotRedirect(i, canModify ? new Slot(requestedEchest, i, 0, 0) : new UnmodifiableSlot(requestedEchest, i));
                }

                gui.open();
            }
        });

        return 1;
    }
    }

    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    synchronized (openedProfiles) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);

        if (openedProfiles.contains(requestedPlayer.getUuid())) {
            context.getSource().sendError(Text.literal(msgOpened));
            return 1;
        }

        TrinketComponent requestedComponent = TrinketsApi.getTrinketComponent(requestedPlayer).get();

        boolean canModify = Permissions.check(context.getSource(), permModify, true);

        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(Text.literal(msgProtected));
            } else {
                SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X2, player, requestedPlayer);
                gui.setTitle(requestedPlayer.getName());
                int index = 0;
                for (Map<String, TrinketInventory> group : requestedComponent.getInventory().values()) {
                    for (TrinketInventory inventory : group.values()) {
                        for (int i = 0; i < inventory.size(); i++) {
                            gui.setSlotRedirect(index, canModify ? new Slot(inventory, i, 0, 0) : new UnmodifiableSlot(inventory, i));
                            index += 1;
                        }
                    }
                }

                gui.open();
            }
        });

        return 1;
    }
    }

    public static int origin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
//        ServerPlayerEntity player = context.getSource().getPlayer();
//        ServerPlayerEntity requestedPlayer = getRequestedPlayer(context);
//
//        boolean canModify = Permissions.check(context.getSource(), permModify, true);
//
//        Permissions.check(requestedPlayer.getUuid(), permProtected, false).thenAcceptAsync(isProtected -> {
//            if (isProtected) {
//                context.getSource().sendError(Text.literal(msgProtected));
//            } else {
//                List<InventoryPower> inventories = PowerHolderComponent.getPowers(requestedPlayer,
//                        InventoryPower.class);
//                if (inventories.isEmpty()) {
//                    context.getSource().sendError(Text.literal("Requested player has no inventory power"));
//                } else {
//                    SimpleGui gui = new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, player, requestedPlayer);
//                    gui.setTitle(requestedPlayer.getName());
//                    int index = 0;
//                    for (InventoryPower inventory : inventories) {
//                        for (int i = 0; i < inventory.size(); i++) {
//                            gui.setSlotRedirect(index, canModify ? new Slot(inventory, i, 0, 0) : new UnmodifiableSlot(inventory, i));
//                            index += 1;
//                        }
//                    }
//
//                    gui.open();
//                }
//            }
//        });

        return 1;
    }

    private static ServerPlayerEntity getRequestedPlayer(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        GameProfile requestedProfile = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerManager().createPlayer(requestedProfile);
            NbtCompound compound = minecraftServer.getPlayerManager().loadPlayerData(requestedPlayer);
            if (compound != null) {
                ServerWorld world = minecraftServer.getWorld(
                        DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, compound.get("Dimension")))
                                .result().get());

                if (world != null) {
                    requestedPlayer.setWorld(world);
                }
            }
        }

        return requestedPlayer;
    }
}
