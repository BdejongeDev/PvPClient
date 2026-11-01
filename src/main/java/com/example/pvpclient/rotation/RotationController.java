package com.example.pvpclient.rotation;

import net.minecraft.util.Mth;

/**
 * Computes smooth, rate-limited rotation steps towards a target rotation.
 * Stateless: it only knows about rotations, never about entities.
 */
public final class RotationController {
	/** Fraction of the remaining angle covered per tick at 100% strength. */
	private static final float MAX_SMOOTHING_PER_TICK = 0.6F;
	/** Below this angular error (degrees) we stop moving, which prevents micro-jitter. */
	private static final float DEADZONE_DEGREES = 0.15F;

	/**
	 * Returns the next rotation after {@code elapsedTicks} ticks (can be fractional, for per-frame updates).
	 *
	 * @param strength     1-100 %, how aggressively the remaining error is closed
	 * @param maxPerTick   hard cap on rotation, degrees per tick
	 */
	public Rotation step(Rotation current, Rotation target, int strength, double maxPerTick, float elapsedTicks) {
		if (elapsedTicks <= 0.0F) {
			return current;
		}

		// Shortest signed yaw difference, normalized to [-180, 180).
		float dYaw = Mth.wrapDegrees(target.yaw() - current.yaw());
		float dPitch = Mth.clamp(target.pitch(), Rotation.MIN_PITCH, Rotation.MAX_PITCH) - current.pitch();
		float error = Mth.sqrt(dYaw * dYaw + dPitch * dPitch);
		if (error < DEADZONE_DEGREES) {
			return current;
		}

		// Frame-rate independent exponential smoothing: the same fraction is covered per
		// tick regardless of how many frames that tick is split into.
		float perTick = MAX_SMOOTHING_PER_TICK * Mth.clamp(strength, 1, 100) / 100.0F;
		float fraction = 1.0F - (float) Math.pow(1.0F - perTick, elapsedTicks);

		float move = error * fraction;
		move = Math.min(move, (float) (maxPerTick * elapsedTicks)); // rate limit
		move = Math.min(move, error);                                // never overshoot

		float scale = move / error;
		// Yaw is accumulated (not wrapped) so it stays continuous with the player's own yaw.
		float yaw = current.yaw() + dYaw * scale;
		float pitch = Mth.clamp(current.pitch() + dPitch * scale, Rotation.MIN_PITCH, Rotation.MAX_PITCH);
		return new Rotation(yaw, pitch);
	}
}
