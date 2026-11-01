package com.example.pvpclient.modules.combat;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.util.CombatTracker;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Name, health bar, armor and distance of the current target. */
public final class TargetHudModule extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int WIDTH = 130;

	private final BoolSetting showArmorItems = setting(new BoolSetting("Armor Items", true));

	public TargetHudModule() {
		super("TargetHUD", "Info about the entity you are fighting", Category.COMBAT, RIGHT, 128);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		LivingEntity target = CombatTracker.get().getTarget();
		if (target == null && !preview) {
			return new int[]{0, 0};
		}
		String name = target != null ? target.getDisplayName().getString() : "Target";
		float health = target != null ? target.getHealth() : 20;
		float max = target != null ? target.getMaxHealth() : 20;
		float absorption = target != null ? target.getAbsorptionAmount() : 0;
		int armor = target != null ? target.getArmorValue() : 0;
		double dist = target != null && mc.player != null ? target.distanceTo(mc.player) : 0;

		int h = showArmorItems.get() ? 58 : 40;
		g.fill(x, y, x + WIDTH, y + h, BACKGROUND);
		g.text(mc.font, name, x + PADDING, y + PADDING, TEXT, true);

		// Health bar with absorption drawn in gold on top.
		int barY = y + 14, barW = WIDTH - PADDING * 2;
		float frac = max > 0 ? health / max : 0;
		g.fill(x + PADDING, barY, x + PADDING + barW, barY + 5, 0xFF303030);
		g.fill(x + PADDING, barY, x + PADDING + (int) (barW * Math.min(1, frac)), barY + 5, RenderUtil.healthColor(frac));
		if (absorption > 0) {
			int absW = (int) (barW * Math.min(1, absorption / max));
			g.fill(x + PADDING, barY + 3, x + PADDING + absW, barY + 5, 0xFFFFC400);
		}
		String hp = String.format("%.1f§7/%.0f", health, max) + (absorption > 0 ? String.format(" §6+%.0f", absorption) : "");
		g.text(mc.font, "§c❤ §f" + hp, x + PADDING, y + 22, TEXT, true);
		g.text(mc.font, String.format("Armor: §b%d  §fDist: §e%.1f", armor, dist), x + PADDING, y + 31, TEXT, true);

		if (showArmorItems.get() && target != null) {
			int ix = x + PADDING;
			for (EquipmentSlot slot : ARMOR) {
				ItemStack stack = target.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					g.item(stack, ix, y + 40);
					g.itemDecorations(mc.font, stack, ix, y + 40);
				}
				ix += 18;
			}
			ItemStack hand = target.getItemBySlot(EquipmentSlot.MAINHAND);
			if (!hand.isEmpty()) {
				g.item(hand, ix + 4, y + 40);
			}
		}
		return new int[]{WIDTH, h};
	}
}
