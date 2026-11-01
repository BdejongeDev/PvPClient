package com.example.pvpclient.config;

import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.module.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists every module (enabled, keybind, settings, HUD position) and the ClickGUI panel
 * positions to {@code config/pvpclient/client.json}.
 *
 * <pre>
 * { "modules": { "AimAssist": { "enabled": true, "key": 86, "settings": {...}, "x": 2, "y": 2 } },
 *   "gui": { "Combat": [10, 10] } }
 * </pre>
 */
public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("pvpclient");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path = FabricLoader.getInstance().getConfigDir().resolve("pvpclient").resolve("client.json");
	private final ModuleManager modules;
	/** ClickGUI panel positions keyed by category name. */
	private final Map<String, int[]> panelPositions = new HashMap<>();

	public ConfigManager(ModuleManager modules) {
		this.modules = modules;
	}

	public void load() {
		if (!Files.exists(path)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			for (Module module : modules.getModules()) {
				if (mods.has(module.getName()) && mods.get(module.getName()).isJsonObject()) {
					readModule(module, mods.getAsJsonObject(module.getName()));
				}
			}
			if (root.has("gui") && root.get("gui").isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("gui").entrySet()) {
					var arr = e.getValue().getAsJsonArray();
					panelPositions.put(e.getKey(), new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()});
				}
			}
		} catch (Exception e) {
			// Corrupt or hand-edited badly: keep defaults and rewrite the file.
			LOGGER.warn("Could not read {}, using defaults", path, e);
		}
		modules.consumeDirty();
		save();
	}

	private void readModule(Module module, JsonObject json) {
		try {
			if (json.has("key")) {
				module.setKey(json.get("key").getAsInt());
			}
			if (json.has("settings") && json.get("settings").isJsonObject()) {
				JsonObject settings = json.getAsJsonObject("settings");
				for (Setting<?> setting : module.getSettings()) {
					setting.fromJson(settings.get(setting.getName()));
				}
			}
			if (module instanceof HudModule hud && json.has("x") && json.has("y")) {
				hud.setPosition(json.get("x").getAsInt(), json.get("y").getAsInt());
			}
			if (json.has("enabled")) {
				ModuleManager.get().loadEnabled(module, json.get("enabled").getAsBoolean());
			}
		} catch (Exception e) {
			LOGGER.warn("Invalid config for module {}, partially using defaults", module.getName(), e);
		}
	}

	public void save() {
		JsonObject root = new JsonObject();
		JsonObject mods = new JsonObject();
		for (Module module : modules.getModules()) {
			JsonObject json = new JsonObject();
			json.addProperty("enabled", module.isEnabled());
			json.addProperty("key", module.getKey());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				settings.add(setting.getName(), setting.toJson());
			}
			json.add("settings", settings);
			if (module instanceof HudModule hud) {
				json.addProperty("x", hud.getX());
				json.addProperty("y", hud.getY());
			}
			mods.add(module.getName(), json);
		}
		root.add("modules", mods);
		JsonObject gui = new JsonObject();
		panelPositions.forEach((name, pos) -> {
			var arr = new com.google.gson.JsonArray();
			arr.add(pos[0]);
			arr.add(pos[1]);
			gui.add(name, arr);
		});
		root.add("gui", gui);

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Could not save {}", path, e);
		}
	}

	/** Saves only when something changed since the last save. Called every tick. */
	public void saveIfDirty() {
		if (modules.consumeDirty()) {
			save();
		}
	}

	public int[] getPanelPosition(String category) { return panelPositions.get(category); }

	public void setPanelPosition(String category, int x, int y) {
		panelPositions.put(category, new int[]{x, y});
		modules.markDirty();
	}
}
