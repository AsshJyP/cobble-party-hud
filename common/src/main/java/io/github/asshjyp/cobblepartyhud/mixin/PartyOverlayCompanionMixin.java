package io.github.asshjyp.cobblepartyhud.mixin;

import com.cobblemon.mod.common.client.gui.PartyOverlay;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.asshjyp.cobblepartyhud.SlotColorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * canRender() oculta el HUD de party con cualquier pantalla abierta salvo las de screenExemptions (chat, combate).
 * Aquí se añade la pantalla "Mover y escalar HUD" de cualquier mod de AsshJyP (este, Cobble Hunt Log,
 * Cobble EV Tracker…), para colocar todos los HUD juntos. Solo se cambia esa comprobación: F1, F3 y la tecla
 * de Cobblemon para ocultar el party siguen funcionando.
 */
@Mixin(PartyOverlay.Companion.class)
public class PartyOverlayCompanionMixin {
	@ModifyExpressionValue(method = "canRender", remap = false,
			at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
	private boolean cobblepartyhud$showInHudEditors(boolean exempt) {
		Screen screen = Minecraft.getInstance().screen;
		// La pantalla de colores también: el HUD real hace de vista previa
		return exempt || isHudEditor(screen) || screen instanceof SlotColorScreen;
	}

	/** Convención compartida: clase HudEditScreen en un paquete io.github.asshjyp.* (sin depender de los otros mods). */
	private static boolean isHudEditor(Screen screen) {
		if (screen == null) return false;
		Class<?> type = screen.getClass();
		return type.getSimpleName().equals("HudEditScreen") && type.getName().startsWith("io.github.asshjyp.");
	}
}
