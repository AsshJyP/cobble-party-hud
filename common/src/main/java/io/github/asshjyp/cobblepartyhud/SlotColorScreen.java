package io.github.asshjyp.cobblepartyhud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntConsumer;

/**
 * "Colores…" del editor: un color por ranura (o el mismo para todas), con 12 predefinidos, barras R/G/B, código hex
 * y la opacidad de las ranuras. Los cambios se ven al momento en el HUD real (PartyOverlayCompanionMixin lo deja
 * visible con esta pantalla abierta) y en la vista previa. Blanco = sin color: el gris de Cobblemon.
 */
public class SlotColorScreen extends Screen {
	private static final int[] PRESETS = {
			PartyHudConfig.NO_COLOR, 0x9A9A9A, 0x4A4A4A, 0xE04848, 0xF09632, 0xF0D040,
			0x48C060, 0x38C0C0, 0x4080E8, 0xA060E0, 0xF070B8, 0x8A5A30};
	/** Valor de {@link #target} para cambiar las 6 ranuras a la vez. */
	private static final int ALL = -1;
	private static final int PANEL_WIDTH = 300;
	private static final int PANEL_HEIGHT = 182;
	private static final ResourceLocation SLOT_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/party/party_slot.png");

	private final Screen parent;
	private int target = 0;
	private ChannelSlider red;
	private ChannelSlider green;
	private ChannelSlider blue;
	private EditBox hex;
	/** Evita que al sincronizar un control con los demás se vuelva a aplicar el color (bucle de avisos). */
	private boolean syncing;
	private int left;
	private int top;

	public SlotColorScreen(Screen parent) {
		super(Component.translatable("cobblepartyhud.colors.title"));
		this.parent = parent;
	}

	private static PartyHudConfig config() {
		return CobblePartyHud.CONFIG;
	}

	/** Color que muestran los controles: el de la ranura elegida (con "Todas", el de la primera). */
	private int currentColor() {
		return config().slotColor(target == ALL ? 0 : target);
	}

	@Override
	protected void init() {
		left = (width - PANEL_WIDTH) / 2;
		top = Math.max(4, (height - PANEL_HEIGHT) / 2);
		int y = top + 14;
		for (int i = 0; i < PartyHudTransform.SLOTS; i++) {
			int slot = i;
			addRenderableWidget(new Swatch(left + i * 24, y, 20, 20, String.valueOf(i + 1),
					() -> config().slotColor(slot), () -> target == slot, ignored -> select(slot)))
					.setTooltip(Tooltip.create(Component.translatable("cobblepartyhud.colors.slot", i + 1)));
		}
		Button all = addRenderableWidget(Button.builder(Component.translatable("cobblepartyhud.colors.all"), button -> select(ALL))
				.bounds(left + 150, y, 100, 20).build());
		all.active = target != ALL;

		y = top + 40;
		for (int i = 0; i < PRESETS.length; i++) {
			int color = PRESETS[i];
			Swatch swatch = addRenderableWidget(new Swatch(left + i * 19, y, 16, 16, "", () -> color,
					() -> currentColor() == color, ignored -> applyAndSync(color)));
			if (color == PartyHudConfig.NO_COLOR) {
				swatch.setTooltip(Tooltip.create(Component.translatable("cobblepartyhud.colors.original")));
			}
		}

		int color = currentColor();
		y = top + 62;
		red = addRenderableWidget(new ChannelSlider(left, y, "R", color >> 16 & 0xFF, value -> fromSliders()));
		green = addRenderableWidget(new ChannelSlider(left, y + 22, "G", color >> 8 & 0xFF, value -> fromSliders()));
		blue = addRenderableWidget(new ChannelSlider(left, y + 44, "B", color & 0xFF, value -> fromSliders()));

		hex = addRenderableWidget(new EditBox(font, left + 182, y + 10, 70, 16, Component.translatable("cobblepartyhud.colors.hex")));
		hex.setMaxLength(7);
		hex.setValue(PartyHudConfig.formatColor(color));
		hex.setResponder(text -> {
			Integer parsed = PartyHudConfig.parseColor(text);
			if (parsed != null && !syncing) {
				apply(parsed);
				syncSliders(parsed);
			}
		});

		addRenderableWidget(new OpacitySlider(left, y + 66));

		y = top + PANEL_HEIGHT - 20;
		addRenderableWidget(Button.builder(Component.translatable("cobblepartyhud.colors.reset_slot"),
						button -> applyAndSync(PartyHudConfig.NO_COLOR))
				.bounds(left, y, 98, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("cobblepartyhud.colors.reset_all"), button -> {
					for (int i = 0; i < PartyHudTransform.SLOTS; i++) config().setSlotColor(i, PartyHudConfig.NO_COLOR);
					config().slotOpacity = 100;
					rebuildWidgets();
				})
				.bounds(left + 101, y, 98, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(left + 202, y, 98, 20).build());
	}

	private void select(int slot) {
		target = slot;
		rebuildWidgets();
	}

	/** Cambia el color de la ranura elegida (o de todas). */
	private void apply(int rgb) {
		if (target == ALL) {
			for (int i = 0; i < PartyHudTransform.SLOTS; i++) config().setSlotColor(i, rgb);
		} else {
			config().setSlotColor(target, rgb);
		}
	}

	private void applyAndSync(int rgb) {
		apply(rgb);
		syncSliders(rgb);
		syncHex(rgb);
	}

	private void fromSliders() {
		if (syncing) return;
		int rgb = red.channel() << 16 | green.channel() << 8 | blue.channel();
		apply(rgb);
		syncHex(rgb);
	}

	private void syncSliders(int rgb) {
		syncing = true;
		red.setChannel(rgb >> 16 & 0xFF);
		green.setChannel(rgb >> 8 & 0xFF);
		blue.setChannel(rgb & 0xFF);
		syncing = false;
	}

	private void syncHex(int rgb) {
		syncing = true;
		hex.setValue(PartyHudConfig.formatColor(rgb));
		syncing = false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Sin desenfoque: el HUD real de party se ve detrás. Solo un panel oscuro bajo los controles.
		graphics.fill(left - 6, top - 6, left + PANEL_WIDTH + 6, top + PANEL_HEIGHT + 6, 0xC0101018);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawString(font, title, left, top, 0xFFFF55);
		graphics.drawString(font, Component.translatable("cobblepartyhud.colors.hex"), left + 182, top + 62, 0xAAAAAA);
		graphics.drawString(font, Component.translatable("cobblepartyhud.colors.preview"), left + 182, top + 92, 0xAAAAAA);
		renderPreview(graphics, left + 182, top + 102);
	}

	/** Ranura de Cobblemon teñida con el color y la opacidad actuales, sobre un fondo claro para juzgar la opacidad. */
	private void renderPreview(GuiGraphics graphics, int x, int y) {
		graphics.fill(x - 2, y - 2, x + 64, y + 32, 0xFF7FAEF0);
		int rgb = currentColor();
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f,
				config().slotOpacity / 100f);
		graphics.blit(SLOT_TEXTURE, x, y, 0, 0, 62, 30, 62, 30);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}

