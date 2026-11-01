package com.example.pvpclient.modules.hud;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphics;

public final class CoordinatesModule extends HudModule {
	private final BoolSetting showDirection = setting(new BoolSetting("Direction", true));

	public CoordinatesModule() {
		super("Coordinates", "XYZ position and facing", Category.HUD, 2, 41);
	}

	@Override
	protected int[] draw(GuiGraphics g, int x, int y, boolean preview) {
		if (mc.player == null) {
			return panel(g, x, y, "XYZ: 0 0 0");
		}
		String xyz = String.format("XYZ: §b%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
		if (!showDirection.get()) {
			return panel(g, x, y, xyz);
		}
		String dir = mc.player.getDirection().getName();
		return panel(g, x, y, xyz, "Facing: §e" + Character.toUpperCase(dir.charAt(0)) + dir.substring(1));
	}
}
