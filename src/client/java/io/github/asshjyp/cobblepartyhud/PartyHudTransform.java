package io.github.asshjyp.cobblepartyhud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Dónde y a qué tamaño se dibuja el HUD de party. Cobblemon lo dibuja en su caja original
 * ({@link #VANILLA_WIDTH}×{@link #VANILLA_HEIGHT}, ver {@link #vanillaTop}); aquí se calcula la transformación
 * x → scale·x + t (en píxeles de GUI reales) que lleva esa caja a la posición del config.
 */
public final class PartyHudTransform {
	/**
	 * Medidas leídas del bytecode de PartyOverlay.render (Cobblemon 1.7.3): 6 slots de 62×30 cada 34, pegados a
	 * x = 0 y empezando en alto/2 − 6·30/2 − 10. El slot seleccionado se adelanta 6 a la derecha (por eso el ancho
	 * es 68). Los avisos de movimiento/evolución sobresalen a la derecha y no cuentan en la caja.
	 */
	public static final int SLOT_WIDTH = 62 + 6;
	public static final int SLOT_HEIGHT = 30;
	public static final int SLOT_STEP_Y = 34;
	/** Separación entre slots en horizontal (ancho con el seleccionado + 4, como el hueco vertical). */
	public static final int SLOT_STEP_X = SLOT_WIDTH + 4;
	public static final int SLOTS = 6;

	/** Ancho y alto de la caja sin escalar según la orientación. */
	public static int[] unscaledSize(PartyHudConfig.Orientation orientation) {
		return orientation == PartyHudConfig.Orientation.HORIZONTAL
				? new int[]{(SLOTS - 1) * SLOT_STEP_X + SLOT_WIDTH, SLOT_HEIGHT}
				: new int[]{SLOT_WIDTH, (SLOTS - 1) * SLOT_STEP_Y + SLOT_HEIGHT};
	}

	private static boolean active;
	/** Escala de la ventana al empezar render, antes de que Diosesmon pueda cambiarla. */
	private static double realGuiScale;
	private static float scale;
	/** Traslación final (GUI real) de esta pasada de render. */
	private static float tx, ty;
	/** Horizontal: desplazamiento ya aplicado en pose() para el slot que se está dibujando (unidades sin escalar). */
	private static float slotX, slotY;
	private static boolean horizontal;
	/** Vertical pegado a la derecha: slots en espejo. */
	private static boolean mirrored;
	/** Dibujar el remate en el lado plano de cada slot. */
	private static boolean caps;
	private static boolean portraitUnflipped;
	private static GuiGraphics graphics;
	/** Ancho del remate del lado plano (en píxeles de la textura). */
	private static final int CAP_WIDTH = 4;
	/** Avisos de movimiento nuevo / evolución: 37×20 a escala 0.5. */
	private static final float NOTIFICATION_WIDTH = 18.5f;
	private static final float NOTIFICATION_HEIGHT = 10f;
	/** Hueco entre la fila y el aviso. */
	private static final float NOTIFICATION_GAP = 1f;
	/** Hueco entre dos avisos apilados del mismo slot. */
	private static final float NOTIFICATION_STACK_GAP = 2f;
	/** Horizontal: avisos debajo de la fila (si está en la mitad de arriba) o encima. */
	private static boolean notificationsBelow;
	/** Avisos ya dibujados en el slot actual (para apilar evolución + movimiento sin solaparlos). */
	private static int notificationsInSlot;
	private static boolean loggedActive;

	/**
	 * Escala y tamaño de pantalla (GUI) con los que se dibujó el HUD la última vez. Con escala 4, Diosesmon
	 * dibuja todo el HUD a 3.5 pero las pantallas siguen a 4: el editor convierte sus coordenadas con esto.
	 */
	private static double hudGuiScale;
	private static int hudScreenWidth;
	private static int hudScreenHeight;

	private PartyHudTransform() {}

	/** Unidades de HUD por cada unidad de la pantalla actual (1 sin Diosesmon; 4 / 3.5 con él a escala 4). */
	public static double hudUnitsPerScreenUnit() {
		if (hudGuiScale <= 0) return 1;
		return Minecraft.getInstance().getWindow().getGuiScale() / hudGuiScale;
	}

	/** Ancho y alto de la pantalla en unidades de HUD (los de la ventana si el HUD aún no se ha dibujado). */
	public static int[] hudScreenSize() {
		if (hudGuiScale <= 0) {
			return new int[]{Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight()};
		}
		return new int[]{hudScreenWidth, hudScreenHeight};
	}

	/** y de la caja original de Cobblemon para una pantalla de alto screenHeight (división entera, como Cobblemon). */
	public static int vanillaTop(int screenHeight) {
		return screenHeight / 2 - 6 * 30 / 2 - 10;
	}

	public static int scaled(int length, float scale) {
		return Math.round(length * scale);
	}

	/** Esquina superior izquierda de la caja ya escalada (w×h): pegada a su ancla y sin salir de la pantalla. */
	public static int[] position(PartyHudConfig config, int screenWidth, int screenHeight, int w, int h) {
		int x = switch (config.hudAnchor.column) {
			case 0 -> config.hudOffsetX;
			case 1 -> (screenWidth - w) / 2 + config.hudOffsetX;
			default -> screenWidth - w - config.hudOffsetX;
		};
		int y = switch (config.hudAnchor.row) {
			case 0 -> config.hudOffsetY;
			case 1 -> (screenHeight - h) / 2 + config.hudOffsetY;
			default -> screenHeight - h - config.hudOffsetY;
		};
		return new int[]{Math.clamp(x, 0, Math.max(0, screenWidth - w)), Math.clamp(y, 0, Math.max(0, screenHeight - h))};
	}

	/**
	 * Al contrario que {@link #position}: guarda el ancla más cercana (por tercios de pantalla) y la separación
	 * hasta ella. Así el HUD sigue en su sitio si cambia la resolución o la escala de la interfaz.
	 */
	public static void anchorAt(PartyHudConfig config, int screenWidth, int screenHeight, int x, int y, int w, int h) {
		int column = third(x + w / 2, screenWidth);
		int row = third(y + h / 2, screenHeight);
		config.hudAnchor = PartyHudConfig.HudAnchor.of(column, row);
		config.hudOffsetX = switch (column) {
			case 0 -> x;
			case 1 -> x - (screenWidth - w) / 2;
			default -> screenWidth - w - x;
		};
		config.hudOffsetY = switch (row) {
			case 0 -> y;
			case 1 -> y - (screenHeight - h) / 2;
			default -> screenHeight - h - y;
		};
	}

	private static int third(int center, int size) {
		return center < size / 3 ? 0 : center > size * 2 / 3 ? 2 : 1;
	}

	/** Caja del HUD en pantalla (x, y, ancho, alto) según el config. */
	public static int[] bounds(PartyHudConfig config, int screenWidth, int screenHeight) {
		int[] size = unscaledSize(config.orientation);
		int w = scaled(size[0], config.hudScale);
		int h = scaled(size[1], config.hudScale);
		int[] pos = position(config, screenWidth, screenHeight, w, h);
		return new int[]{pos[0], pos[1], w, h};
	}

	public static boolean hasRun() {
		return loggedActive;
	}

	public static void begin(GuiGraphics graphics) {
		PartyHudTransform.graphics = graphics;
		PartyHudConfig config = CobblePartyHud.CONFIG;
		if (!loggedActive) {
			loggedActive = true;
			CobblePartyHud.LOGGER.info("[Cobble Party HUD] Mixin de PartyOverlay activo");
		}
		Minecraft mc = Minecraft.getInstance();
		realGuiScale = mc.getWindow().getGuiScale();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		hudGuiScale = realGuiScale;
		hudScreenWidth = screenWidth;
		hudScreenHeight = screenHeight;
		if (!config.enabled) {
			active = false;
			return;
		}
		int[] box = bounds(config, screenWidth, screenHeight);
		scale = config.hudScale;
		tx = box[0];                                  // caja original en x = 0
		ty = box[1] - scale * vanillaTop(screenHeight);

		horizontal = config.orientation == PartyHudConfig.Orientation.HORIZONTAL;
		// En vertical pegado a la derecha, el slot se dibuja en espejo: su lado plano queda contra el borde derecho
		mirrored = !horizontal && config.hudAnchor.column == 2;
		// Remate en el lado plano cuando no toca el borde de la pantalla (en horizontal, siempre)
		caps = horizontal || (mirrored ? box[0] + box[2] < screenWidth : box[0] > 0);
		notificationsBelow = box[1] + box[3] / 2 < screenHeight / 2;

		graphics.pose().pushPose();
		graphics.pose().translate(tx, ty, 0);
		graphics.pose().scale(scale, scale, 1);
		if (mirrored) {
			// x → SLOT_WIDTH − x: el slot (0–62) pasa a 6–68 y el seleccionado (6–68) se adelanta hacia la izquierda
			graphics.pose().translate(SLOT_WIDTH, 0, 0);
			graphics.pose().scale(-1, 1, 1);
		}
		slotX = 0;
		slotY = 0;
		notificationsInSlot = 0;
		portraitUnflipped = false;
		active = true;
	}

	/**
	 * Cobblemon calcula 34·i (local 24 de render) al empezar cada slot. En horizontal se corre pose() para que el
	 * slot i quede a la derecha del anterior (+i·SLOT_STEP_X) y a la altura del primero (−34·i). Se aplica como
	 * diferencia con el slot anterior: aquí pose() es la nuestra (Cobblemon aún no ha hecho push en este slot).
	 */
	public static int slotOffset(int offsetY) {
		if (!active || !horizontal) return offsetY;
		int index = offsetY / SLOT_STEP_Y;
		float targetX = index * SLOT_STEP_X;
		float targetY = -offsetY;
		if (targetX != slotX || targetY != slotY) {
			graphics.pose().translate(targetX - slotX, targetY - slotY, 0);
			slotX = targetX;
			slotY = targetY;
			notificationsInSlot = 0;
		}
		return offsetY;
	}

	public static void end(GuiGraphics graphics) {
		if (!active) return;
		active = false;
		graphics.pose().popPose();
		PartyHudTransform.graphics = null;
	}

	/**
	 * enableScissor no aplica pose() y multiplica por la escala ACTUAL de la ventana (que Diosesmon puede haber
	 * cambiado), mientras que nuestra transformación está en GUI real: la traslación se corrige con k = real / actual.
	 */
	public static void scissor(GuiGraphics graphics, int x1, int y1, int x2, int y2, Operation<Void> original) {
		if (!active) {
			original.call(graphics, x1, y1, x2, y2);
			return;
		}
		double k = realGuiScale / Minecraft.getInstance().getWindow().getGuiScale();
		// En espejo el recorte también se refleja (y sus extremos se intercambian)
		int left = mirrored ? SLOT_WIDTH - x2 : x1;
		int right = mirrored ? SLOT_WIDTH - x1 : x2;
		original.call(graphics,
				Mth.floor(scale * (left + slotX) + tx * k), Mth.floor(scale * (y1 + slotY) + ty * k),
				Mth.ceil(scale * (right + slotX) + tx * k), Mth.ceil(scale * (y2 + slotY) + ty * k));
		if (mirrored) {
			// Entre este recorte y disableScissor solo se dibuja el retrato: se deja al derecho (un modelo 3D en
			// espejo se ve del revés). Se deshace en disableScissor.
			graphics.pose().pushPose();
			unflip(graphics.pose(), (x1 + x2) / 2f, 0);
			portraitUnflipped = true;
		}
	}

	public static void disableScissor(GuiGraphics graphics, Operation<Void> original) {
		original.call(graphics);
		if (portraitUnflipped) {
			portraitUnflipped = false;
			graphics.pose().popPose();
		}
	}

	// --- Espejo: el fondo del slot se refleja; texto, iconos, objeto y retrato se dibujan al derecho ---

	/** Fondos del slot (los únicos que se dibujan en espejo y los que llevan remate). */
	private static boolean isSlotBackground(ResourceLocation texture) {
		String path = texture.getPath();
		return path.startsWith("textures/gui/party/party_slot")
				&& !path.contains("portrait") && !path.contains("notification");
	}

	private static boolean isCollapsed(ResourceLocation texture) {
		return texture.getPath().endsWith("party_slot_collapsed.png");
	}

	private static boolean isNotification(ResourceLocation texture) {
		return texture.getPath().startsWith("textures/gui/party/party_slot_notification");
	}

	/**
	 * Columnas donde la textura tiene su chaflán: el seleccionado ("_active") es un slot 6 px más ancho con el
	 * borde en 48–51, no en 42–45.
	 */
	private static int capOffset(ResourceLocation texture) {
		return texture.getPath().contains("_active") ? 6 : 0;
	}

	/**
	 * Horizontal: Cobblemon pone los avisos (movimiento nuevo, evolución) a la derecha del slot (x + 56.5, 18.5 de
	 * ancho), donde tapan al slot siguiente. Se centran sobre el slot, debajo si la barra está arriba de la pantalla
	 * y encima si está abajo, conservando su altura relativa (evolución en +0, movimiento en +17, más la animación).
	 */
	private static void moveNotification(PoseStack pose, ResourceLocation texture, float y, float blitScale) {
		float slotTop = vanillaTop(hudScreenHeight) - slotY;          // slotY = −34·i
		float current = y * blitScale;                                 // arriba del aviso ahora
		// Cobblemon baja el de movimiento nuevo 17 px para que quepa bajo el de evolución; aquí sobra (lo separaba de
		// la fila). Solo se conserva la animación.
		// Sin el desplazamiento de la animación de entrada: cada aviso tiene la suya y, apilados, el de atrás se
		// deslizaba sobre el de delante. El fundido (transparencia) se mantiene.
		float dx = (SLOT_WIDTH - 6) / 2f - NOTIFICATION_WIDTH / 2f - 56.5f;
		// Si el mismo slot ya dibujó un aviso (evolución va antes que movimiento), este va a continuación, más lejos
		float stack = notificationsInSlot * (NOTIFICATION_HEIGHT + NOTIFICATION_STACK_GAP);
		notificationsInSlot++;
		float target = notificationsBelow
				? slotTop + SLOT_HEIGHT + NOTIFICATION_GAP + stack
				: slotTop - NOTIFICATION_GAP - NOTIFICATION_HEIGHT - stack;
		pose.translate(dx, target - current, 0);
	}

	/**
	 * Deja al derecho lo que se dibuje en [x, x + width] dentro del espejo: queda en el sitio reflejado pero sin
	 * reflejar. Con width = 0 refleja alrededor de x (para lo centrado).
	 */
	private static void unflip(PoseStack pose, float x, float width) {
		pose.translate(x + width, 0, 0);
		pose.scale(-1, 1, 1);
		pose.translate(-x, 0, 0);
	}

	/** blitk de Cobblemon: escala la pose por scale y dibuja en (x, y) con el ancho dado. */
	public static void blit(PoseStack pose, ResourceLocation texture, float x, float y, float width, float blitScale, Runnable original) {
		if (!active) {
			original.run();
			return;
		}
		boolean background = isSlotBackground(texture);
		if (horizontal && isCollapsed(texture)) {
			return; // en fila, la pestaña del slot vacío (5 px, pensada para el borde) solo estorba
		}
		if (horizontal && isNotification(texture)) {
			pose.pushPose();
			moveNotification(pose, texture, y, blitScale);
			original.run();
			pose.popPose();
			return;
		}
		if (mirrored && !background) {
			pose.pushPose();
			unflip(pose, x * blitScale, width * blitScale);
			original.run();
			pose.popPose();
		} else if (mirrored) {
			// Con el espejo los triángulos quedan al revés: sin esto, el culling podría no dibujarlos
			RenderSystem.disableCull();
			original.run();
			RenderSystem.enableCull();
		} else {
			original.run();
		}
		if (caps && background && !isCollapsed(texture) && blitScale == 1) {
			drawCap(pose, texture, x, y);
		}
	}

	/**
	 * Remate del lado plano: las columnas 42–45 (borde y chaflán del panel, filas 0–23) y 52–55 (punta de la franja
	 * inferior, filas 24–29) de la propia textura, reflejadas a la izquierda de x. Al usar la misma textura sirve
	 * para normal, seleccionado y debilitado, y para resource packs que respeten la forma.
	 */
	private static void drawCap(PoseStack pose, ResourceLocation texture, float x, float y) {
		pose.pushPose();
		pose.translate(x, y, 0);
		pose.scale(-1, 1, 1);
		RenderSystem.disableCull();
		int offset = capOffset(texture);
		graphics.blit(texture, 0, 0, 42 + offset, 0, CAP_WIDTH, 24, 62, 30);
		graphics.blit(texture, 0, 24, 52 + offset, 24, CAP_WIDTH, 6, 62, 30);
		RenderSystem.enableCull();
		pose.popPose();
	}

	/** drawScaledText de Cobblemon: x es el inicio (o el centro si centered) y el ancho es el del texto × textScale. */
	public static void text(PoseStack pose, Component text, ResourceLocation font, float x, float textScale, boolean centered, Runnable original) {
		if (!active || !mirrored) {
			original.run();
			return;
		}
		float width = 0;
		if (!centered) {
			Component styled = font == null ? text : text.copy().withStyle(style -> style.withFont(font));
			width = Minecraft.getInstance().font.width(styled) * textScale;
		}
		pose.pushPose();
		unflip(pose, x, width);
		original.run();
		pose.popPose();
	}

	/** renderScaledGuiItemIcon: objeto de 16×16 × itemScale a partir de x. */
	public static void item(PoseStack pose, float x, float itemScale, Runnable original) {
		if (!active || !mirrored) {
			original.run();
			return;
		}
		pose.pushPose();
		unflip(pose, x, 16 * itemScale);
		original.run();
		pose.popPose();
	}
}
