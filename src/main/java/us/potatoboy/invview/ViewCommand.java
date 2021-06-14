package us.potatoboy.invview;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.emi.trinkets.api.TrinketInventory;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.OfflineNameCache;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.util.Tristate;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import us.potatoboy.invview.data.OfflineTrinketsComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ViewCommand {
    public static int inv(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        UUID requestedPlayer = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next().getId();
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(requestedPlayer).copy();
        NbtList inventoryTag = playerTag.getList("Inventory", NbtElement.COMPOUND_TYPE);
        PlayerInventory inventory = new PlayerInventory(null);
        inventory.readNbt(inventoryTag);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X5, player, false) {
                    @Override
                    public void onClose() {
                        InvView.savePlayerInventory(requestedPlayer, inventory);
                    }
                };
                gui.setTitle(Text.of(OfflineNameCache.INSTANCE.getNameFromUUID(requestedPlayer)));
                for (int i = 0; i < player.getInventory().size(); i++) {
                    gui.setSlotRedirect(i, new Slot(inventory, i, 0, 0));
                }

                gui.open();
            }
        });

        return 1;
    }

    public static int eChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        UUID requestedPlayer = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next().getId();
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(requestedPlayer).copy();
        NbtList echestTag = playerTag.getList("EnderItems", NbtElement.COMPOUND_TYPE);
        EnderChestInventory echest = new EnderChestInventory();
        echest.readNbtList(echestTag);

        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false) {
                    @Override
                    public void onClose() {
                        InvView.savePlayerEnderChest(requestedPlayer, echest);
                    }
                };
                gui.setTitle(Text.of(OfflineNameCache.INSTANCE.getNameFromUUID(requestedPlayer)));
                for (int i = 0; i < echest.size(); i++) {
                    gui.setSlotRedirect(i, new Slot(echest, i, 0, 0));
                }

                gui.open();
            }
        });

        return 1;
    }

    public static int trinkets(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        UUID requestedPlayer = GameProfileArgumentType.getProfileArgument(context, "target").iterator().next().getId();
        NbtCompound playerTag = OfflineDataCache.INSTANCE.reload(requestedPlayer).copy();
        NbtCompound trinketsTag = playerTag.getCompound("cardinal_components").getCompound("trinkets:trinkets");
        OfflineTrinketsComponent trinketsComponent = new OfflineTrinketsComponent(EntityType.PLAYER);
        trinketsComponent.readFromNbt(trinketsTag);
        isProtected(requestedPlayer).thenAcceptAsync(isProtected -> {
            if (isProtected) {
                context.getSource().sendError(new LiteralText("Requested inventory is protected"));
            } else {
                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X2, player, false) {
                    @Override
                    public void onClose() {
                        InvView.savePlayerTrinkets(requestedPlayer, trinketsComponent);
                    }
                };
                gui.setTitle(Text.of(OfflineNameCache.INSTANCE.getNameFromUUID(requestedPlayer)));

                int index = 0;
                for (Map<String, TrinketInventory> group : trinketsComponent.getInventory().values()) {
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

    private static CompletableFuture<Boolean> isProtected(UUID playerId) {
        if (!InvView.isLuckPerms) return CompletableFuture.completedFuture(false);

        return LuckPermsProvider.get().getUserManager().loadUser(playerId)
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
