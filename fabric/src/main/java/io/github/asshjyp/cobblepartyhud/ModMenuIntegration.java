package io.github.asshjyp.cobblepartyhud;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Botón de configuración en Mod Menu: abre "Mover y escalar HUD". Solo se carga si Mod Menu está instalado. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HudEditScreen::new;
	}
}
