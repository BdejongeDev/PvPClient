package com.example.pvpclient.modules.combat;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.util.CombatTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Shows current/best combo, total hits, misses and accuracy. */
public final class ComboCounterModule extends HudModule {
	private final BoolSetting showStats = setting(new BoolSetting("Show Hits/Misses", true));
	private final BoolSetting showAccuracy = setting(new BoolSetting("Show Accuracy", true));

	public ComboCounterModule() {
		super("ComboCounter", "Combo, best combo, hits and misses", Category.COMBAT, 2, 68);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		CombatTracker t = CombatTracker.get();
		List<String> lines = new ArrayList<>();
		lines.add("Combo: §e" + t.getCombo() + " §7(best " + t.getBestCombo() + ")");
		if (showStats.get()) {
			lines.add("Hits: §a" + t.getHits() + " §fMisses: §c" + t.getMisses());
		}
		if (showAccuracy.get()) {
			lines.add(String.format("Accuracy: §b%.0f%%", t.getAccuracy()));
		}
		return panel(g, x, y, lines.toArray(String[]::new));
	}
}
