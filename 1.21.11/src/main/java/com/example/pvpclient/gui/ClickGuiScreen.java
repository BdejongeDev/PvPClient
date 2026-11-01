package com.example.pvpclient.gui;

import com.example.pvpclient.PvPClient;
import com.example.pvpclient.input.KeybindManager;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.module.setting.ModeSetting;
import com.example.pvpclient.module.setting.NumberSetting;
import com.example.pvpclient.module.setting.Setting;
import com.example.pvpclient.modules.misc.ClickGuiModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Category panels with modules. Left click toggles a module, right click expands its
 * settings. Panels can be dragged by their header; positions are saved in the config.
 *
 * <p>The layout is rebuilt every frame into a flat list of {@link Element}s which is then
 * used both for drawing and for hit-testing clicks, so the two can never disagree.
 */
public final class ClickGuiScreen extends Screen {
	private static final int PANEL_W = 120, HEADER_H = 16, ROW_H = 14, SLIDER_H = 20;
	private static final int BG = 0xE0121418, HEADER_BG = 0xF00B0C0F, ROW_HOVER = 0x20FFFFFF;
	private static final int TEXT = 0xFFFFFFFF, MUTED = 0xFF9CA3AF;
	// Raw GLFW button ids as delivered by MouseButtonEvent#button(). Not InputConstants.MOUSE_BUTTON_*:
	// in 26.x those are remapped values (left = 1, right = 3) and do not match.
	private static final int BUTTON_LEFT = 0, BUTTON_RIGHT = 1;

	private enum Kind { HEADER, MODULE, BOOL, NUMBER, MODE, BIND, HUD_BUTTON, DISCORD_BUTTON }

	private record Element(Kind kind, int x, int y, int w, int h, @Nullable Category category,
						   @Nullable Module module, @Nullable Setting<?> setting) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	@Nullable
	private final Screen parent;
	private final Map<Category, int[]> panelPos = new EnumMap<>(Category.class);
	private final Set<Module> expanded = new HashSet<>();
	private List<Element> elements = new ArrayList<>();

	@Nullable
	private Category draggingPanel;
	private double dragOffX, dragOffY;
	@Nullable
	private NumberSetting draggingSlider;
	private int sliderX, sliderW;
	@Nullable
	private Module listeningForKey;

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal("PvPClient"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		// Default grid: as many columns as fit; each row starts below the tallest panel above it.
		int columns = Math.max(1, (this.width - 10) / (PANEL_W + 8));
		Category[] cats = Category.values();
		int rowY = 10, rowHeight = 0;
		for (int i = 0; i < cats.length; i++) {
			if (i > 0 && i % columns == 0) {
				rowY += rowHeight + 8;
				rowHeight = 0;
			}
			rowHeight = Math.max(rowHeight, HEADER_H + ModuleManager.get().getModules(cats[i]).size() * ROW_H);
			int[] saved = PvPClient.config().getPanelPosition(cats[i].displayName);
			panelPos.put(cats[i], saved != null ? saved.clone()
					: new int[]{10 + (i % columns) * (PANEL_W + 8), rowY});
		}
	}

	private int accent() {
		return ModuleManager.get().get(ClickGuiModule.class).accentColor();
	}

