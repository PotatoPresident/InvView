package us.potatoboy.invview.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.InvView;

public class InventoryPowerScreenHandler extends Generic3x3ContainerScreenHandler {
	private final ServerPlayerEntity viewedPlayer;

	public InventoryPowerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ServerPlayerEntity viewedPlayer) {
		super(syncId, playerInventory, inventory);
		this.viewedPlayer = viewedPlayer;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void close(PlayerEntity player) {
		InvView.SavePlayerData(this.viewedPlayer);
		super.close(player);
	}
}
