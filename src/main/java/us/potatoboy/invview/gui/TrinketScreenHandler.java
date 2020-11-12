package us.potatoboy.invview.gui;

import com.google.common.collect.Lists;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.InvView;

import java.util.List;

public class TrinketScreenHandler extends ScreenHandler {
    private final TrinketComponent trinketComponent;
    private final ServerPlayerEntity viewedPlayer;
    private final PlayerInventory playerInventory;
    private final List<ScreenHandlerListener> listeners = Lists.newArrayList();

    public TrinketScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity viewedPlayer) {
        super(ScreenHandlerType.GENERIC_9X2, syncId);
        this.playerInventory = playerInventory;
        trinketComponent = TrinketsApi.getTrinketComponent(viewedPlayer);
        this.viewedPlayer = viewedPlayer;

        int rows = 2;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(trinketComponent.getInventory(), m + n * 9, 8 + m * 18, 18 + n * 18));
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
    public void addListener(ScreenHandlerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            listener.onHandlerRegistered(this, getStacks());
            sendContentUpdates();
        }
    }

    @Override
    public void sendContentUpdates() {
        for (int j = 0; j < slots.size(); ++j) {
            ItemStack itemStack = slots.get(j).getStack();

            for (ScreenHandlerListener screenHandlerListener: listeners) {
                screenHandlerListener.onSlotUpdate(this, j, itemStack.copy());
            }
        }
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        if (!(player instanceof ServerPlayerEntity))
            return ItemStack.EMPTY;

        this.sendContentUpdates();
        return playerInventory.getCursorStack();
    }

    @Override
    public ItemStack onSlotClick(int i, int j, SlotActionType actionType, PlayerEntity playerEntity) {
        if (actionType == SlotActionType.QUICK_MOVE) return playerInventory.getCursorStack();

        sendContentUpdates();

        if (i < 0)
            return ItemStack.EMPTY;

        if (i < TrinketSlots.getSlotCount()) {
            return super.onSlotClick(i, j, actionType, playerEntity);
        } else if (i > 17) {
            return super.onSlotClick(i, j, actionType, playerEntity);
        } else {
            return playerInventory.getCursorStack();
        }
    }

    @Override
    public void close(PlayerEntity player) {
        InvView.SavePlayerData(viewedPlayer);
        super.close(player);
    }
}
