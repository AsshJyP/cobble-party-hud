package io.github.asshjyp.cobblepartyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/** Arranque en Fabric: registra la tecla y el tick; el resto está en common (CobblePartyHud). */
public class CobblePartyHudFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(CobblePartyHud.EDIT_HUD);
		ClientTickEvents.END_CLIENT_TICK.register(CobblePartyHud::onClientTick);
		CobblePartyHud.init();
	}
}
