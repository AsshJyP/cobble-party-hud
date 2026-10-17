package io.github.asshjyp.cobblepartyhud.platform;

import java.nio.file.Path;

/** Lo único que Party HUD necesita del loader. */
public interface IPlatformHelper {
	/** "Fabric" o "NeoForge" (solo para el log). */
	String getPlatformName();

	/** La carpeta config/ de la instancia. */
	Path getConfigDir();

	/** ¿Está cargado el mod con este id? */
	boolean isModLoaded(String modId);
}
