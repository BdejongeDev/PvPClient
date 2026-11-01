package com.example.pvpclient.modules.hud;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FpsModule extends HudModule {
	public FpsModule() {
		super("FPS", "Frames per second", Category.HUD, 2, 15);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		return panel(g, x, y, "FPS: §a" + mc.getFps());
	}
}
