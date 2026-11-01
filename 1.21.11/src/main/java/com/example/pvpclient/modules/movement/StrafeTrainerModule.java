package com.example.pvpclient.modules.movement;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.NumberSetting;
import com.example.pvpclient.util.CombatTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Practice tool: while fighting, measures how much you strafe and how often you switch
 * direction, and tells you when you have been strafing one way for too long.
 */
public final class StrafeTrainerModule extends HudModule {
	private static final int WINDOW_TICKS = 100; // 5 seconds

	private final NumberSetting combatRange = setting(new NumberSetting("Combat Range", 6, 2, 12, 0.5));
	private final NumberSetting switchAfter = setting(new NumberSetting("Switch Hint (ticks)", 20, 5, 60, 1));

	/** Per tick in the window: -1 left, 1 right, 0 no strafe. */
	private final Deque<Integer> history = new ArrayDeque<>();
	private int currentDir, sameDirTicks;
	private boolean inCombat;

	public StrafeTrainerModule() {
		super("StrafeTrainer", "Feedback on your strafing in fights", Category.MOVEMENT, RIGHT, 90);
		on(Events.Tick.class, e -> onTick());
	}

	private void onTick() {
		if (!inGame()) {
			return;
		}
		LivingEntity target = CombatTracker.get().getTarget();
		inCombat = target != null && target.distanceTo(mc.player) <= combatRange.get();
		if (!inCombat) {
			return;
		}
		boolean left = mc.player.input.keyPresses.left(), right = mc.player.input.keyPresses.right();
		int dir = left == right ? 0 : (left ? -1 : 1);
		sameDirTicks = dir != 0 && dir == currentDir ? sameDirTicks + 1 : 0;
		currentDir = dir;
		history.addLast(dir);
		while (history.size() > WINDOW_TICKS) {
			history.pollFirst();
		}
	}

	@Override
	protected int[] draw(GuiGraphics g, int x, int y, boolean preview) {
		if (!inCombat && !preview && history.isEmpty()) {
			return new int[]{0, 0};
		}
		int strafing = 0, switches = 0, prev = 0;
		for (int d : history) {
			if (d != 0) {
				strafing++;
				if (prev != 0 && d != prev) {
					switches++;
				}
				prev = d;
			}
		}
		int pct = history.isEmpty() ? 0 : strafing * 100 / history.size();
		String arrow = currentDir < 0 ? "←" : currentDir > 0 ? "→" : "-";
		String hint = !inCombat ? "§7(out of combat)"
				: sameDirTicks >= switchAfter.intValue() ? "§c§lSwitch direction!"
				: pct < 50 ? "§eStrafe more" : "§aGood";
		String pctColor = pct >= 70 ? "§a" : pct >= 40 ? "§e" : "§c";
		return panel(g, x, y,
				"Strafe: " + pctColor + pct + "% §f" + arrow,
				"Switches (5s): §b" + switches,
				hint);
	}

	@Override
	protected void onDisable() {
		history.clear();
		sameDirTicks = 0;
	}
}
