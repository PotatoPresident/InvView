package us.potatoboy.invview;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public class CombinedInv extends PlayerInventory {
    private PlayerInventory inventory;

    public CombinedInv(ServerPlayerEntity player) {
        super(player);
        inventory = player.inventory;
        inventory.setStack(42, new ItemStack(Items.RED_STAINED_GLASS_PANE));
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
        if (slot > 40) return;
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
}
