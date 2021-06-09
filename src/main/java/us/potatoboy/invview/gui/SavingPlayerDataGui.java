package us.potatoboy.invview.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import us.potatoboy.invview.InvView;

public class SavingPlayerDataGui extends SimpleGui {
    private final ServerPlayerEntity savedPlayer;

    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param type                        the screen handler that the client should display
     * @param player                      the player to server this gui to
     */
    public SavingPlayerDataGui(ScreenHandlerType<?> type, ServerPlayerEntity player, ServerPlayerEntity savedPlayer) {
        super(type, player, false);
        this.savedPlayer = savedPlayer;
    }

    @Override
    public void onClose() {
        InvView.savePlayerData(savedPlayer);
    }
}
