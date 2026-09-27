package us.potatoboy.invview.gui;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

/**
 * Validates a container click before SGUI applies the client's predicted state.
 */
public interface ContainerClickValidator {
    boolean acceptPacket(ServerboundContainerClickPacket packet);
}
