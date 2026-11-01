package com.example.pvpclient.module;

import com.example.pvpclient.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for every feature. A module declares its settings and event listeners in its
 * constructor; the {@link ModuleManager} only delivers events while the module is enabled.
 */
public abstract class Module {
	/** Key code meaning "no keybind". */
	public static final int NO_KEY = -1;

	protected final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	final List<Listener<?>> listeners = new ArrayList<>();

	private boolean enabled;
	private int key = NO_KEY;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <S extends Setting<?>> S setting(S setting) {
		settings.add(setting);
		return setting;
	}

	/** Registers an event handler that only runs while this module is enabled. */
	protected <E> void on(Class<E> type, Consumer<E> handler) {
		listeners.add(new Listener<>(type, handler));
	}

	record Listener<E>(Class<E> type, Consumer<E> handler) {}

	protected void onEnable() {}

	protected void onDisable() {}

	/** Enable/disable; lifecycle exceptions are contained by the ModuleManager. */
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		ModuleManager.get().runSafely(this, enabled ? this::onEnable : this::onDisable);
		ModuleManager.get().markDirty();
	}

	public void toggle() { setEnabled(!enabled); }

	public boolean isEnabled() { return enabled; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Category getCategory() { return category; }
	public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
	public int getKey() { return key; }

	/** Default keybind set in a constructor, before the ModuleManager exists. */
	protected void setKeySilently(int key) {
		this.key = key;
	}

	public void setKey(int key) {
		this.key = key;
		ModuleManager.get().markDirty();
	}

	/** Enabled state as loaded from config, without firing onEnable before the world exists. */
	void setEnabledSilently(boolean enabled) { this.enabled = enabled; }

	/** True when there is a player and level to act on. */
	protected boolean inGame() {
		return mc.player != null && mc.level != null;
	}
}
