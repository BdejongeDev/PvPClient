package com.example.pvpclient.input;

import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

/**
 * Toggles modules on their keybind. Keys are polled once per tick and only react on the
 * press edge, and only while no screen is open (so typing in chat never toggles modules).
 */
public final class KeybindManager {
	private final ModuleManager modules;
	private final Set<Integer> heldKeys = new HashSet<>();

	public KeybindManager(ModuleManager modules) {
		this.modules = modules;
	}

	public void onTick(Minecraft mc) {
		boolean canToggle = mc.screen == null;
		Set<Integer> nowHeld = new HashSet<>();
		for (Module module : modules.getModules()) {
			int key = module.getKey();
			if (key <= 0) {
				continue;
			}
			if (InputConstants.isKeyDown(mc.getWindow(), key)) {
				nowHeld.add(key);
				if (canToggle && !heldKeys.contains(key)) {
					module.toggle();
				}
			}
		}
		heldKeys.clear();
		heldKeys.addAll(nowHeld);
	}

	/** Human readable key name, e.g. "R" or "Right Shift". */
	public static String keyName(int key) {
		if (key <= 0) {
			return "None";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
	}
}
