package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record TrinketsLayout(List<TrinketSlotAccess> regular, List<TrinketSlotAccess> cosmetic) {
    TrinketsLayout {
        regular = List.copyOf(regular);
        cosmetic = List.copyOf(cosmetic);
    }

    static TrinketsLayout read(TrinketAttachment attachment) {
        var inventories = new ArrayList<>(attachment.getInventories().values());
        inventories.sort(Comparator
                .comparingInt((TrinketInventory inv) -> {
                    var group = attachment.getGroups().get(inv.slotType().group());
                    return group != null ? group.order() : 0;
                })
                .thenComparing(inv -> inv.slotType().group())
                .thenComparingInt(inv -> inv.slotType().order())
                .thenComparing(inv -> inv.slotType().getId()));

        var regular = new ArrayList<TrinketSlotAccess>();
        var cosmetic = new ArrayList<TrinketSlotAccess>();
        for (var inventory : inventories) {
            int size = inventory.getContainerSize();
            for (int i = 0; i < size; i++) {
                var access = inventory.getSlotAccess(i);
                if (access != null) {
                    regular.add(access);
                }
                var cosmeticAccess = inventory.getCosmeticSlotAccess(i);
                if (cosmeticAccess != null) {
                    cosmetic.add(cosmeticAccess);
                }
            }
        }
        return new TrinketsLayout(regular, cosmetic);
    }

    List<TrinketSlotAccess> slots(TrinketsMode mode) {
        return mode.isCosmetic() ? cosmetic : regular;
    }

    int pageCount(TrinketsMode mode, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        var slots = slots(mode);
        return (slots.size() + pageSize - 1) / pageSize;
    }

    List<TrinketSlotAccess> page(TrinketsMode mode, int page, int pageSize) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        var slots = slots(mode);
        int start = page * pageSize;
        if (start >= slots.size()) {
            return List.of();
        }
        return slots.subList(start, Math.min(start + pageSize, slots.size()));
    }

    boolean sameBindings(TrinketsLayout other) {
        return sameSlots(regular, other.regular) && sameSlots(cosmetic, other.cosmetic);
    }

    private static boolean sameSlots(List<TrinketSlotAccess> first, List<TrinketSlotAccess> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            var a = first.get(i);
            var b = second.get(i);
            // Trinkets inventory equality compares types, not inventory lifetimes.
            if (a.inventory() != b.inventory() || a.index() != b.index() || a.cosmetic() != b.cosmetic()) {
                return false;
            }
        }
        return true;
    }
}
