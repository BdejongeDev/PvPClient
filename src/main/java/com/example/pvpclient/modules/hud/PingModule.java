package com.example.pvpclient.modules.hud;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Latency to the server as reported in the player list. */
public final class PingModule extends HudModule {
	public PingModule() {
		super("Ping", "Latency to the server", Category.HUD, 2, 28);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		int ping = 0;
		if (mc.player != null && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) {
				ping = info.getLatency();
			}
		}
		String color = ping < 80 ? "§a" : ping < 160 ? "§e" : "§c";
		return panel(g, x, y, "Ping: " + color + ping + " ms");
	}
}
