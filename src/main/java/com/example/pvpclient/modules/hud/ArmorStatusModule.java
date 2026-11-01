package com.example.pvpclient.modules.hud;

import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Worn armor (and held item) with remaining durability. */
public final class ArmorStatusModule extends HudModule {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

	private static final int CELL = 28;

	private final BoolSetting showHeld = setting(new BoolSetting("Held Item", true));
	private final BoolSetting percent = setting(new BoolSetting("Percentage", false));

	public ArmorStatusModule() {
		super("ArmorStatus", "Armor durability", Category.HUD, RIGHT, BOTTOM);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		if (mc.player == null) {
			return new int[]{0, 0};
		}
		int col = 0;
		for (EquipmentSlot slot : SLOTS) {
			if (slot == EquipmentSlot.MAINHAND && !showHeld.get()) {
				continue;
			}
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			int cx = x + col * CELL;
			g.item(stack, cx + 2, y);
			if (stack.isDamageableItem()) {
				int left = stack.getMaxDamage() - stack.getDamageValue();
				float frac = left / (float) stack.getMaxDamage();
				String text = percent.get() ? Math.round(frac * 100) + "%" : String.valueOf(left);
				// Durability centred under the item.
				g.text(mc.font, text, cx + CELL / 2 - mc.font.width(text) / 2, y + 17, RenderUtil.healthColor(frac), true);
			} else if (stack.getCount() > 1) {
				g.itemDecorations(mc.font, stack, cx + 2, y);
			}
			col++;
		}
		if (col == 0 && preview) {
			return panel(g, x, y, "Armor");
		}
		return col == 0 ? new int[]{0, 0} : new int[]{col * CELL, 26};
	}
}
