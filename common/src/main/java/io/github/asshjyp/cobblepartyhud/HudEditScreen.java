package io.github.asshjyp.cobblepartyhud;

import com.cobblemon.mod.common.client.CobblemonClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Objects;

/**
 * Mover y escalar el HUD de party: se arrastra con el ratón, la rueda cambia el tamaño y las flechas lo afinan
 * píxel a píxel (Mayús = 10). Sin fondo desenfocado: lo que se ve es el HUD real de Cobblemon
 * (PartyOverlayCompanionMixin lo deja visible con esta pantalla abierta) y los HUD de los otros mods.
 * Al soltarlo se ancla al punto más cercano ({@link PartyHudTransform#anchorAt}).
 * Todo se calcula en unidades de HUD, que no siempre son las de esta pantalla: con escala 4, Diosesmon dibuja el HUD
 * a 3.5 ({@link PartyHudTransform#hudUnitsPerScreenUnit}). El ratón se convierte y el recuadro se dibuja reescalado.
 * Convención compartida con Cobble Hunt Log y Cobble EV Tracker: esta clase se llama HudEditScreen y vive en
 * io.github.asshjyp.<mod>; no renombrarla.
 */
public class HudEditScreen extends Screen {
	private static final int BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final float SCALE_STEP = 0.1f;
	private static final int OUTLINE = 0xFFFFFFFF;
	private static final int OUTLINE_IDLE = 0xA0FFFFFF;

	private final Screen parent;
	private boolean dragging;
	private double grabX;
	private double grabY;

