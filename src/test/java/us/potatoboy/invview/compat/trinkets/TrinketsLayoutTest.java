package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class TrinketsLayoutTest {
    @Test
    void detectsReplacementReorderingAndCosmeticChanges() {
        var first = layoutInventory("hand", "hand/first", 0, 1, false);
        var second = layoutInventory("hand", "hand/second", 1, 1, false);
        var a = new TrinketSlotAccess(first, 0);
        var b = new TrinketSlotAccess(second, 0);
        var layout = new TrinketsLayout(List.of(a, b), List.of());

        assertTrue(layout.sameBindings(new TrinketsLayout(
                List.of(new TrinketSlotAccess(first, 0), b), List.of())));
        assertFalse(layout.sameBindings(new TrinketsLayout(List.of(b, a), List.of())));
        assertFalse(layout.sameBindings(new TrinketsLayout(List.of(a), List.of())));
        assertFalse(layout.sameBindings(new TrinketsLayout(List.of(a, b),
                List.of(new TrinketSlotAccess(first, 0, true)))));
        assertFalse(new TrinketsLayout(List.of(a), List.of())
                .sameBindings(new TrinketsLayout(List.of(b), List.of())));
    }

    @Test
    void paginatesRegularAndCosmeticSlotsIndependently() {
        var inventory = layoutInventory("hand", "hand/ring", 0, 19, true);
        var regular = new ArrayList<TrinketSlotAccess>();
        var cosmetic = new ArrayList<TrinketSlotAccess>();
        for (int i = 0; i < 19; i++) regular.add(new TrinketSlotAccess(inventory, i));
        for (int i = 0; i < 5; i++) cosmetic.add(new TrinketSlotAccess(inventory, i, true));
        var layout = new TrinketsLayout(regular, cosmetic);

        assertEquals(2, layout.pageCount(TrinketsMode.REGULAR, 18));
        assertEquals(1, layout.pageCount(TrinketsMode.COSMETIC, 18));
        assertEquals(18, layout.page(TrinketsMode.REGULAR, 0, 18).size());
        assertSame(regular.get(18), layout.page(TrinketsMode.REGULAR, 1, 18).getFirst());
        assertEquals(cosmetic, layout.page(TrinketsMode.COSMETIC, 0, 18));
        assertTrue(layout.page(TrinketsMode.REGULAR, 2, 18).isEmpty());
    }

    @Test
    void rejectsInvalidPaginationArguments() {
        var layout = new TrinketsLayout(List.of(), List.of());

        assertThrows(IllegalArgumentException.class,
                () -> layout.pageCount(TrinketsMode.REGULAR, 0));
        assertThrows(IllegalArgumentException.class,
                () -> layout.page(TrinketsMode.REGULAR, -1, 18));
        assertThrows(IllegalArgumentException.class,
                () -> layout.page(TrinketsMode.REGULAR, 0, 0));
    }

    @Test
    void readsActiveInventoriesInGroupTypeAndIndexOrder() {
        var hat = layoutInventory("head", "head/hat", -512, 1, false);
        var face = layoutInventory("head", "head/face", -1024, 1, true);
        var rings = layoutInventory("hand", "hand/ring", -512, 2, true);
        var attachment = proxy(TrinketAttachment.class, (name, args) -> switch (name) {
            case "getInventories" -> Map.of("hand/ring", rings, "head/hat", hat, "head/face", face);
            case "getGroups" -> Map.of("hand", slotGroup("hand", -1200), "head", slotGroup("head", -1400));
            default -> throw new UnsupportedOperationException(name);
        });

        var layout = TrinketsLayout.read(attachment);

        assertEquals(List.of("head/face@0", "head/hat@0", "hand/ring@0", "hand/ring@1"),
                layout.regular().stream().map(TrinketSlotAccess::getSerializedName).toList());
        assertEquals(List.of("head/face@0?cosmetic", "hand/ring@0?cosmetic", "hand/ring@1?cosmetic"),
                layout.cosmetic().stream().map(TrinketSlotAccess::getSerializedName).toList());
    }

    @Test
    void pageStateNavigatesAndResetsWhenModeChanges() {
        var inventory = layoutInventory("hand", "hand/ring", 0, 19, true);
        var regular = new ArrayList<TrinketSlotAccess>();
        for (int i = 0; i < 19; i++) regular.add(new TrinketSlotAccess(inventory, i));
        var cosmetic = List.of(new TrinketSlotAccess(inventory, 0, true));
        var layout = new TrinketsLayout(regular, cosmetic);
        var state = new TrinketsPageState();

        assertTrue(state.next(layout, 18));
        assertEquals(1, state.page());
        assertFalse(state.next(layout, 18));
        assertTrue(state.previous(layout, 18));
        state.next(layout, 18);
        state.toggleMode(layout, 18);
        assertEquals(TrinketsMode.COSMETIC, state.mode());
        assertEquals(0, state.page());
        assertEquals(cosmetic, state.visibleSlots(layout, 18));
    }

    private static SlotGroup slotGroup(String group, int order) {
        return proxy(SlotGroup.class, (name, args) -> switch (name) {
            case "name" -> group;
            case "order" -> order;
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private static TrinketInventory layoutInventory(String group, String id, int order, int size,
            boolean cosmetic) {
        var type = proxy(SlotType.class, (name, args) -> switch (name) {
            case "group" -> group;
            case "getId" -> id;
            case "order" -> order;
            default -> throw new UnsupportedOperationException(name);
        });
        var inventory = new TrinketInventory[1];
        inventory[0] = proxy(TrinketInventory.class, (name, args) -> switch (name) {
            case "slotType" -> type;
            case "getContainerSize" -> size;
            case "getSlotAccess" -> new TrinketSlotAccess(inventory[0], (int) args[0]);
            case "getCosmeticSlotAccess" -> cosmetic
                    ? new TrinketSlotAccess(inventory[0], (int) args[0], true) : null;
            default -> throw new UnsupportedOperationException(name);
        });
        return inventory[0];
    }

    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args)));
    }
}
