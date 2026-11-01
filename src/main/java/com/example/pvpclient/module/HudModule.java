package com.example.pvpclient.module;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A module that draws a movable HUD panel. Positions are stored in the config and edited
 * with the HUD editor. {@link #draw} returns the panel size so the editor knows its bounds.
 */
public abstract class HudModule extends Module {
	public static final int PADDING = 3;
	public static final int BACKGROUND = 0x90101018;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int MUTED = 0xFFAAAAAA;
	/** Default positions past the screen edge; rendering clamps them to the right/bottom edge. */
	public static final int RIGHT = 100000, BOTTOM = 100000;

	private int x, y;
	private int width, height;
	/** Where the panel was last drawn (after clamping to the screen). */
	private int drawX, drawY;

	protected HudModule(String name, String description, Category category, int defaultX, int defaultY) {
		super(name, description, category);
		this.x = defaultX;
		this.y = defaultY;
	}

	/**
	 * Draws the panel at (x, y) and returns {width, height}. When {@code preview} is true
	 * (HUD editor) draw placeholder content even if there is nothing to show.
	 * Return {0, 0} to draw nothing.
	 */
	protected abstract int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview);

	public final void render(GuiGraphicsExtractor g, boolean preview) {
		// Keep panels on screen when the window/GUI scale changes.
		int cx = Math.max(0, Math.min(x, g.guiWidth() - Math.max(width, 10)));
		int cy = Math.max(0, Math.min(y, g.guiHeight() - Math.max(height, 10)));
		drawX = cx;
		drawY = cy;
		int[] size = draw(g, cx, cy, preview);
		width = size[0];
		height = size[1];
	}

	/** Helper: draws text lines on a translucent background and returns the size. */
	protected int[] panel(GuiGraphicsExtractor g, int x, int y, String... lines) {
		int w = 0;
		for (String line : lines) {
			w = Math.max(w, mc.font.width(line));
		}
		int h = lines.length * (mc.font.lineHeight + 1) + PADDING * 2 - 1;
		w += PADDING * 2;
		g.fill(x, y, x + w, y + h, BACKGROUND);
		for (int i = 0; i < lines.length; i++) {
			g.text(mc.font, lines[i], x + PADDING, y + PADDING + i * (mc.font.lineHeight + 1), TEXT, true);
		}
		return new int[]{w, h};
	}

	public int getX() { return x; }
	public int getY() { return y; }
	public int getDrawX() { return drawX; }
	public int getDrawY() { return drawY; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
		ModuleManager.get().markDirty();
	}
}
