package us.potatoboy.invview.data;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;

public class OfflineTrinketInventory extends TrinketInventory {
    public OfflineTrinketInventory(SlotType slotType) {
        super(slotType, null, inv -> {});
    }

    @Override
    public TrinketComponent getComponent() {
        throw new UnsupportedOperationException("OfflineTrinketInventory doesn't have a component!");
    }

    @Override
    public void update() {
    }
}
