package io.github.asshjyp.cobblepartyhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblePartyHud implements ClientModInitializer {
	public static final String MOD_ID = "cobblepartyhud";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final PartyHudConfig CONFIG = PartyHudConfig.load();

	/** Abre "Mover y escalar HUD". Y por defecto (libre en el pack; J, U, O, P, K, N y H están ocupadas). */
	private static final KeyMapping EDIT_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.cobblepartyhud.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.cobblepartyhud"));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// consumeClick solo da true jugando (sin otra pantalla abierta)
			while (EDIT_HUD.consumeClick()) {
				client.setScreen(new HudEditScreen(null));
			}
		});
		LOGGER.info("Cobble Party HUD cargado");
	}
}
