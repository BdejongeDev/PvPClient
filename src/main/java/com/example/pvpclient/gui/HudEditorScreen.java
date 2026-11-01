package com.example.pvpclient.gui;

import com.example.pvpclient.PvPClient;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Drag enabled HUD panels to reposition them. The panels themselves are drawn by the normal
 * HUD pass (in preview mode while this screen is open); this screen only adds outlines and
 * handles dragging. Panels snap to screen edges and the screen centre.
 */
public final class HudEditorScreen extends Screen {
	private static final int SNAP = 4;

	private final Screen parent;
	@Nullable
	private HudModule dragging;
	private double offX, offY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0x30000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		// Centre guides.
		g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x20FFFFFF);
		g.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x20FFFFFF);

		for (HudModule hud : enabledHuds()) {
			if (hud.getWidth() <= 0) {
				continue;
			}
			int x = hud.getDrawX(), y = hud.getDrawY();
			boolean hover = hud == dragging || inside(hud, mouseX, mouseY);
			RenderUtil.outline(g, x - 1, y - 1, x + hud.getWidth() + 1, y + hud.getHeight() + 1, hover ? 0xFF4ADE80 : 0x80FFFFFF);
			if (hover) {
				g.text(font, hud.getName(), x, Math.max(0, y - 10), 0xFF4ADE80, true);
			}
		}
		g.centeredText(font, "Drag HUD elements to move them. Esc to go back.", this.width / 2, this.height - 14, 0xFFFFFFFF);
	}

	private static java.util.List<HudModule> enabledHuds() {
		return ModuleManager.get().getHudModules().stream().filter(HudModule::isEnabled).toList();
	}

	private static boolean inside(HudModule hud, double mx, double my) {
		return mx >= hud.getDrawX() && mx < hud.getDrawX() + hud.getWidth()
				&& my >= hud.getDrawY() && my < hud.getDrawY() + hud.getHeight();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (HudModule hud : enabledHuds()) {
			if (hud.getWidth() > 0 && inside(hud, event.x(), event.y())) {
				dragging = hud;
				offX = event.x() - hud.getDrawX();
				offY = event.y() - hud.getDrawY();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) {
			return super.mouseDragged(event, dx, dy);
		}
		int w = dragging.getWidth(), h = dragging.getHeight();
		int x = snap((int) (event.x() - offX), w, this.width);
		int y = snap((int) (event.y() - offY), h, this.height);
		dragging.setPosition(Math.max(0, Math.min(this.width - w, x)), Math.max(0, Math.min(this.height - h, y)));
		return true;
	}

	/** Snap the start, centre or end of a panel to the start, centre or end of the screen. */
	private static int snap(int pos, int size, int screen) {
		if (Math.abs(pos) < SNAP) return 0;
		if (Math.abs(pos + size - screen) < SNAP) return screen - size;
		if (Math.abs(pos + size / 2 - screen / 2) < SNAP) return screen / 2 - size / 2;
		return pos;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		PvPClient.config().save();
		this.minecraft.gui.setScreen(parent);
	}
}
