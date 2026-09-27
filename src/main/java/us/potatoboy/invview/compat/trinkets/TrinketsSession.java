package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class TrinketsSession {
    private static final String PLAYER_CHANGED = "Player state changed. Reopen the trinket inventory.";
    private static final String SLOTS_CHANGED = "Trinket slots changed. Reopen the trinket inventory.";

    private final MinecraftServer server;
    private final ServerPlayer target;
    private final boolean onlineTarget;
    private final TrinketAttachment attachment;

    TrinketsSession(MinecraftServer server, ServerPlayer target) {
        this.server = server;
        this.target = target;
        this.onlineTarget = server.getPlayerList().getPlayer(target.getUUID()) == target;
        this.attachment = TrinketsApi.getAttachment(target);
    }

    ServerPlayer target() {
        return target;
    }

    TrinketsLayout readLayout() {
        return TrinketsLayout.read(attachment);
    }

    boolean targetIsCurrent() {
        var current = server.getPlayerList().getPlayer(target.getUUID());
        return onlineTarget ? current == target && target.isAlive() : current == null;
    }

    String invalidReason(TrinketsLayout expectedLayout) {
        if (!targetIsCurrent()) {
            return PLAYER_CHANGED;
        }
        return expectedLayout.sameBindings(readLayout()) ? null : SLOTS_CHANGED;
    }
}