	@Override
	public void removed() {
		config().save();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	/** Cuadro de color que se puede pulsar (ranuras y predefinidos); borde blanco si está elegido. */
	private class Swatch extends AbstractWidget {
		private final String label;
		private final java.util.function.IntSupplier color;
		private final java.util.function.BooleanSupplier selected;
		private final IntConsumer onPress;

		Swatch(int x, int y, int width, int height, String label, java.util.function.IntSupplier color,
				java.util.function.BooleanSupplier selected, IntConsumer onPress) {
			super(x, y, width, height, Component.literal(label));
			this.label = label;
			this.color = color;
			this.selected = selected;
			this.onPress = onPress;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int rgb = color.getAsInt();
			int border = selected.getAsBoolean() ? 0xFFFFFFFF : isHovered() ? 0xFFA0A0A0 : 0xFF000000;
			graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
			graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | rgb);
			if (!label.isEmpty()) {
				// Texto blanco o negro según lo claro que sea el color
				int luma = (rgb >> 16 & 0xFF) * 299 + (rgb >> 8 & 0xFF) * 587 + (rgb & 0xFF) * 114;
				graphics.drawCenteredString(font, label, getX() + width / 2, getY() + (height - 8) / 2,
						luma > 150_000 ? 0x202020 : 0xFFFFFF);
			}
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			onPress.accept(0);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** Barra de un canal (0–255). */
	private static class ChannelSlider extends AbstractSliderButton {
		private final String name;
		private final IntConsumer onChange;

		ChannelSlider(int x, int y, String name, int channel, IntConsumer onChange) {
			super(x, y, 170, 20, Component.empty(), channel / 255.0);
			this.name = name;
			this.onChange = onChange;
			updateMessage();
		}

		int channel() {
			return (int) Math.round(value * 255);
		}

		void setChannel(int channel) {
			value = channel / 255.0;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(name + ": " + channel()));
		}

		@Override
		protected void applyValue() {
			onChange.accept(channel());
		}
	}

	/** Opacidad de los paneles de las ranuras, de MIN_OPACITY a 100 %. */
	private static class OpacitySlider extends AbstractSliderButton {
		private static final int RANGE = 100 - PartyHudConfig.MIN_OPACITY;

		OpacitySlider(int x, int y) {
			super(x, y, 170, 20, Component.empty(), (config().slotOpacity - PartyHudConfig.MIN_OPACITY) / (double) RANGE);
			updateMessage();
		}

		private int percent() {
			return PartyHudConfig.MIN_OPACITY + (int) Math.round(value * RANGE);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("cobblepartyhud.colors.opacity", percent()));
		}

		@Override
		protected void applyValue() {
			config().slotOpacity = percent();
		}
	}
}
