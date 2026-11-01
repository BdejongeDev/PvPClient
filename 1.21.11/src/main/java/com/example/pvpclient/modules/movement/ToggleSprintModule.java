package com.example.pvpclient.modules.movement;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/** Press the sprint key once to start sprinting, press again to stop. */
public final class ToggleSprintModule extends Module {
	private boolean sprintToggled;
	private boolean wasPressed;

	public ToggleSprintModule() {
		super("ToggleSprint", "Sprint key toggles sprinting", Category.MOVEMENT);
		on(Events.Tick.class, e -> onTick());
	}

	private void onTick() {
		if (!inGame()) {
			return;
		}
		// Read the physical key: the KeyMapping itself is overwritten by setDown below.
		InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mc.options.keySprint);
		boolean pressed = bound.getType() == InputConstants.Type.KEYSYM
				&& mc.screen == null
				&& InputConstants.isKeyDown(mc.getWindow(), bound.getValue());
		if (pressed && !wasPressed) {
			sprintToggled = !sprintToggled;
		}
		wasPressed = pressed;
		mc.options.keySprint.setDown(sprintToggled);
	}

	public boolean isSprintToggled() {
		return isEnabled() && sprintToggled;
	}

	@Override
	protected void onDisable() {
		sprintToggled = false;
		mc.options.keySprint.setDown(false);
	}
}
