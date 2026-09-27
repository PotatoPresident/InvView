package us.potatoboy.invview.mixin;

import eu.pb4.sgui.api.containerwrappers.SlotBasedWrapperMenu;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.potatoboy.invview.gui.ContainerClickValidator;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ContainerClickValidationMixin {
    @Shadow
    public ServerPlayer player;

    // After the vanilla thread check, but before SGUI's AFTER resetLastActionTime
    // injection. Do not let SGUI or vanilla apply predictions for an obsolete layout.
    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
    private void invview$validateContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (player.containerMenu instanceof SlotBasedWrapperMenu menu
                && menu.getBackingGui() instanceof ContainerClickValidator validator
                && !validator.acceptPacket(packet)) {
            ci.cancel();
        }
    }
}
