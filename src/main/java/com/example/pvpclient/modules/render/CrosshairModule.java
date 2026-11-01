package com.example.pvpclient.modules.render;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.module.setting.ModeSetting;
import com.example.pvpclient.module.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom crosshair. While enabled it replaces the vanilla crosshair element (see
 * {@code PvPClient#registerHud}); when disabled the vanilla one is drawn as usual.
 */
public final class CrosshairModule extends Module {
	private final ModeSetting style = setting(new ModeSetting("Style", "Cross", "Cross", "Cross + Dot", "Dot", "Circle", "T-Shape"));
	private final ModeSetting color = setting(new ModeSetting("Color", "White", "White", "Green", "Aqua", "Yellow", "Pink", "Red"));
	private final NumberSetting size = setting(new NumberSetting("Size", 5, 1, 15, 1));
	private final NumberSetting gap = setting(new NumberSetting("Gap", 2, 0, 8, 1));
	private final NumberSetting thickness = setting(new NumberSetting("Thickness", 1, 1, 4, 1));
	private final NumberSetting opacity = setting(new NumberSetting("Opacity", 100, 20, 100, 5));
	private final BoolSetting outline = setting(new BoolSetting("Outline", true));
	private final BoolSetting targetColor = setting(new BoolSetting("Red On Target", true));
	private final BoolSetting thirdPerson = setting(new BoolSetting("Show In Third Person", false));

	public CrosshairModule() {
		super("Crosshair", "Custom crosshair styles and colors", Category.RENDER);
	}

	/** Whether the crosshair should be drawn at all this frame (mirrors vanilla rules). */
	public boolean shouldDraw() {
		if (!inGame() || mc.player.isSpectator()) {
			return false;
		}
		return thirdPerson.get() || mc.options.getCameraType().isFirstPerson();
	}

	private int argb() {
		int rgb;
		if (targetColor.get() && mc.crosshairPickEntity instanceof LivingEntity) {
			rgb = 0xFF3030;
		} else {
			rgb = switch (color.get()) {
				case "Green" -> 0x4ADE80;
				case "Aqua" -> 0x40E0FF;
				case "Yellow" -> 0xFFD640;
				case "Pink" -> 0xFF6AD5;
				case "Red" -> 0xFF3030;
				default -> 0xFFFFFF;
			};
		}
		int alpha = (int) Math.round(opacity.get() / 100.0 * 255);
		return (alpha << 24) | rgb;
	}

	public void draw(GuiGraphicsExtractor g) {
		int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
		int col = argb();
		int outlineCol = (Math.round(((col >>> 24) & 0xFF) * 0.8F) << 24);
		int s = size.intValue(), gp = gap.intValue(), t = thickness.intValue();
		// Offsets so odd/even thicknesses stay centred on the screen centre pixel.
		int lo = -(t / 2), hi = lo + t;

		switch (style.get()) {
			case "Dot" -> rect(g, cx + lo, cy + lo, cx + hi, cy + hi, col, outlineCol);
			case "Circle" -> circle(g, cx, cy, Math.max(2, s), t, col, outlineCol);
			case "T-Shape" -> {
				arms(g, cx, cy, s, gp, lo, hi, col, outlineCol, false);
			}
			default -> {
				arms(g, cx, cy, s, gp, lo, hi, col, outlineCol, true);
				if (style.is("Cross + Dot")) {
					rect(g, cx + lo, cy + lo, cx + hi, cy + hi, col, outlineCol);
				}
			}
		}
	}

	/** Left, right and bottom arms, plus the top arm when {@code top} is true. */
	private void arms(GuiGraphicsExtractor g, int cx, int cy, int s, int gp, int lo, int hi, int col, int outlineCol, boolean top) {
		rect(g, cx - gp - s, cy + lo, cx - gp, cy + hi, col, outlineCol);          // left
		rect(g, cx + gp + 1, cy + lo, cx + gp + 1 + s, cy + hi, col, outlineCol);  // right
		rect(g, cx + lo, cy + gp + 1, cx + hi, cy + gp + 1 + s, col, outlineCol);  // bottom
		if (top) {
			rect(g, cx + lo, cy - gp - s, cx + hi, cy - gp, col, outlineCol);      // top
		}
	}

	private void rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int col, int outlineCol) {
		if (outline.get()) {
			g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, outlineCol);
		}
		g.fill(x1, y1, x2, y2, col);
	}

	/** Pixel circle: fills every pixel whose distance to the centre is within the ring. */
	private void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int t, int col, int outlineCol) {
		double inner = r - t + 0.5, outer = r + 0.5;
		for (int dy = -r - 1; dy <= r + 1; dy++) {
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				double d = Math.sqrt(dx * dx + dy * dy);
				if (d >= inner && d < outer) {
					g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, col);
				} else if (outline.get() && ((d >= inner - 1 && d < inner) || (d >= outer && d < outer + 1))) {
					g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, outlineCol);
				}
			}
		}
		g.fill(cx, cy, cx + 1, cy + 1, col); // centre pixel
	}
}
