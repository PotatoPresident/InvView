package us.potatoboy.invview.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.InvView;
import us.potatoboy.invview.ViewCommand;

public class EnderChestScreenHandler extends GenericContainerScreenHandler {
    private ServerPlayerEntity requestedPlayer;

    public EnderChestScreenHandler(int syncId, PlayerInventory playerInventory, EnderChestInventory inventory, int rows, ServerPlayerEntity requestedPlayer) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, rows);
        this.requestedPlayer = requestedPlayer;
    }

    @Override
    public void close(PlayerEntity player) {
        InvView.SavePlayerData(requestedPlayer);
        super.close(player);
    }
}
