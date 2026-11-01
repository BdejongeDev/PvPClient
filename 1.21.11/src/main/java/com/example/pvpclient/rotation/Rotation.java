package com.example.pvpclient.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Immutable yaw/pitch pair in degrees, using Minecraft's conventions. */
public record Rotation(float yaw, float pitch) {
	public static final float MIN_PITCH = -90.0F;
	public static final float MAX_PITCH = 90.0F;

	/** Rotation that looks from {@code from} towards {@code to}. Yaw is normalized to [-180, 180). */
	public static Rotation between(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		// Minecraft yaw 0 faces +Z, and increases clockwise when viewed from above.
		float yaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
		float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
		return new Rotation(yaw, Mth.clamp(pitch, MIN_PITCH, MAX_PITCH));
	}

	/** Angular distance in degrees, taking yaw wrap-around into account. */
	public double distanceTo(Rotation other) {
		float dYaw = Mth.wrapDegrees(other.yaw - yaw);
		float dPitch = other.pitch - pitch;
		return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
	}
}
