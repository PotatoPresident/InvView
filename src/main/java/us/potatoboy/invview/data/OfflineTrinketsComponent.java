package us.potatoboy.invview.data;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;

public class OfflineTrinketsComponent {
    private final Map<String, Map<String, TrinketInventory>> inventories = new HashMap<>();
    private final EntityType<?> entityType;

    public OfflineTrinketsComponent(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    private void initInventories() {
        inventories.clear();

        Map<String, SlotGroup> groups = TrinketsApi.getEntitySlots(entityType);
        for (Map.Entry<String, SlotGroup> groupEntry : groups.entrySet()) {
            Map<String, TrinketInventory> groupMap = new HashMap<>();
            inventories.put(groupEntry.getKey(), groupMap);
            for (Map.Entry<String, SlotType> typeEntry : groupEntry.getValue().getSlots().entrySet()) {
                TrinketInventory inv = new OfflineTrinketInventory(typeEntry.getValue());
                groupMap.put(typeEntry.getKey(), inv);
            }
        }
    }

    public void readFromNbt(NbtCompound nbt) {
        initInventories();

        for (String groupId : nbt.getKeys()) {
            if (inventories.get(groupId) == null) continue;

            NbtCompound groupTag = nbt.getCompound(groupId);
            for (String typeId : groupTag.getKeys()) {
                if (inventories.get(groupId).get(typeId) == null) continue;

                NbtCompound typeTag = groupTag.getCompound(typeId);
                TrinketInventory inv = inventories.get(groupId).get(typeId);
                inv.fromTag(typeTag.getCompound("Metadata"));
                NbtList itemsTag = typeTag.getList("Items", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < itemsTag.size(); i++) {
                    ItemStack stack = ItemStack.fromNbt(itemsTag.getCompound(i));
                    if (i < inv.size())
                        inv.setStack(i, stack);
                }
            }
        }
    }

    public NbtCompound writeToNbt(NbtCompound tag) {
        for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : inventories.entrySet()) {
            NbtCompound groupTag = new NbtCompound();
            tag.put(groupEntry.getKey(), groupTag);
            for (Map.Entry<String, TrinketInventory> typeEntry : groupEntry.getValue().entrySet()) {
                TrinketInventory inv = typeEntry.getValue();
                NbtCompound typeTag = new NbtCompound();
                groupTag.put(typeEntry.getKey(), typeTag);
                typeTag.put("Metadata", inv.toTag());
                NbtList itemsTag = new NbtList();
                for (int i = 0; i < inv.size(); i++) {
                    NbtCompound stackNbt = new NbtCompound();
                    inv.getStack(i).writeNbt(stackNbt);
                    itemsTag.add(stackNbt);
                }
                typeTag.put("Items", itemsTag);
            }
        }
        return tag;
    }

    public Map<String, Map<String, TrinketInventory>> getInventory() {
        return inventories;
    }
}
