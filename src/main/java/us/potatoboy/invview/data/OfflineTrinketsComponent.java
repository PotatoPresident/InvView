package us.potatoboy.invview.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class OfflineTrinketsComponent implements TrinketComponent {
    private final Map<String, Map<String, TrinketInventory>> inventories = new HashMap<>();
    private Map<String, SlotGroup> groups = new HashMap<>();
    private final EntityType<?> entityType;

    public OfflineTrinketsComponent(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    private void initInventories() {
        inventories.clear();

        groups = TrinketsApi.getEntitySlots(entityType);
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

    public void writeToNbt(NbtCompound tag) {
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
    }

    @Override
    public LivingEntity getEntity() {
        throw new UnsupportedOperationException("OfflineTrinketsComponent doesn't have any entity!");
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return groups;
    }

    public Map<String, Map<String, TrinketInventory>> getInventory() {
        return inventories;
    }

    @Override
    public void update() {

    }

    private Pair<String, String> splitSlotType(String whole) {
        int firstSlash = whole.indexOf('/');
        return new Pair<>(whole.substring(0, firstSlash), whole.substring(firstSlash));
    }

    @Override
    public void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
            Pair<String, String> slotType = splitSlotType(entry.getKey());
            Map<String, TrinketInventory> groupMap = inventories.get(slotType.getLeft());
            if (groupMap == null) continue;
            TrinketInventory inv = groupMap.get(slotType.getRight());
            if (inv == null) continue;
            for (EntityAttributeModifier modifier : entry.getValue())
                inv.addModifier(modifier);
        }
    }

    @Override
    public void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
            Pair<String, String> slotType = splitSlotType(entry.getKey());
            Map<String, TrinketInventory> groupMap = inventories.get(slotType.getLeft());
            if (groupMap == null) continue;
            TrinketInventory inv = groupMap.get(slotType.getRight());
            if (inv == null) continue;
            for (EntityAttributeModifier modifier : entry.getValue())
                inv.addPersistentModifier(modifier);
        }
    }

    @Override
    public void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
            Pair<String, String> slotType = splitSlotType(entry.getKey());
            Map<String, TrinketInventory> groupMap = inventories.get(slotType.getLeft());
            if (groupMap == null) continue;
            TrinketInventory inv = groupMap.get(slotType.getRight());
            if (inv == null) continue;
            for (EntityAttributeModifier modifier : entry.getValue())
                inv.removeModifier(modifier.getId());
        }
    }

    @Override
    public void clearModifiers() {
        for (Map<String, TrinketInventory> groupMap : inventories.values()) {
            for (TrinketInventory inv : groupMap.values()) {
                inv.clearModifiers();
            }
        }
    }

    @Override
    public Multimap<String, EntityAttributeModifier> getModifiers() {
        Multimap<String, EntityAttributeModifier> modifierMap = HashMultimap.create();
        for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : inventories.entrySet()) {
            for (Map.Entry<String, TrinketInventory> typeEntry : groupEntry.getValue().entrySet()) {
                modifierMap.putAll(groupEntry.getKey() + "/" + typeEntry.getKey(), typeEntry.getValue().getModifiers().values());
            }
        }
        return modifierMap;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (Map<String, TrinketInventory> groupMap : inventories.values()) {
            for (TrinketInventory inv : groupMap.values()) {
                for (int i = 0; i < inv.size(); i++) {
                    if (predicate.test(inv.getStack(i))) return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        ArrayList<Pair<SlotReference, ItemStack>> equipped = new ArrayList<>();
        for (Map<String, TrinketInventory> groupMap : inventories.values()) {
            for (TrinketInventory inv : groupMap.values()) {
                for (int i = 0; i < inv.size(); i++) {
                    if (predicate.test(inv.getStack(i)))
                        equipped.add(new Pair<>(new SlotReference(inv, i), inv.getStack(i)));
                }
            }
        }
        return equipped;
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
        for (Map<String, TrinketInventory> groupMap : inventories.values()) {
            for (TrinketInventory inv : groupMap.values()) {
                for (int i = 0; i < inv.size(); i++) {
                        consumer.accept(new SlotReference(inv, i), inv.getStack(i));
                }
            }
        }
    }

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return Collections.emptySet();
    }

    @Override
    public void clearCachedModifiers() {
        for (Map<String, TrinketInventory> groupMap : inventories.values()) {
            for (TrinketInventory inv : groupMap.values()) {
                inv.clearCachedModifiers();
            }
        }
    }
}
