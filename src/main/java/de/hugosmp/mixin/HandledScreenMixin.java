package de.hugosmp.mixin;

import de.hugosmp.ItemProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected int x;
    @Shadow protected int y;

    /** Rotes Schloss auf jedem gesperrten Item im Inventar. */
    @Inject(method = "render", at = @At("TAIL"))
    private void hugosmp$locks(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (Slot s : ((HandledScreen<?>) (Object) this).getScreenHandler().slots) {
            if (ItemProtect.isProtected(s.getStack())) ItemProtect.drawLock(context, this.x + s.x, this.y + s.y);
        }
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void hugosmp$protect(Slot slot, int slotId, int button, SlotActionType type, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean block = false;
        if (slot != null && ItemProtect.isProtected(slot.getStack())) block = true;
        if (ItemProtect.isProtected(((HandledScreen<?>) (Object) this).getScreenHandler().getCursorStack())) block = true;
        if (type == SlotActionType.SWAP && button >= 0 && (button < 9 || button == 40)
                && ItemProtect.isProtected(mc.player.getInventory().getStack(button))) block = true;
        if (block) {
            ItemProtect.notifyBlocked();
            ci.cancel();
        }
    }
}
