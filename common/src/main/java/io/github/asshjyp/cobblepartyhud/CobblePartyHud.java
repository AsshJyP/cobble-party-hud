package io.github.asshjyp.cobblepartyhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lo común a Fabric y NeoForge. Cada loader registra EDIT_HUD a su manera (KeyBindingHelper / RegisterKeyMappingsEvent)
 * y llama a onClientTick al final de cada tick del cliente (ClientTickEvents.END_CLIENT_TICK / ClientTickEvent.Post).
 */
public final class CobblePartyHud {
	public static final String MOD_ID = "cobblepartyhud";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final PartyHudConfig CONFIG = PartyHudConfig.load();

	/** Abre "Mover y escalar HUD". Y por defecto (libre en el pack; J, U, O, P, K, N y H están ocupadas). */
	public static final KeyMapping EDIT_HUD = new KeyMapping(
			"key.cobblepartyhud.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.cobblepartyhud");

	private CobblePartyHud() {}

	/** Al arrancar el cliente, después de registrar la tecla. */
	public static void init() {
		LOGGER.info("Cobble Party HUD cargado ({})", Services.PLATFORM.getPlatformName());
	}

	/** Al final de cada tick del cliente. consumeClick solo da true jugando (sin otra pantalla abierta). */
	public static void onClientTick(Minecraft client) {
		while (EDIT_HUD.consumeClick()) {
			client.setScreen(new HudEditScreen(null));
		}
	}
}
