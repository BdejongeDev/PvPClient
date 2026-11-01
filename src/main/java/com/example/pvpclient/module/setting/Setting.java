package com.example.pvpclient.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** A single configurable value of a module. Serialized to JSON by the ConfigManager. */
public abstract class Setting<T> {
	private final String name;
	private final T defaultValue;
	protected T value;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() { return name; }
	public T get() { return value; }
	public T getDefault() { return defaultValue; }

	public void set(T value) {
		this.value = value;
	}

	/** Only show this setting in the ClickGUI when the supplier returns true. */
	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
		this.visibility = supplier;
		return (S) this;
	}

	public boolean isVisible() { return visibility.getAsBoolean(); }

	public abstract JsonElement toJson();

	/** Must tolerate malformed input: fall back to the current value. */
	public abstract void fromJson(JsonElement json);
}
