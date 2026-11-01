package com.example.pvpclient.modules.render;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.module.setting.NumberSetting;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Floating damage (and optionally heal) numbers. Health is sampled every tick; a drop spawns
 * a number at the entity that rises and fades. Only for entities you can see.
 */
public final class DamageIndicatorModule extends Module {
	private static final long LIFETIME_MS = 1200;

	private final NumberSetting range = setting(new NumberSetting("Range", 16, 4, 32, 1));
	private final BoolSetting showHeals = setting(new BoolSetting("Show Heals", false));

	private final Map<Integer, Float> lastHealth = new HashMap<>();
	private final List<Popup> popups = new ArrayList<>();

	private record Popup(Vec3 pos, float amount, long start) {}

	public DamageIndicatorModule() {
		super("DamageIndicator", "Floating damage numbers", Category.RENDER);
		on(Events.Tick.class, e -> onTick());
		on(Events.WorldOverlay.class, this::draw);
	}

	private void onTick() {
		if (!inGame()) {
			lastHealth.clear();
			return;
		}
		Map<Integer, Float> seen = new HashMap<>();
		AABB area = mc.player.getBoundingBox().inflate(range.get());
		for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, area, en -> !(en instanceof ArmorStand))) {
			float hp = entity.getHealth() + entity.getAbsorptionAmount();
			Float prev = lastHealth.get(entity.getId());
			if (prev != null) {
				float diff = hp - prev;
				boolean show = diff <= -0.5F || (showHeals.get() && diff >= 0.5F);
				if (show && entity != mc.player && RenderUtil.canSee(entity)) {
					// Small random horizontal offset so rapid hits do not overlap.
					double ox = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
					popups.add(new Popup(RenderUtil.aboveHead(entity, 1.0F, 0.2).add(ox, 0, oz), diff, System.currentTimeMillis()));
				}
			}
			seen.put(entity.getId(), hp);
		}
		lastHealth.clear();
		lastHealth.putAll(seen);
	}

	private void draw(Events.WorldOverlay e) {
		long now = System.currentTimeMillis();
		Iterator<Popup> it = popups.iterator();
		while (it.hasNext()) {
			Popup p = it.next();
			float age = (now - p.start()) / (float) LIFETIME_MS;
			if (age >= 1) {
				it.remove();
				continue;
			}
			double[] pos = RenderUtil.worldToScreen(e.graphics(), p.pos().add(0, age * 0.8, 0));
			if (pos == null) {
				continue;
			}
			int alpha = (int) (255 * (1 - age * age));
			int rgb = p.amount() < 0 ? 0xFF5555 : 0x55FF55;
			String text = String.format("%s%.1f", p.amount() < 0 ? "-" : "+", Math.abs(p.amount()));
			int w = mc.font.width(text);
			e.graphics().text(mc.font, text, (int) pos[0] - w / 2, (int) pos[1], (Math.max(alpha, 8) << 24) | rgb, true);
		}
	}

	@Override
	protected void onDisable() {
		popups.clear();
		lastHealth.clear();
	}
}
