package de.hugosmp.mixin;

import de.hugosmp.ItemProtect;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void hugosmp$noDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (ItemProtect.isProtected(((ClientPlayerEntity) (Object) this).getMainHandStack())) {
            ItemProtect.notifyBlocked();
            cir.setReturnValue(false);
        }
    }
}
