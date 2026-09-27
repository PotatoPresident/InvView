package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

final class ReadOnlyTrinketsSlot extends TrinketsSlot {
    ReadOnlyTrinketsSlot(TrinketSlotAccess access) {
        super(access);
    }

    @Override
    public @NonNull ItemStack getItem() {
        return super.getItem().copy();
    }

    @Override
    public boolean mayPlace(@NonNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(@NonNull Player player) {
        return false;
    }

    @Override
    public boolean allowModification(@NonNull Player player) {
        return false;
    }

    @Override
    public @NonNull ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack safeInsert(@NonNull ItemStack stack, int count) {
        return stack;
    }

    @Override
    public void setByPlayer(@NonNull ItemStack stack) {
    }

    @Override
    public void setByPlayer(@NonNull ItemStack stack, @NonNull ItemStack previous) {
    }

    @Override
    public void set(@NonNull ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }
}