	private List<Element> layout() {
		List<Element> list = new ArrayList<>();
		for (Category cat : Category.values()) {
			int[] p = panelPos.get(cat);
			int x = p[0], y = p[1];
			list.add(new Element(Kind.HEADER, x, y, PANEL_W, HEADER_H, cat, null, null));
			y += HEADER_H;
			for (Module m : ModuleManager.get().getModules(cat)) {
				list.add(new Element(Kind.MODULE, x, y, PANEL_W, ROW_H, cat, m, null));
				y += ROW_H;
				if (!expanded.contains(m)) {
					continue;
				}
				for (Setting<?> s : m.getSettings()) {
					if (!s.isVisible()) {
						continue;
					}
					Kind kind = s instanceof BoolSetting ? Kind.BOOL : s instanceof NumberSetting ? Kind.NUMBER : Kind.MODE;
					int h = kind == Kind.NUMBER ? SLIDER_H : ROW_H;
					list.add(new Element(kind, x, y, PANEL_W, h, cat, m, s));
					y += h;
				}
				list.add(new Element(Kind.BIND, x, y, PANEL_W, ROW_H, cat, m, null));
				y += ROW_H;
			}
		}
		String label = "Edit HUD Layout";
		int bw = font.width(label) + 16;
		list.add(new Element(Kind.HUD_BUTTON, (this.width - bw) / 2, this.height - 32, bw, 16, null, null, null));
		int dw = font.width("Discord") + 16;
		list.add(new Element(Kind.DISCORD_BUTTON, (this.width + bw) / 2 + 6, this.height - 32, dw, 16, null, null, null));
		return list;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0x60000000); // plain dim instead of the vanilla blur
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		elements = layout();
		int accent = accent();
		Element hovered = null;

