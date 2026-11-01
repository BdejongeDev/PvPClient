package com.example.pvpclient.gametest;

import com.example.pvpclient.gui.ClickGuiScreen;
import com.example.pvpclient.gui.HudEditorScreen;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.modules.misc.ClickGuiModule;
import com.example.pvpclient.util.CombatTracker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * In-game smoke test: creates a world, enables every module, fights a zombie, toggles all
 * modules repeatedly and opens both screens. Fails if any module disabled itself because of
 * an exception. Screenshots end up in the test run directory.
 */
public final class ClientSmokeTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("time set day");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");
			world.getServer().runCommand("item replace entity @a armor.chest with minecraft:iron_chestplate");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
			world.getServer().runCommand("effect give @a minecraft:speed 120 1");
			world.getServer().runCommand("effect give @a minecraft:strength 120 0");
			context.waitTicks(20);

			List<Module> testable = context.computeOnClient(mc -> ModuleManager.get().getModules().stream()
					.filter(m -> !(m instanceof ClickGuiModule)).toList());

			context.runOnClient(mc -> testable.forEach(m -> m.setEnabled(true)));

			// Zombie right in front of the player that cannot move or attack back.
			BlockPos pos = context.computeOnClient(mc -> mc.player.blockPosition());
			BlockPos zombie = pos.south(2);
			world.getServer().runCommand(String.format("summon minecraft:zombie %d %d %d {NoAI:1b,active_effects:[{id:\"minecraft:fire_resistance\",duration:-1}]}",
					zombie.getX(), zombie.getY(), zombie.getZ()));
			context.waitTicks(10);
			context.getInput().lookAt(zombie.above());
			context.waitTicks(5);

			// Hit it a few times (with attack cooldown in between) and miss once.
			for (int i = 0; i < 3; i++) {
				context.getInput().pressKey(options -> options.keyAttack);
				context.waitTicks(15);
			}
			context.waitTicks(5);
			context.takeScreenshot("pvpclient-hud-in-combat");

			int hits = context.computeOnClient(mc -> CombatTracker.get().getHits());
			if (hits < 1) {
				int misses = context.computeOnClient(mc -> CombatTracker.get().getMisses());
				throw new AssertionError("Expected CombatTracker to register hits, got " + hits + " (misses " + misses + ")");
			}

			// Toggle everything rapidly: must never crash or auto-disable a module.
			for (int round = 0; round < 3; round++) {
				context.runOnClient(mc -> testable.forEach(Module::toggle));
				context.waitTicks(2);
				context.runOnClient(mc -> testable.forEach(Module::toggle));
				context.waitTicks(2);
			}
			context.waitTicks(10);
			List<String> broken = context.computeOnClient(mc -> testable.stream()
					.filter(m -> !m.isEnabled()).map(Module::getName).toList());
			if (!broken.isEmpty()) {
				throw new AssertionError("Modules disabled themselves after an error: " + broken);
			}

			context.setScreen(() -> new ClickGuiScreen(null));
			context.waitTicks(5);
			checkClickGuiMouse(context);
			context.takeScreenshot("pvpclient-clickgui");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("pvpclient-hud-editor");
			context.setScreen(() -> null);

			// Leave the modules off so the dev config is clean.
			context.runOnClient(mc -> testable.forEach(m -> m.setEnabled(false)));
		}
	}

	/**
	 * Left click on a module row must toggle it, right click must only expand it. Clicks the
	 * first row of the Render panel (Crosshair) at its default position.
	 */
	private static void checkClickGuiMouse(ClientGameTestContext context) {
		int scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		Module crosshair = context.computeOnClient(mc -> ModuleManager.get().byName("Crosshair").orElseThrow());
		boolean before = crosshair.isEnabled();
		context.getInput().setCursorPos((10 + 2 * 128 + 60) * scale, 33 * scale);
		context.getInput().pressMouse(1); // right: expand only
		if (crosshair.isEnabled() != before) {
			throw new AssertionError("Right click toggled the module instead of opening its settings");
		}
		context.getInput().pressMouse(0); // left: toggle
		if (crosshair.isEnabled() == before) {
			throw new AssertionError("Left click did not toggle the module");
		}
		context.getInput().pressMouse(0); // toggle back
		context.getInput().pressMouse(1); // collapse again
	}
}
