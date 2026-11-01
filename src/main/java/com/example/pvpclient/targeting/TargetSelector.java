package com.example.pvpclient.targeting;

import com.example.pvpclient.rotation.Rotation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Finds the best entity to aim at. Knows nothing about how the camera is rotated.
 */
public final class TargetSelector {
	/** Which entity types may be targeted. */
	public enum Filter { PLAYERS, HOSTILES, ALL }

	/** Targeting parameters, supplied by the module that uses the selector. */
	public record Options(double range, double fov, Filter filter, boolean visibilityCheck) {}

	private static final double ANGLE_WEIGHT = 0.6;
	private static final double DISTANCE_WEIGHT = 0.4;
	/** A new target must score this much better (relative) than the current one before we switch. */
	private static final double SWITCH_MARGIN = 0.25;
	/** Shrinks the hitbox used as aim area so we aim slightly inside the edges. */
	private static final double HITBOX_SHRINK = 0.1;

	/**
	 * Returns the best target, preferring {@code current} unless another candidate is clearly better.
	 * Returns {@code null} when nothing (including {@code current}) is valid.
	 */
	@Nullable
	public LivingEntity select(LocalPlayer player, Options config, @Nullable LivingEntity current) {
		Level level = player.level();
		double range = config.range();

		// 1. Collect candidates in a box around the player; 2. filter to valid targets.
		List<LivingEntity> candidates = level.getEntitiesOfClass(
				LivingEntity.class,
				player.getBoundingBox().inflate(range + 1.0),
				entity -> isValid(player, entity, config));

		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		double currentScore = Double.MAX_VALUE;

		for (LivingEntity entity : candidates) {
			// 3. + 4. FOV/range check and scoring.
			double score = score(player, entity, config, 1.0F);
			if (Double.isNaN(score)) {
				continue;
			}
			if (entity == current) {
				currentScore = score;
			}
			if (score < bestScore) {
				bestScore = score;
				best = entity;
			}
		}

		// Hysteresis: keep the current target while it is valid and not clearly beaten.
		// This prevents flip-flopping (jitter) between targets standing close together.
		if (currentScore != Double.MAX_VALUE && bestScore > currentScore * (1.0 - SWITCH_MARGIN)) {
			return current;
		}
		// 5. Best target (or null).
		return best;
	}

	/** Whether {@code target} is still valid, in range and inside the FOV. Cheap enough to call every frame. */
	public boolean isStillValid(LocalPlayer player, LivingEntity target, Options config, float partialTick) {
		return target.level() == player.level()
				&& isValid(player, target, config)
				&& !Double.isNaN(score(player, target, config, partialTick));
	}

	private boolean isValid(LocalPlayer player, LivingEntity entity, Options config) {
		if (entity == player || !entity.isAlive() || entity.isRemoved() || entity.isDeadOrDying()) {
			return false;
		}
		if (entity.isSpectator() || entity instanceof ArmorStand) {
			return false;
		}
		boolean typeAllowed = switch (config.filter()) {
			case PLAYERS -> entity instanceof Player;
			case HOSTILES -> entity instanceof Player || entity instanceof Enemy;
			case ALL -> true;
		};
		if (!typeAllowed) {
			return false;
		}
		return !config.visibilityCheck() || player.hasLineOfSight(entity);
	}

	/** Lower is better. Returns NaN when the entity is out of range or outside the FOV. */
	private double score(LocalPlayer player, LivingEntity entity, Options config, float partialTick) {
		Vec3 eyes = player.getEyePosition(partialTick);
		Vec3 aim = aimPoint(player, entity, partialTick);

		double distance = eyes.distanceTo(aim);
		if (distance > config.range()) {
			return Double.NaN;
		}

		double halfFov = config.fov() / 2.0;
		Rotation view = new Rotation(player.getViewYRot(partialTick), player.getViewXRot(partialTick));
		double angle = view.distanceTo(Rotation.between(eyes, aim));
		if (angle > halfFov) {
			return Double.NaN;
		}

		return ANGLE_WEIGHT * (angle / halfFov) + DISTANCE_WEIGHT * (distance / config.range());
	}

	/**
	 * Point on the target's (slightly shrunk) hitbox closest to where the crosshair currently points.
	 * If the crosshair is already on the hitbox, this is exactly the crosshair point, so no rotation is needed.
	 */
	public Vec3 aimPoint(LocalPlayer player, LivingEntity target, float partialTick) {
		// Hitboxes are updated per tick; offset by the interpolated position for smooth per-frame aiming.
		Vec3 offset = target.getPosition(partialTick).subtract(target.position());
		AABB box = target.getBoundingBox().move(offset);
		box = box.deflate(box.getXsize() * HITBOX_SHRINK, box.getYsize() * HITBOX_SHRINK, box.getZsize() * HITBOX_SHRINK);

		Vec3 eyes = player.getEyePosition(partialTick);
		double distance = eyes.distanceTo(box.getCenter());
		Vec3 lookPoint = eyes.add(player.getViewVector(partialTick).scale(distance));
		return new Vec3(
				clamp(lookPoint.x, box.minX, box.maxX),
				clamp(lookPoint.y, box.minY, box.maxY),
				clamp(lookPoint.z, box.minZ, box.maxZ));
	}

	private static double clamp(double value, double min, double max) {
		return value < min ? min : Math.min(value, max);
	}
}
