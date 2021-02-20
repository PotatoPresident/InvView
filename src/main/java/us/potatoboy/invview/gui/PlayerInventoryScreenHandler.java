package us.potatoboy.invview.gui;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.InvView;

import java.util.List;

public class PlayerInventoryScreenHandler extends ScreenHandler {
    private final ServerPlayerEntity player;
    private final PlayerInventory viewInventory;
    private final List<ScreenHandlerListener> listeners = Lists.newArrayList();

    public PlayerInventoryScreenHandler(int syncId, ServerPlayerEntity player, PlayerInventory viewInventory) {
        super(ScreenHandlerType.GENERIC_9X5, syncId);
        this.player = player;
        this.viewInventory = viewInventory;
        PlayerInventory playerInventory = player.inventory;

        int rows = 5;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(viewInventory, m + n * 9, 8 + m * 18, 18 + n * 18));
            }
        }

        for (n = 0; n < 3; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 103 + n * 18 + i));
            }
        }

        for (n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 161 + i));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        this.resendInventory();
        return new ItemStack(Items.OAK_PLANKS);
    }

    @Override
    public ItemStack onSlotClick(int i, int j, SlotActionType actionType, PlayerEntity playerEntity) {
        if (i > 40 && i < 45) {
            resendInventory();
            return ItemStack.EMPTY;
        }
        
        return super.onSlotClick(i, j, actionType, playerEntity);
    }

    @Override
    public void close(PlayerEntity player) {
        InvView.SavePlayerData((ServerPlayerEntity) viewInventory.player);
        super.close(player);
    }

    private void resendInventory() {
        player.onHandlerRegistered(this, this.getStacks());
    }
}
