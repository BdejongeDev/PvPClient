package com.example.pvpclient.modules.render;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.ModeSetting;
import com.example.pvpclient.util.CombatTracker;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

/** Draws a marker above the entity you are currently fighting (only while visible). */
public final class TargetIndicatorModule extends Module {
	private final ModeSetting color = setting(new ModeSetting("Color", "Red", "Red", "Yellow", "Aqua", "White"));

	private boolean targetVisible;

	public TargetIndicatorModule() {
		super("TargetIndicator", "Marker above your current target", Category.RENDER);
		on(Events.Tick.class, e -> {
			LivingEntity t = CombatTracker.get().getTarget();
			targetVisible = t != null && RenderUtil.canSee(t);
		});
		on(Events.WorldOverlay.class, this::draw);
	}

	private int argb() {
		return switch (color.get()) {
			case "Yellow" -> 0xFFFFD640;
			case "Aqua" -> 0xFF40E0FF;
			case "White" -> 0xFFFFFFFF;
			default -> 0xFFFF4040;
		};
	}

	private void draw(Events.WorldOverlay e) {
		LivingEntity target = CombatTracker.get().getTarget();
		if (target == null || !targetVisible) {
			return;
		}
		double[] pos = RenderUtil.worldToScreen(e.graphics(), RenderUtil.aboveHead(target, e.partialTick(), 0.35));
		if (pos == null) {
			return;
		}
		GuiGraphicsExtractor g = e.graphics();
		int cx = (int) pos[0], cy = (int) pos[1];
		// Bobbing downward-pointing triangle built from horizontal lines.
		int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 150.0) * 2);
		int col = argb();
		for (int row = 0; row < 6; row++) {
			int half = 6 - row;
			g.fill(cx - half, cy - 12 + row + bob, cx + half, cy - 11 + row + bob, col);
		}
	}
}
