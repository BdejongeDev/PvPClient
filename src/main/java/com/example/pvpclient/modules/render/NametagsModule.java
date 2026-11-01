package com.example.pvpclient.modules.render;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.module.setting.NumberSetting;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Extra nametag above players showing health and distance. Only drawn for players you can
 * actually see (line of sight), so it never reveals players behind walls.
 */
public final class NametagsModule extends Module {
	private final NumberSetting range = setting(new NumberSetting("Range", 32, 4, 64, 1));
	private final BoolSetting showHealth = setting(new BoolSetting("Health", true));
	private final BoolSetting showDistance = setting(new BoolSetting("Distance", true));

	/** Recomputed every tick: line-of-sight raycasts are too expensive to do per frame. */
	private List<Player> visible = new ArrayList<>();

	public NametagsModule() {
		super("Nametags", "Health and distance above visible players", Category.RENDER);
		on(Events.Tick.class, e -> {
			List<Player> list = new ArrayList<>();
			if (inGame()) {
				for (Player p : mc.level.players()) {
					if (p != mc.player && p.isAlive() && p.distanceTo(mc.player) <= range.get() && RenderUtil.canSee(p)) {
						list.add(p);
					}
				}
			}
			visible = list;
		});
		on(Events.WorldOverlay.class, this::draw);
	}

	private void draw(Events.WorldOverlay e) {
		GuiGraphicsExtractor g = e.graphics();
		for (Player p : visible) {
			double[] pos = RenderUtil.worldToScreen(g, RenderUtil.aboveHead(p, e.partialTick(), 0.75));
			if (pos == null) {
				continue;
			}
			StringBuilder text = new StringBuilder(p.getName().getString());
			if (showHealth.get()) {
				float hp = p.getHealth() + p.getAbsorptionAmount();
				text.append(String.format(" §c%.1f❤", hp));
			}
			if (showDistance.get()) {
				text.append(String.format(" §7%.0fm", p.distanceTo(mc.player)));
			}
			String s = text.toString();
			int w = mc.font.width(s);
			int x = (int) pos[0] - w / 2, y = (int) pos[1] - mc.font.lineHeight;
			g.fill(x - 2, y - 1, x + w + 2, y + mc.font.lineHeight, 0x80000000);
			g.text(mc.font, s, x, y, 0xFFFFFFFF, true);
		}
	}

	@Override
	protected void onDisable() {
		visible = new ArrayList<>();
	}
}
