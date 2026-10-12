package io.github.asshjyp.cobblepartyhud.mixin;

import com.cobblemon.mod.common.client.gui.PartyOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.asshjyp.cobblepartyhud.PartyHudTransform;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Envuelve el dibujo del HUD de party de Cobblemon con nuestra transformación.
 * HEAD/RETURN quedan por fuera del WrapOperation de Diosesmon (PartyOverlayScaleMixin), así que su ajuste de escala
 * se dibuja dentro del nuestro. Nunca tocamos Window.setGuiScale.
 * Los ganchos de dibujo (blitk, drawScaledText, renderScaledGuiItemIcon) solo actúan dentro de este render.
 */
@Mixin(PartyOverlay.class)
public class PartyOverlayMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void cobblepartyhud$begin(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		PartyHudTransform.begin(graphics);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void cobblepartyhud$end(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		PartyHudTransform.end(graphics);
	}

	/** Orientación horizontal: local 24 = 34·i, la altura de cada slot (se guarda dos veces por slot con Pokémon). */
	@ModifyVariable(method = "render", at = @At("STORE"), index = 24)
	private int cobblepartyhud$slotOffset(int offsetY) {
		return PartyHudTransform.slotOffset(offsetY);
	}

	/** El recorte del retrato no pasa por pose(): hay que moverlo a mano o el Pokémon desaparece. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;enableScissor(IIII)V"))
	private void cobblepartyhud$scissor(GuiGraphics graphics, int x1, int y1, int x2, int y2, Operation<Void> original) {
		PartyHudTransform.scissor(graphics, x1, y1, x2, y2, original);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V"))
	private void cobblepartyhud$disableScissor(GuiGraphics graphics, Operation<Void> original) {
		PartyHudTransform.disableScissor(graphics, original);
	}

	/** Texturas e iconos. mask = parámetros por defecto de Kotlin (bit 16 = scale, que entonces vale 1). */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/cobblemon/mod/common/api/gui/GuiUtilsKt;blitk$default(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;ZFILjava/lang/Object;)V"))
	private void cobblepartyhud$blit(PoseStack pose, ResourceLocation texture, Number x, Number y, Number height, Number width,
			Number u, Number v, Number textureWidth, Number textureHeight, Number blitOffset,
			Number red, Number green, Number blue, Number alpha, boolean blend, float scale, int mask, Object marker,
			Operation<Void> original) {
		float blitScale = (mask & (1 << 16)) != 0 ? 1f : scale;
		PartyHudTransform.blit(pose, texture, x == null ? 0 : x.floatValue(), y == null ? 0 : y.floatValue(),
				width == null ? 0 : width.floatValue(), blitScale,
				() -> original.call(pose, texture, x, y, height, width, u, v, textureWidth, textureHeight, blitOffset,
						red, green, blue, alpha, blend, scale, mask, marker));
	}

	/** Nivel y nombre. Bits de mask: 1 = fuente, 5 = escala, 9 = centrado. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText$default(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/chat/MutableComponent;Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;ILjava/lang/Object;)V"))
	private void cobblepartyhud$text(GuiGraphics graphics, ResourceLocation font, MutableComponent text, Number x, Number y,
			float scale, Number opacity, int maxCharacterWidth, int colour, boolean centered, boolean shadow,
			Integer mouseX, Integer mouseY, int mask, Object marker, Operation<Void> original) {
		PartyHudTransform.text(graphics.pose(), text, (mask & (1 << 1)) != 0 ? null : font,
				x == null ? 0 : x.floatValue(), (mask & (1 << 5)) != 0 ? 1f : scale,
				(mask & (1 << 9)) == 0 && centered,
				() -> original.call(graphics, font, text, x, y, scale, opacity, maxCharacterWidth, colour, centered, shadow,
						mouseX, mouseY, mask, marker));
	}

	/** Objeto equipado. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;renderScaledGuiItemIcon(Lnet/minecraft/world/item/ItemStack;DDDFLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void cobblepartyhud$item(ItemStack stack, double x, double y, double scale, float z, PoseStack pose,
			Operation<Void> original) {
		PartyHudTransform.item(pose, (float) x, (float) scale, () -> original.call(stack, x, y, scale, z, pose));
	}
}
