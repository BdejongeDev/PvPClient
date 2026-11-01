package com.example.pvpclient.modules.combat;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.util.CombatTracker;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Training aid for attack timing. Shows the weapon charge as a bar under the crosshair,
 * optionally ticks when the next hit will do full damage, and tracks how many of your hits
 * were fully charged. It never attacks for you.
 */
public final class HitTimingModule extends HudModule {
	/** Charge at which a hit counts as "full" (vanilla crits/sweeps need > 0.9). */
	private static final float FULL = 0.95F;
	private static final int BAR_W = 40, BAR_H = 3;

	private final BoolSetting showBar = setting(new BoolSetting("Crosshair Bar", true));
	private final BoolSetting sound = setting(new BoolSetting("Ready Sound", true));
	private final BoolSetting soundOnlyInCombat = setting(new BoolSetting("Sound Only In Combat", true));
	private final BoolSetting showStats = setting(new BoolSetting("Show Stats", true));

	private int fullHits, earlyHits;
	private float lastHitCharge = -1, chargeSum;
	private boolean wasReady = true;

	public HitTimingModule() {
		super("HitTiming", "Learn to time your hits: charge bar, ready sound and stats", Category.COMBAT, 2, 140);
		on(Events.AttackEntity.class, e -> onAttack());
		on(Events.Tick.class, e -> onTick());
		on(Events.WorldOverlay.class, this::drawBar);
	}

	private float charge(float partialTick) {
		return mc.player.getAttackStrengthScale(partialTick);
	}

	private void onAttack() {
		// Fired before vanilla resets the cooldown, so this is the charge the hit was made with.
		float c = charge(0.5F);
		lastHitCharge = c;
		chargeSum += c;
		if (c >= FULL) {
			fullHits++;
		} else {
			earlyHits++;
		}
	}

	private void onTick() {
		if (!inGame()) {
			return;
		}
		boolean ready = charge(0) >= FULL;
		if (ready && !wasReady && sound.get()
				&& (!soundOnlyInCombat.get() || CombatTracker.get().getTarget() != null)) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F, 0.6F));
		}
		wasReady = ready;
	}

	private void drawBar(Events.WorldOverlay e) {
		if (!showBar.get() || mc.screen != null) {
			return;
		}
		float c = charge(e.partialTick());
		boolean inCombat = CombatTracker.get().getTarget() != null;
		if (c >= 1 && !inCombat) {
			return; // nothing to learn while idle and fully charged
		}
		GuiGraphics g = e.graphics();
		int x = g.guiWidth() / 2 - BAR_W / 2, y = g.guiHeight() / 2 + 9;
		g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xA0000000);
		int color = c >= FULL ? 0xFF4ADE80 : RenderUtil.healthColor(c * 0.8F);
		g.fill(x, y, x + (int) (BAR_W * c), y + BAR_H, color);
		// Tick mark at the "full damage" point.
		int mark = x + (int) (BAR_W * FULL);
		g.fill(mark, y - 1, mark + 1, y + BAR_H + 1, 0xFFFFFFFF);
	}

	@Override
	protected int[] draw(GuiGraphics g, int x, int y, boolean preview) {
		if (!showStats.get()) {
			return new int[]{0, 0};
		}
		int total = fullHits + earlyHits;
		List<String> lines = new ArrayList<>();
		int pct = total == 0 ? 0 : fullHits * 100 / total;
		String color = pct >= 80 ? "§a" : pct >= 50 ? "§e" : "§c";
		lines.add("Timing: " + color + pct + "% §7full hits");
		lines.add("Full: §a" + fullHits + " §fToo early: §c" + earlyHits);
		if (lastHitCharge >= 0) {
			String c = lastHitCharge >= FULL ? "§a" : "§c";
			lines.add(String.format("Last hit: %s%.0f%% §7(avg %.0f%%)", c, lastHitCharge * 100, chargeSum / total * 100));
		}
		if (mc.player != null) {
			float delay = mc.player.getCurrentItemAttackStrengthDelay() / 20F;
			lines.add(String.format("§7Weapon cooldown: %.2fs", delay));
		}
		return panel(g, x, y, lines.toArray(String[]::new));
	}

	/** Clear the statistics (used when re-enabling the module). */
	@Override
	protected void onEnable() {
		fullHits = earlyHits = 0;
		chargeSum = 0;
		lastHitCharge = -1;
	}
}
