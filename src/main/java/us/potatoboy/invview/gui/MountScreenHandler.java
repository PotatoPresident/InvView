package us.potatoboy.invview.gui;

import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.HorseScreenHandler;

public class MountScreenHandler extends HorseScreenHandler {
    public MountScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, HorseBaseEntity entity) {
        super(syncId, playerInventory, inventory, entity);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