	public HudEditScreen(Screen parent) {
		super(Component.translatable("cobblepartyhud.edit.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = height / 2 + 16;
		int left = width / 2 - BUTTON_WIDTH * 3 / 2 - 4;
		addRenderableWidget(Button.builder(Component.translatable("cobblepartyhud.edit.reset"), button -> {
					CobblePartyHud.CONFIG.reset();
					rebuildWidgets();
				})
				.bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		Button orientation = addRenderableWidget(Button.builder(orientationLabel(), button -> {
					PartyHudConfig config = CobblePartyHud.CONFIG;
					// Se gira alrededor de la esquina superior izquierda y se recoloca para que quepa
					int[] before = bounds();
					config.orientation = config.orientation == PartyHudConfig.Orientation.VERTICAL
							? PartyHudConfig.Orientation.HORIZONTAL : PartyHudConfig.Orientation.VERTICAL;
					shrinkToFit();
					int[] after = bounds();
					moveTo(before[0], before[1], after[2], after[3]);
					button.setMessage(orientationLabel());
				})
				.bounds(left + BUTTON_WIDTH + 4, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		orientation.active = CobblePartyHud.CONFIG.enabled;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(left + (BUTTON_WIDTH + 4) * 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		// Desactivado: el party vuelve al sitio original de Cobblemon; la posición guardada se conserva
		addRenderableWidget(Button.builder(enabledLabel(), button -> {
					CobblePartyHud.CONFIG.enabled = !CobblePartyHud.CONFIG.enabled;
					dragging = false;
					rebuildWidgets();
				})
				.bounds(width / 2 - BUTTON_WIDTH / 2, y + BUTTON_HEIGHT + 4, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	private static Component enabledLabel() {
		return Component.translatable(CobblePartyHud.CONFIG.enabled ? "cobblepartyhud.edit.on" : "cobblepartyhud.edit.off");
	}

	private static Component orientationLabel() {
		return Component.translatable(CobblePartyHud.CONFIG.orientation == PartyHudConfig.Orientation.HORIZONTAL
				? "cobblepartyhud.edit.horizontal" : "cobblepartyhud.edit.vertical");
	}

	/** El HUD nunca más grande que la pantalla: baja la escala de décima en décima hasta que quepa. */
	private void shrinkToFit() {
		PartyHudConfig config = CobblePartyHud.CONFIG;
		int[] screen = PartyHudTransform.hudScreenSize();
		while (config.hudScale > PartyHudConfig.MIN_SCALE && !fits(screen)) {
			config.hudScale = Math.round((config.hudScale - SCALE_STEP) * 10) / 10f;
		}
	}

	private boolean fits(int[] screen) {
		int[] box = bounds();
		return box[2] <= screen[0] && box[3] <= screen[1];
	}

	/** x, y, ancho y alto del HUD, ya escalado, en unidades de HUD. */
	private int[] bounds() {
		int[] screen = PartyHudTransform.hudScreenSize();
		return PartyHudTransform.bounds(CobblePartyHud.CONFIG, screen[0], screen[1]);
	}

	/** Coordenada de esta pantalla → unidades de HUD. */
	private static double toHud(double screenCoordinate) {
		return screenCoordinate * PartyHudTransform.hudUnitsPerScreenUnit();
	}

	/** mouseX/mouseY en unidades de pantalla; box en unidades de HUD. */
	private boolean isOver(int[] box, double mouseX, double mouseY) {
		double x = toHud(mouseX);
		double y = toHud(mouseY);
		return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
	}

	/** Coloca la esquina superior izquierda del HUD en (x, y) (unidades de HUD), sin salir de la pantalla. */
	private void moveTo(int x, int y, int w, int h) {
		int[] screen = PartyHudTransform.hudScreenSize();
		x = Math.clamp(x, 0, Math.max(0, screen[0] - w));
		y = Math.clamp(y, 0, Math.max(0, screen[1] - h));
		PartyHudTransform.anchorAt(CobblePartyHud.CONFIG, screen[0], screen[1], x, y, w, h);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Sin desenfoque ni oscurecer: se ve el juego, el party real y los demás HUD detrás
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		PartyHudConfig config = CobblePartyHud.CONFIG;
		if (config.enabled) {
			int[] box = bounds();
			float toScreen = (float) (1 / PartyHudTransform.hudUnitsPerScreenUnit());
			graphics.pose().pushPose();
			graphics.pose().scale(toScreen, toScreen, 1);
			graphics.renderOutline(box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2,
					dragging || isOver(box, mouseX, mouseY) ? OUTLINE : OUTLINE_IDLE);
			graphics.pose().popPose();
		}
		super.render(graphics, mouseX, mouseY, partialTick);

		int textY = height / 2 - 24;
		graphics.drawCenteredString(font, title, width / 2, textY, 0xFFFF55);
		graphics.drawCenteredString(font, Component.translatable("cobblepartyhud.edit.help"), width / 2, textY + 12, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.translatable("cobblepartyhud.edit.scale",
				String.format(Locale.ROOT, "%.1f", config.hudScale)), width / 2, textY + 24, 0xAAAAAA);
		Component hint = hint();
		if (hint != null) {
			graphics.drawCenteredString(font, hint, width / 2, height / 2 + 16 + (BUTTON_HEIGHT + 4) * 2 + 4, 0xFF5555);
		}
	}

	/** Aviso si el HUD no se ve: party vacío, mod desactivado o mixin que no se aplicó (otra versión de Cobblemon). */
	private Component hint() {
		if (!CobblePartyHud.CONFIG.enabled) {
			return Component.translatable("cobblepartyhud.edit.disabled");
		}
		boolean partyEmpty = CobblemonClient.INSTANCE.getStorage().getParty().getSlots().stream().allMatch(Objects::isNull);
		if (partyEmpty) {
			return Component.translatable("cobblepartyhud.edit.empty_party");
		}
		if (!PartyHudTransform.hasRun()) {
			return Component.translatable("cobblepartyhud.edit.no_mixin");
		}
		return null;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		int[] box = bounds();
		if (CobblePartyHud.CONFIG.enabled && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isOver(box, mouseX, mouseY)) {
			dragging = true;
			grabX = toHud(mouseX) - box[0];
			grabY = toHud(mouseY) - box[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (dragging) {
			int[] box = bounds();
			moveTo((int) Math.round(toHud(mouseX) - grabX), (int) Math.round(toHud(mouseY) - grabY), box[2], box[3]);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0 || !CobblePartyHud.CONFIG.enabled) {
			return false;
		}
		PartyHudConfig config = CobblePartyHud.CONFIG;
		float scale = config.hudScale + (scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
		// Redondeo a décimas para que no se acumulen errores (0.30000001…)
		config.hudScale = Math.clamp(Math.round(scale * 10) / 10f, PartyHudConfig.MIN_SCALE, PartyHudConfig.MAX_SCALE);
		shrinkToFit();
		// Al cambiar de tamaño crece desde su ancla; se recoloca por si ya no cabe
		int[] box = bounds();
		moveTo(box[0], box[1], box[2], box[3]);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
		int dx = switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT -> -step;
			case GLFW.GLFW_KEY_RIGHT -> step;
			default -> 0;
		};
		int dy = switch (keyCode) {
			case GLFW.GLFW_KEY_UP -> -step;
			case GLFW.GLFW_KEY_DOWN -> step;
			default -> 0;
		};
		if ((dx != 0 || dy != 0) && CobblePartyHud.CONFIG.enabled) {
			int[] box = bounds();
			moveTo(box[0] + dx, box[1] + dy, box[2], box[3]);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		CobblePartyHud.CONFIG.save();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
