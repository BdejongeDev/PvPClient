package com.example.pvpclient.modules.movement;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

/** Horizontal speed, sprint/sneak state and ground state. */
public final class MovementHudModule extends HudModule {
	private double speed;
	private Vec3 lastPos;

	public MovementHudModule() {
		super("MovementHUD", "Speed and movement state", Category.MOVEMENT, RIGHT, 52);
		on(Events.Tick.class, e -> {
			if (!inGame()) {
				return;
			}
			Vec3 pos = mc.player.position();
			if (lastPos != null) {
				double dx = pos.x - lastPos.x, dz = pos.z - lastPos.z;
				speed = Math.sqrt(dx * dx + dz * dz) * 20.0; // blocks per tick -> per second
			}
			lastPos = pos;
		});
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
		if (!inGame()) {
			return panel(g, x, y, "Speed: 0.00 b/s");
		}
		boolean toggled = ModuleManager.get().get(ToggleSprintModule.class).isSprintToggled();
		String state = mc.player.isShiftKeyDown() ? "§eSneaking"
				: mc.player.isSprinting() ? "§aSprinting" + (toggled ? " §7(toggled)" : "")
				: "§7Walking";
		return panel(g, x, y,
				String.format("Speed: §b%.2f §fb/s", speed),
				state,
				mc.player.onGround() ? "§fOn ground" : "§9Airborne");
	}
}
