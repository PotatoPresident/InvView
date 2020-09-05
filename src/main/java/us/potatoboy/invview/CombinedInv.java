package us.potatoboy.invview;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class CombinedInv extends PlayerInventory {
    private PlayerInventory inventory;

    public CombinedInv(ServerPlayerEntity player) {
        super(player);
        inventory = player.inventory;
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void onClose(PlayerEntity player) {
        InvView.SavePlayerData((ServerPlayerEntity) super.player);
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
    }

    @Override
    public void markDirty() {
        inventory.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return inventory.removeStack(slot, amount);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }
}