		for (Element e : elements) {
			boolean hover = e.contains(mouseX, mouseY);
			if (hover) {
				hovered = e;
			}
			switch (e.kind()) {
				case HEADER -> {
					g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), HEADER_BG);
					g.fill(e.x(), e.y() + e.h() - 1, e.x() + e.w(), e.y() + e.h(), accent);
					g.drawString(font, e.category().displayName, e.x() + 5, e.y() + 4, TEXT, true);
				}
				case MODULE -> {
					Module m = e.module();
					g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), BG);
					if (hover) {
						g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), ROW_HOVER);
					}
					if (m.isEnabled()) {
						g.fill(e.x(), e.y(), e.x() + 2, e.y() + e.h(), accent);
					}
					g.drawString(font, m.getName(), e.x() + 6, e.y() + 3, m.isEnabled() ? accent : TEXT, true);
					g.drawString(font, expanded.contains(m) ? "-" : "+", e.x() + e.w() - 9, e.y() + 3, MUTED, false);
				}
				case BOOL -> {
					BoolSetting s = (BoolSetting) e.setting();
					settingRow(g, e, hover);
					g.drawString(font, s.getName(), e.x() + 10, e.y() + 3, MUTED, false);
					int bx = e.x() + e.w() - 14;
					g.fill(bx, e.y() + 3, bx + 8, e.y() + 11, 0xFF2A2E35);
					if (s.get()) {
						g.fill(bx + 1, e.y() + 4, bx + 7, e.y() + 10, accent);
					}
				}
				case NUMBER -> {
					NumberSetting s = (NumberSetting) e.setting();
					settingRow(g, e, hover);
					g.drawString(font, s.getName(), e.x() + 10, e.y() + 2, MUTED, false);
					String v = s.display();
					g.drawString(font, v, e.x() + e.w() - 6 - font.width(v), e.y() + 2, TEXT, false);
					int sx = e.x() + 10, sw = e.w() - 16, sy = e.y() + 13;
					double frac = (s.get() - s.getMin()) / (s.getMax() - s.getMin());
					g.fill(sx, sy, sx + sw, sy + 3, 0xFF2A2E35);
					g.fill(sx, sy, sx + (int) (sw * frac), sy + 3, accent);
				}
				case MODE -> {
					ModeSetting s = (ModeSetting) e.setting();
					settingRow(g, e, hover);
					g.drawString(font, s.getName(), e.x() + 10, e.y() + 3, MUTED, false);
					g.drawString(font, s.get(), e.x() + e.w() - 6 - font.width(s.get()), e.y() + 3, TEXT, false);
				}
				case BIND -> {
					settingRow(g, e, hover);
					String key = listeningForKey == e.module() ? "§ePress a key..." : KeybindManager.keyName(e.module().getKey());
					g.drawString(font, "Bind", e.x() + 10, e.y() + 3, MUTED, false);
					g.drawString(font, key, e.x() + e.w() - 6 - font.width(key), e.y() + 3, TEXT, false);
				}
				case HUD_BUTTON -> {
					g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), hover ? accent : HEADER_BG);
					g.drawCenteredString(font, "Edit HUD Layout", e.x() + e.w() / 2, e.y() + 4, hover ? 0xFF000000 : TEXT);
				}
				case DISCORD_BUTTON -> {
					g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), hover ? 0xFF5865F2 : 0xF03C3F8C);
					g.drawCenteredString(font, "Discord", e.x() + e.w() / 2, e.y() + 4, TEXT);
				}
			}
		}

		g.drawString(font, "§7Left click: toggle  |  Right click: settings  |  Drag headers to move",
				6, this.height - 12, MUTED, false);
		if (hovered != null && hovered.kind() == Kind.MODULE) {
			g.setTooltipForNextFrame(font, Component.literal(hovered.module().getDescription()), mouseX, mouseY);
		}
	}

	private static void settingRow(GuiGraphics g, Element e, boolean hover) {
		g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), 0xE80D0F12);
		if (hover) {
			g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), ROW_HOVER);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int button = event.button();
		listeningForKey = null;
		// Iterate in reverse so the top-most (last drawn) element wins.
		for (int i = elements.size() - 1; i >= 0; i--) {
			Element e = elements.get(i);
			if (!e.contains(mx, my)) {
				continue;
			}
			switch (e.kind()) {
				case HEADER -> {
					draggingPanel = e.category();
					dragOffX = mx - e.x();
					dragOffY = my - e.y();
				}
				case MODULE -> {
					if (button == BUTTON_LEFT) {
						e.module().toggle();
					} else if (button == BUTTON_RIGHT && !expanded.remove(e.module())) {
						expanded.add(e.module());
					}
				}
				case BOOL -> ((BoolSetting) e.setting()).toggle();
				case NUMBER -> {
					draggingSlider = (NumberSetting) e.setting();
					sliderX = e.x() + 10;
					sliderW = e.w() - 16;
					updateSlider(mx);
				}
				case MODE -> ((ModeSetting) e.setting()).cycle(button != BUTTON_RIGHT);
				case BIND -> listeningForKey = e.module();
				case HUD_BUTTON -> this.minecraft.setScreen(new HudEditorScreen(this));
				case DISCORD_BUTTON -> ConfirmLinkScreen.confirmLinkNow(this, URI.create(PvPClient.DISCORD_URL));
			}
			ModuleManager.get().markDirty();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void updateSlider(double mx) {
		if (draggingSlider == null) {
			return;
		}
		double frac = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
		draggingSlider.set(draggingSlider.getMin() + frac * (draggingSlider.getMax() - draggingSlider.getMin()));
		ModuleManager.get().markDirty();
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingPanel != null) {
			int x = (int) Math.max(0, Math.min(this.width - PANEL_W, event.x() - dragOffX));
			int y = (int) Math.max(0, Math.min(this.height - HEADER_H, event.y() - dragOffY));
			panelPos.put(draggingPanel, new int[]{x, y});
			return true;
		}
		if (draggingSlider != null) {
			updateSlider(event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingPanel != null) {
			int[] p = panelPos.get(draggingPanel);
			PvPClient.config().setPanelPosition(draggingPanel.displayName, p[0], p[1]);
		}
		draggingPanel = null;
		draggingSlider = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningForKey != null) {
			int key = event.key();
			// Escape cancels, Delete/Backspace removes the bind.
			if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
				listeningForKey.setKey(Module.NO_KEY);
			} else if (key != InputConstants.KEY_ESCAPE) {
				listeningForKey.setKey(key);
			}
			listeningForKey = null;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		PvPClient.config().save();
		this.minecraft.setScreen(parent);
	}
}
