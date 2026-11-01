package com.example.pvpclient.modules.misc;

import com.example.pvpclient.gui.ClickGuiScreen;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.ModeSetting;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Opening the ClickGUI is modelled as a module so it gets a rebindable key like every other
 * module (default Right Shift). Enabling opens the screen and immediately switches back off.
 */
public final class ClickGuiModule extends Module {
	private final ModeSetting accent = setting(new ModeSetting("Accent", "Green", "Green", "Blue", "Red", "Purple", "Orange"));

	public ClickGuiModule() {
		super("ClickGUI", "Open this menu", Category.MISC);
		setKeySilently(InputConstants.KEY_RSHIFT);
	}

	@Override
	protected void onEnable() {
		if (mc.screen == null) {
			mc.setScreen(new ClickGuiScreen(null));
		}
		setEnabled(false);
	}

	public int accentColor() {
		return switch (accent.get()) {
			case "Blue" -> 0xFF3B82F6;
			case "Red" -> 0xFFEF4444;
			case "Purple" -> 0xFFA855F7;
			case "Orange" -> 0xFFF59E0B;
			default -> 0xFF4ADE80;
		};
	}
}
