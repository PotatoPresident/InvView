package us.potatoboy.invview.mixin;

import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import sun.java2d.pipe.SpanShapeRenderer;

@Mixin(HorseBaseEntity.class)
public interface HorseInventoryAccess {
    @Accessor("items") SimpleInventory getItems();
}
