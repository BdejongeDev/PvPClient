package com.example.pvpclient.util;

import com.example.pvpclient.event.EventBus;
import com.example.pvpclient.event.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.Nullable;

/**
 * Shared combat statistics (hits, misses, combo, current target). Always running so HUD
 * modules can be toggled at any time without losing data.
 */
public final class CombatTracker {
	/** Combo ends when no hit has landed for this long. */
	private static final long COMBO_TIMEOUT_MS = 2500;
	/** How long the last attacked entity stays the current target. */
	private static final long TARGET_TIMEOUT_MS = 5000;
	private static final double TARGET_MAX_DISTANCE = 12.0;

	private static CombatTracker instance;

	private int hits, misses, combo, bestCombo;
	private long lastHitMs;
	@Nullable
	private LivingEntity lastTarget;
	private int lastHurtTime;

	public CombatTracker(EventBus bus) {
		instance = this;
		bus.subscribe(Events.AttackEntity.class, this::onAttack);
		bus.subscribe(Events.AttackClick.class, e -> {
			if (e.missed()) {
				misses += e.clicks();
			}
		});
		bus.subscribe(Events.Tick.class, e -> onTick(e.mc()));
	}

	public static CombatTracker get() {
		return instance;
	}

	private void onAttack(Events.AttackEntity event) {
		if (!(event.target() instanceof LivingEntity living) || living instanceof ArmorStand) {
			return;
		}
		long now = System.currentTimeMillis();
		hits++;
		combo = living == lastTarget && now - lastHitMs < COMBO_TIMEOUT_MS ? combo + 1 : 1;
		bestCombo = Math.max(bestCombo, combo);
		lastTarget = living;
		lastHitMs = now;
	}

	private void onTick(Minecraft mc) {
		if (mc.player == null) {
			return;
		}
		// Getting hit (hurtTime jumps up) breaks the combo.
		int hurt = mc.player.hurtTime;
		if (hurt > lastHurtTime) {
			combo = 0;
		}
		lastHurtTime = hurt;
		if (System.currentTimeMillis() - lastHitMs > COMBO_TIMEOUT_MS) {
			combo = 0;
		}
	}

	/** The entity being fought: last attacked (if recent and valid), else the one under the crosshair. */
	@Nullable
	public LivingEntity getTarget() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		if (lastTarget != null && isValid(mc, lastTarget)
				&& System.currentTimeMillis() - lastHitMs < TARGET_TIMEOUT_MS) {
			return lastTarget;
		}
		Entity looked = mc.crosshairPickEntity;
		if (looked instanceof LivingEntity living && !(living instanceof ArmorStand) && isValid(mc, living)) {
			return living;
		}
		return null;
	}

	private static boolean isValid(Minecraft mc, LivingEntity e) {
		return e.isAlive() && !e.isRemoved() && e.level() == mc.player.level()
				&& e.distanceTo(mc.player) <= TARGET_MAX_DISTANCE;
	}

	public void reset() {
		hits = misses = combo = bestCombo = 0;
	}

	public int getHits() { return hits; }
	public int getMisses() { return misses; }
	public int getCombo() { return combo; }
	public int getBestCombo() { return bestCombo; }

	public double getAccuracy() {
		int total = hits + misses;
		return total == 0 ? 0 : hits * 100.0 / total;
	}
}
