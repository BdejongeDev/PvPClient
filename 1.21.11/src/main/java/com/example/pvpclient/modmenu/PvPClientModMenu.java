package com.example.pvpclient.modmenu;

import com.example.pvpclient.gui.ClickGuiScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu entrypoint (registered as "modmenu" in fabric.mod.json): opens the ClickGUI. */
public final class PvPClientModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ClickGuiScreen::new;
	}
}
