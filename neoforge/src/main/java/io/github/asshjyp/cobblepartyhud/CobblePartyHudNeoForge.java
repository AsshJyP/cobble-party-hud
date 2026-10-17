package io.github.asshjyp.cobblepartyhud;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Arranque en NeoForge (solo cliente): tecla en el bus del mod, tick en el bus del juego y botón "Config" de la lista
 * de mods; el resto está en common (CobblePartyHud).
 */
@Mod(value = CobblePartyHud.MOD_ID, dist = Dist.CLIENT)
public class CobblePartyHudNeoForge {
	public CobblePartyHudNeoForge(IEventBus modBus, ModContainer container) {
		modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(CobblePartyHud.EDIT_HUD));
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> CobblePartyHud.onClientTick(Minecraft.getInstance()));
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new HudEditScreen(parent));
		CobblePartyHud.init();
	}
}
