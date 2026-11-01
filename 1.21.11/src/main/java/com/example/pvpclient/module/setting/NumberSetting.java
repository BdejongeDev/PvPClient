package com.example.pvpclient.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Numeric setting clamped to [min, max] and snapped to {@code step}. */
public final class NumberSetting extends Setting<Double> {
	private final double min, max, step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	@Override
	public void set(Double v) {
		if (v == null || v.isNaN() || v.isInfinite()) {
			return;
		}
		double snapped = Math.round(v / step) * step;
		super.set(Math.max(min, Math.min(max, snapped)));
	}

	public double getMin() { return min; }
	public double getMax() { return max; }
	public double getStep() { return step; }
	public int intValue() { return (int) Math.round(value); }
	public float floatValue() { return value.floatValue(); }

	/** Value formatted with as many decimals as the step needs. */
	public String display() {
		return step >= 1 ? String.valueOf(intValue()) : String.format(step >= 0.1 ? "%.1f" : "%.2f", value);
	}

	@Override
	public JsonElement toJson() { return new JsonPrimitive(value); }

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsDouble());
		}
	}
}
