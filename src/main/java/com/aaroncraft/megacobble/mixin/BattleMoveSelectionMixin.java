package com.aaroncraft.megacobble.mixin;

import com.aaroncraft.megacobble.client.MegaButton;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects the Mega Evolve button into Cobblemon's move-selection ("Fight") subscreen.
 * Rendering and click handling are delegated to {@link MegaButton}; this mixin only wires
 * into the screen's render and click methods. Nothing in Cobblemon's source is modified.
 */
@Mixin(BattleMoveSelection.class)
public class BattleMoveSelectionMixin {

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void megacobble$renderMegaButton(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MegaButton.render((BattleMoveSelection) (Object) this, context, mouseX, mouseY);
    }

    // mousePrimaryClicked is Cobblemon's own method (not a Minecraft method), so it has no
    // obfuscation mapping — remap = false keeps its literal name at runtime.
    @Inject(method = "mousePrimaryClicked(DD)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void megacobble$clickMegaButton(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        BattleMoveSelection selection = (BattleMoveSelection) (Object) this;
        // A click on the Mega button itself arms mega and is consumed.
        if (MegaButton.handleButtonClick(selection, mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        // Otherwise handle move-commit (fire transform) or back-button de-select while armed.
        // We do NOT consume the click so the native move/back handling proceeds normally.
        MegaButton.handleNonButtonClick(selection, mouseX, mouseY);
    }
}
