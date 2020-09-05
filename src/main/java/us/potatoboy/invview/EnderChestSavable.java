package us.potatoboy.invview;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;

public class EnderChestSavable extends EnderChestInventory {
    private ServerPlayerEntity target;
    private EnderChestInventory enderChestInventory;

    public EnderChestSavable(ServerPlayerEntity target) {
        super();
        this.target = target;
        this.enderChestInventory = target.getEnderChestInventory();
    }

    @Override
    public void onClose(PlayerEntity player) {
        InvView.SavePlayerData(target);
    }

    @Override
    public void readTags(ListTag tags) {
        enderChestInventory.readTags(tags);
    }

    @Override
    public ItemStack getStack(int slot) {
        return enderChestInventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return enderChestInventory.removeStack(slot, amount);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        enderChestInventory.setStack(slot, stack);
    }
}
