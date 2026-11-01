package com.example.pvpclient.modules.movement;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;

/**
 * Keeps the sprint key logically held. Vanilla still decides whether sprinting is allowed
 * (hunger, sneaking, using items, walking backwards), so this never sprints illegally.
 */
public final class AutoSprintModule extends Module {
	public AutoSprintModule() {
		super("AutoSprint", "Always sprint when moving forward", Category.MOVEMENT);
		on(Events.Tick.class, e -> {
			if (inGame() && mc.player.input.hasForwardImpulse()) {
				mc.options.keySprint.setDown(true);
			}
		});
	}

	@Override
	protected void onDisable() {
		mc.options.keySprint.setDown(false);
	}
}
