package com.example.pvpclient.modules.player;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.DeathScreen;

/** Respawns automatically after a short, configurable delay. */
public final class AutoRespawnModule extends Module {
	private final NumberSetting delay = setting(new NumberSetting("Delay (ticks)", 10, 0, 100, 1));
	private int deadTicks;

	public AutoRespawnModule() {
		super("AutoRespawn", "Respawn automatically on death", Category.PLAYER);
		on(Events.Tick.class, e -> {
			if (inGame() && mc.screen instanceof DeathScreen) {
				if (++deadTicks >= delay.intValue()) {
					mc.player.respawn();
					mc.setScreen(null);
					deadTicks = 0;
				}
			} else {
				deadTicks = 0;
			}
		});
	}
}
