package com.example.pvpclient.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** One of a fixed list of string options. */
public final class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String defaultValue, String... modes) {
		super(name, defaultValue);
		this.modes = List.of(modes);
	}

	public List<String> getModes() { return modes; }
	public boolean is(String mode) { return value.equals(mode); }

	public void cycle(boolean forward) {
		int i = modes.indexOf(value);
		int next = Math.floorMod(i + (forward ? 1 : -1), modes.size());
		set(modes.get(next));
	}

	@Override
	public void set(String v) {
		if (modes == null || modes.contains(v)) { // modes is null during super constructor
			super.set(v);
		}
	}

	@Override
	public JsonElement toJson() { return new JsonPrimitive(value); }

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) {
			set(json.getAsString());
		}
	}
}
