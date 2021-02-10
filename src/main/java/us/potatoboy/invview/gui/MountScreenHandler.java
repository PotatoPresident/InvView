package us.potatoboy.invview.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.HorseScreenHandler;

public class MountScreenHandler extends HorseScreenHandler {
    public MountScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(syncId, playerInventory, inventory, null);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
