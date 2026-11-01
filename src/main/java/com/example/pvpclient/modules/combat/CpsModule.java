package com.example.pvpclient.modules.combat;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.util.CpsTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Left-click clicks per second. */
public final class CpsModule extends HudModule {
	public CpsModule() {
		super("CPS", "Clicks per second counter", Category.COMBAT, 2, 2);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		return panel(g, x, y, "CPS: §e" + CpsTracker.get().getCps());
	}
}
