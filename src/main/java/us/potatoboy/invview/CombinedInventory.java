package us.potatoboy.invview;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;
import java.util.List;

public class CombinedInventory implements Inventory, Nameable {
    public final List<DefaultedList<ItemStack>> combined;
    private int changeCount;

    public CombinedInventory(DefaultedList<ItemStack> main, DefaultedList<ItemStack> armor, DefaultedList<ItemStack> offhand) {
        this.combined = ImmutableList.of(main, armor, offhand);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public boolean isEmpty() {
        return combined.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        List<ItemStack> list = null;

        DefaultedList defaultedList;
        for(Iterator var3 = combined.iterator(); var3.hasNext(); slot -= defaultedList.size()) {
            defaultedList = (DefaultedList)var3.next();
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }
        }

        return list == null ? ItemStack.EMPTY : (ItemStack)list.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        List<ItemStack> list = null;

        DefaultedList defaultedList;
        for(Iterator var4 = combined.iterator(); var4.hasNext(); slot -= defaultedList.size()) {
            defaultedList = (DefaultedList)var4.next();
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }
        }

        return list != null && !((ItemStack)list.get(slot)).isEmpty() ? Inventories.splitStack(list, slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        DefaultedList<ItemStack> defaultedList = null;

        DefaultedList defaultedList2;
        for(Iterator var3 = combined.iterator(); var3.hasNext(); slot -= defaultedList2.size()) {
            defaultedList2 = (DefaultedList)var3.next();
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
        }

        if (defaultedList != null && !((ItemStack)defaultedList.get(slot)).isEmpty()) {
            ItemStack itemStack = (ItemStack)defaultedList.get(slot);
            defaultedList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        DefaultedList<ItemStack> defaultedList = null;

        DefaultedList defaultedList2;
        for(Iterator var4 = combined.iterator(); var4.hasNext(); slot -= defaultedList2.size()) {
            defaultedList2 = (DefaultedList)var4.next();
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
        }

        if (defaultedList != null) {
            defaultedList.set(slot, stack);
        }

    }

    @Override
    public void markDirty() {
        ++this.changeCount;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        Iterator var1 = combined.iterator();

        while(var1.hasNext()) {
            List<ItemStack> list = (List)var1.next();
            list.clear();
        }

    }

    @Override
    public Text getName() {
        return null;
    }
}
