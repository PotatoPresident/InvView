package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

class TrinketsSlot extends Slot {
    private final TrinketSlotAccess access;
    private final Runnable changeListener;
    private boolean bound = true;

    TrinketsSlot(TrinketSlotAccess access) {
        this(access, () -> {
        });
    }

    TrinketsSlot(TrinketSlotAccess access, Runnable changeListener) {
        super(access.inventory(), access.index(), 0, 0);
        this.access = access;
        this.changeListener = changeListener;
    }

    @Override
    public boolean isActive() {
        return bound && access.inventory().getAttachment().getInventory(access.slotType().getId()) == access.inventory()
                && access.isValid();
    }

    void unbind() {
        bound = false;
    }

    void bindToMenuIndex(int menuIndex) {
        index = menuIndex;
    }

    @Override
    public void setChanged() {
        if (isActive()) {
            super.setChanged();
            changeListener.run();
        }
    }

    @Override
    public @NonNull ItemStack getItem() {
        return isActive() ? access.get() : ItemStack.EMPTY;
    }

    @Override
    public void set(@NonNull ItemStack stack) {
        if (isActive() && access.set(stack)) {
            setChanged();
        }
    }

    @Override
    public @NonNull ItemStack remove(int amount) {
        if (!isActive() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        // SlotAccess selects the regular or cosmetic storage; Container.removeItem does not.
        ItemStack remaining = access.get();
        ItemStack removed = remaining.split(amount);
        set(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        return removed;
    }

    @Override
    public boolean mayPlace(@NonNull ItemStack stack) {
        // Like /view inv, this is an administrative editor, not an equip action.
        return isActive();
    }

    @Override
    public boolean mayPickup(@NonNull Player player) {
        return isActive();
    }
}
