package io.github.asshjyp.cobblepartyhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración en {@code config/cobblepartyhud.json}. Se lee al arrancar Minecraft;
 * si no existe, se crea con los valores por defecto (el HUD en su sitio de Cobblemon).
 */
public final class PartyHudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(CobblePartyHud.MOD_ID + ".json");
	public static final int MAX_OFFSET = 999;
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 2.0f;

	/**
	 * Punto de la pantalla al que se pega el HUD: las 4 esquinas, el centro de cada borde y el centro.
	 * LEFT (centro del borde izquierdo) es el sitio original de Cobblemon.
	 */
	public enum HudAnchor {
		TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
		LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
		BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

		/** 0 = izquierda/arriba, 1 = centro, 2 = derecha/abajo. */
		public final int column;
		public final int row;

		HudAnchor(int column, int row) {
			this.column = column;
			this.row = row;
		}

		public static HudAnchor of(int column, int row) {
			return values()[row * 3 + column];
		}
	}

	/** Cómo se colocan los 6 slots: en columna (como Cobblemon) o en fila. */
	public enum Orientation { VERTICAL, HORIZONTAL }

	// Gson ignora este campo al leer; solo sirve de ayuda a quien edite el archivo
	@SuppressWarnings("unused")
	private String _info = "Lo más cómodo es la pantalla \"Mover y escalar HUD\" (tecla Y o Mod Menu). enabled: false deja el HUD de party como en Cobblemon. hudAnchor: TOP_LEFT, TOP, TOP_RIGHT, LEFT, CENTER, RIGHT, BOTTOM_LEFT, BOTTOM o BOTTOM_RIGHT (LEFT es el sitio original). hudOffsetX/hudOffsetY lo separan de ese punto (en píxeles de la interfaz) y hudScale es su tamaño (0.5 a 2). orientation: VERTICAL (como Cobblemon) u HORIZONTAL (los 6 en fila).";

	public boolean enabled = true;
	public HudAnchor hudAnchor = HudAnchor.LEFT;
	public int hudOffsetX = 0;
	public int hudOffsetY = 0;
	public float hudScale = 1.0f;
	public Orientation orientation = Orientation.VERTICAL;

	public static PartyHudConfig load() {
		PartyHudConfig config = new PartyHudConfig();
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				PartyHudConfig read = GSON.fromJson(reader, PartyHudConfig.class);
				if (read != null) {
					// El texto de ayuda siempre es el de esta versión, no el que hubiera en el archivo
					read._info = config._info;
					config = read;
				}
			} catch (IOException | JsonParseException e) {
				// No se sobrescribe: así no se pierde lo que se haya editado a mano
				CobblePartyHud.LOGGER.warn("No se pudo leer {}; se usan los valores por defecto", PATH, e);
				return config.sanitized();
			}
		}
		config = config.sanitized();
		config.save();
		return config;
	}

	/** Vuelve al sitio, tamaño y orientación originales de Cobblemon. */
	public void reset() {
		orientation = Orientation.VERTICAL;
		hudAnchor = HudAnchor.LEFT;
		hudOffsetX = 0;
		hudOffsetY = 0;
		hudScale = 1.0f;
	}

	/** Corrige enums desconocidos (Gson los deja en null) y valores fuera de rango. */
	private PartyHudConfig sanitized() {
		if (hudAnchor == null) hudAnchor = HudAnchor.LEFT;
		if (orientation == null) orientation = Orientation.VERTICAL;
		hudOffsetX = Math.clamp(hudOffsetX, -MAX_OFFSET, MAX_OFFSET);
		hudOffsetY = Math.clamp(hudOffsetY, -MAX_OFFSET, MAX_OFFSET);
		hudScale = Float.isNaN(hudScale) ? 1.0f : Math.clamp(hudScale, MIN_SCALE, MAX_SCALE);
		return this;
	}

	/** Guarda el archivo (crea los campos nuevos si faltaban). */
	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			CobblePartyHud.LOGGER.warn("No se pudo guardar {}", PATH, e);
		}
	}
}
