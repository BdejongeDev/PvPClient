package com.example.pvpclient.modules.hud;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Active potion effects with level and remaining time. */
public final class PotionEffectsModule extends HudModule {
	private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

	public PotionEffectsModule() {
		super("PotionEffects", "Active effects and durations", Category.HUD, 2, 105);
	}

	@Override
	protected int[] draw(GuiGraphics g, int x, int y, boolean preview) {
		List<String> lines = new ArrayList<>();
		if (mc.player != null) {
			for (MobEffectInstance effect : mc.player.getActiveEffects()) {
				int amp = effect.getAmplifier();
				String level = amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1);
				String color = effect.getEffect().value().isBeneficial() ? "§a" : "§c";
				lines.add(color + effect.getEffect().value().getDisplayName().getString() + level + " §f" + duration(effect));
			}
		}
		if (lines.isEmpty()) {
			return preview ? panel(g, x, y, "§aSpeed II §f1:30") : new int[]{0, 0};
		}
		return panel(g, x, y, lines.toArray(String[]::new));
	}

	private static String duration(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return "∞";
		}
		int seconds = effect.getDuration() / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
