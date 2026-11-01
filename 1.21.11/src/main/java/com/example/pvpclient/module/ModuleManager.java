package com.example.pvpclient.module;

import com.example.pvpclient.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Owns all modules and wires their listeners to the event bus. Every call into a module is
 * wrapped so that an exception disables that one module instead of crashing the game.
 */
public final class ModuleManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("pvpclient");
	private static ModuleManager instance;

	private final EventBus bus;
	private final List<Module> modules = new ArrayList<>();
	private boolean dirty;

	public ModuleManager(EventBus bus) {
		this.bus = bus;
		instance = this;
	}

	public static ModuleManager get() {
		return instance;
	}

	public void register(Module module) {
		modules.add(module);
		for (Module.Listener<?> listener : module.listeners) {
			subscribe(module, listener);
		}
	}

	private <E> void subscribe(Module module, Module.Listener<E> listener) {
		bus.subscribe(listener.type(), event -> {
			if (module.isEnabled()) {
				runSafely(module, () -> listener.handler().accept(event));
			}
		});
	}

	/** Runs HUD drawing for a module with the same crash protection as event handlers. */
	public void renderSafely(Module module, Runnable action) {
		runSafely(module, action);
	}

	void runSafely(Module module, Runnable action) {
		try {
			action.run();
		} catch (Throwable t) {
			LOGGER.error("Module {} failed and was disabled", module.getName(), t);
			module.setEnabledSilently(false);
			markDirty();
		}
	}

	/**
	 * Restores the enabled state from config without calling onEnable: the game is still
	 * starting up, so modules apply side effects lazily from their tick handlers.
	 */
	public void loadEnabled(Module module, boolean enabled) {
		module.setEnabledSilently(enabled);
	}

	public List<Module> getModules() { return Collections.unmodifiableList(modules); }

	public List<Module> getModules(Category category) {
		return modules.stream().filter(m -> m.getCategory() == category).toList();
	}

	public List<HudModule> getHudModules() {
		return modules.stream().filter(m -> m instanceof HudModule).map(m -> (HudModule) m).toList();
	}

	@SuppressWarnings("unchecked")
	public <M extends Module> M get(Class<M> type) {
		for (Module m : modules) {
			if (type.isInstance(m)) {
				return (M) m;
			}
		}
		throw new IllegalArgumentException("Module not registered: " + type.getName());
	}

	public Optional<Module> byName(String name) {
		return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
	}

	public void markDirty() { dirty = true; }

	public boolean consumeDirty() {
		boolean d = dirty;
		dirty = false;
		return d;
	}
}
