package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TrinketsSlotTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ItemStack stack(Item item, int count) {
        // Fixture-local components, without rebinding global registry entries.
        var components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        return new ItemStack(Holder.direct(item, components), count);
    }

    @Test
    void cosmeticRemovalConservesItemsAndLeavesRegularStorageUntouched() {
        var storage = new Storage();
        storage.regular = stack(Items.DIAMOND, 5);
        storage.cosmetic = stack(Items.EMERALD, 4);
        var slot = storage.slot(true, false);

        var removed = slot.remove(3);

        assertSame(Items.EMERALD, removed.getItem());
        assertEquals(3, removed.getCount());
        assertEquals(1, storage.cosmetic.getCount());
        assertEquals(5, storage.regular.getCount());
        assertSame(Items.DIAMOND, storage.regular.getItem());
        assertTrue(storage.changes > 0);
        assertEquals(1, slot.remove(20).getCount());
        assertTrue(storage.cosmetic.isEmpty());
    }

    @Test
    void insertionRespectsSlotLimitAndSelectsCosmeticStorage() {
        var storage = new Storage();
        var slot = storage.slot(true, false);
        var input = stack(Items.DIAMOND, 5);

        slot.safeInsert(input, input.getCount());

        assertEquals(2, storage.cosmetic.getCount());
        assertEquals(3, input.getCount());
        assertTrue(storage.regular.isEmpty());
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // These slot guards do not inspect the player argument.
    void readOnlyDoesNotExposeMutableStacksOrAcceptAnyWrites() {
        var storage = new Storage();
        storage.cosmetic = stack(Items.EMERALD, 2);
        var slot = storage.slot(true, true);
        slot.getItem().shrink(1);
        assertFalse(slot.mayPlace(stack(Items.DIAMOND, 1)));
        assertFalse(slot.mayPickup(null));
        assertFalse(slot.allowModification(null));
        assertTrue(slot.remove(2).isEmpty());
        var input = stack(Items.DIAMOND, 1);
        assertSame(input, slot.safeInsert(input, 1));
        slot.set(input);
        slot.setByPlayer(input);
        slot.setByPlayer(input, ItemStack.EMPTY);
        slot.setChanged();

        assertSame(Items.EMERALD, storage.cosmetic.getItem());
        assertEquals(2, storage.cosmetic.getCount());
        assertTrue(storage.regular.isEmpty());
        assertEquals(0, storage.changes);
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // The inactive-slot guard runs before the player could be inspected.
    void replacedInventoryCannotBeEditedThroughStaleSlot() {
        var storage = new Storage();
        storage.regular = stack(Items.DIAMOND, 2);
        var slot = storage.slot(false, false);
        storage.current = false;

        assertFalse(slot.isActive());
        assertFalse(slot.mayPlace(stack(Items.EMERALD, 1)));
        assertFalse(slot.mayPickup(null));
        assertTrue(slot.getItem().isEmpty());
        assertTrue(slot.remove(1).isEmpty());
        slot.set(ItemStack.EMPTY);
        assertEquals(2, storage.regular.getCount());
    }

    @Test
    void oldGuiBindingCannotWriteEvenWhenInventoryIsStillCurrent() {
        var storage = new Storage();
        storage.regular = stack(Items.DIAMOND, 2);
        var oldSlot = storage.slot(false, false);
        oldSlot.unbind();
        var newSlot = storage.slot(true, false);

        assertTrue(newSlot.isActive());
        assertFalse(oldSlot.isActive());
        assertFalse(oldSlot.mayPlace(stack(Items.EMERALD, 1)));
        assertTrue(oldSlot.remove(2).isEmpty());
        oldSlot.setByPlayer(stack(Items.EMERALD, 1));
        var input = stack(Items.EMERALD, 1);
        assertSame(input, oldSlot.safeInsert(input, 1));
        assertEquals(2, storage.regular.getCount());
        assertSame(Items.DIAMOND, storage.regular.getItem());
        newSlot.set(input);
        assertSame(Items.EMERALD, storage.cosmetic.getItem());
    }

    @Test
    void changeListenerRunsOnlyForActiveWritableSlot() {
        var storage = new Storage();
        var notifications = new AtomicInteger();
        var slot = new TrinketsSlot(new TrinketSlotAccess(storage.inventory, 0), notifications::incrementAndGet);

        slot.set(stack(Items.DIAMOND, 1));
        assertEquals(1, notifications.get());

        slot.unbind();
        slot.setChanged();
        assertEquals(1, notifications.get());
    }

    // Model independent regular/cosmetic storage without a running Minecraft entity.
    private static final class Storage {
        ItemStack regular = ItemStack.EMPTY;
        ItemStack cosmetic = ItemStack.EMPTY;
        boolean current = true;
        int changes;
        final SlotType type = proxy(SlotType.class, (name, args) -> {
            if (name.equals("getId")) return "hand/ring";
            throw new UnsupportedOperationException(name);
        });
        final TrinketAttachment attachment = proxy(TrinketAttachment.class, (name, args) -> {
            if (name.equals("getInventory")) return current ? inventory() : null;
            throw new UnsupportedOperationException(name);
        });
        final TrinketInventory inventory = proxy(TrinketInventory.class, (name, args) -> switch (name) {
            case "slotType" -> type;
            case "getAttachment" -> attachment;
            case "isValidSlot" -> (int) args[0] == 0;
            case "hasCosmeticItems" -> true;
            case "getItem" -> regular;
            case "getCosmeticItem" -> cosmetic;
            case "getMaxStackSize" -> 2;
            case "setItem" -> { regular = (ItemStack) args[1]; yield null; }
            case "setCosmeticItem" -> { cosmetic = (ItemStack) args[1]; yield true; }
            case "setChanged" -> { changes++; yield null; }
            default -> throw new UnsupportedOperationException(name);
        });

        private TrinketInventory inventory() {
            return inventory;
        }

        TrinketsSlot slot(boolean cosmetic, boolean readOnly) {
            var access = new TrinketSlotAccess(inventory, 0, cosmetic);
            return readOnly ? new ReadOnlyTrinketsSlot(access) : new TrinketsSlot(access);
        }
    }

    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args)));
    }

}
