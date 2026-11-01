package com.example.pvpclient.gametest;

import com.example.pvpclient.gui.ClickGuiScreen;
import com.example.pvpclient.gui.HudEditorScreen;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.module.setting.ModeSetting;
import com.example.pvpclient.module.setting.Setting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Takes 1920x1080 promotional screenshots in a normal world. Not a correctness test: it only
 * stages scenes. Output: build/run/clientGameTest/screenshots/promo-*.png
 */
public final class PromoScreenshots implements FabricClientGameTest {
	private static final List<String> SHOWCASE = List.of(
			"CPS", "FPS", "Ping", "Coordinates", "ComboCounter", "TargetHUD", "HitTiming", "PotionEffects",
			"ArmorStatus", "MovementHUD", "Crosshair", "DamageIndicator", "TargetIndicator");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(3);
			mc.resizeGui();
		});

		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			buildArena(server);
			for (String cmd : List.of(
					"item replace entity @a armor.head with minecraft:diamond_helmet",
					"item replace entity @a armor.chest with minecraft:diamond_chestplate",
					"item replace entity @a armor.legs with minecraft:diamond_leggings",
					"item replace entity @a armor.feet with minecraft:diamond_boots",
					"item replace entity @a weapon.mainhand with minecraft:netherite_sword",
					"item replace entity @a hotbar.1 with minecraft:golden_apple 16",
					"item replace entity @a hotbar.2 with minecraft:ender_pearl 16",
					"effect give @a minecraft:speed 600 1 true",
					"effect give @a minecraft:strength 600 0 true",
					"effect give @a minecraft:fire_resistance 600 0 true")) {
				server.runCommand(cmd);
			}

			ctx.runOnClient(mc -> {
				for (String name : SHOWCASE) {
					ModuleManager.get().byName(name).ifPresent(m -> m.setEnabled(true));
				}
				setMode("Crosshair", "Style", "Cross + Dot");
				setMode("Crosshair", "Color", "Aqua");
				setMode("ClickGUI", "Accent", "Orange");
			});

			// Armored opponent three blocks in front of the player, facing it, that stands still.
			server.runCommand("execute as @p at @p rotated ~ 0 run summon minecraft:zombie ^ ^ ^3 "
					+ "{NoAI:1b,IsBaby:0b,PersistenceRequired:1b,CustomName:\"Opponent\",CustomNameVisible:1b,"
					+ "Health:100f,attributes:[{id:\"minecraft:max_health\",base:100}],"
					+ "active_effects:[{id:\"minecraft:fire_resistance\",duration:-1,show_particles:0b}],"
					+ "equipment:{head:{id:\"minecraft:iron_helmet\",count:1},chest:{id:\"minecraft:iron_chestplate\",count:1},"
					+ "legs:{id:\"minecraft:iron_leggings\",count:1},feet:{id:\"minecraft:iron_boots\",count:1},"
					+ "mainhand:{id:\"minecraft:iron_sword\",count:1}}}");
			ctx.waitTicks(20);
			aimAtOpponent(ctx);

			// Let advancement toasts and chat messages fade out.
			ctx.waitTicks(320);
			aimAtOpponent(ctx);

			// 1. Combat: three fully charged hits, screenshot right after the last one so the
			//    damage number is floating and the charge bar is refilling.
			for (int i = 0; i < 3; i++) {
				ctx.getInput().pressKey(options -> options.keyAttack);
				ctx.waitTicks(i < 2 ? 14 : 9);
			}
			ctx.takeScreenshot("promo-1-combat");

			// 2. Crosshair styles, looking past the opponent at the landscape.
			ctx.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() + 60));
			ctx.waitTicks(10);
			String[] styles = {"Cross", "Cross + Dot", "Dot", "Circle", "T-Shape"};
			for (int i = 0; i < styles.length; i++) {
				String style = styles[i];
				ctx.runOnClient(mc -> setMode("Crosshair", "Style", style));
				ctx.waitTicks(2);
				ctx.takeScreenshot("promo-crosshair-" + i);
			}
			ctx.runOnClient(mc -> setMode("Crosshair", "Style", "Cross + Dot"));
			aimAtOpponent(ctx);
			ctx.waitTicks(40);

			// 3. ClickGUI with the Crosshair settings expanded (right click on its row).
			ctx.setScreen(() -> new ClickGuiScreen(null));
			ctx.waitTicks(5);
			int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
			// Render panel is the third column at the default layout: x = 10 + 2 * 128, first row y = 26..40.
			ctx.getInput().setCursorPos((10 + 2 * 128 + 60) * scale, 33 * scale);
			ctx.getInput().pressMouse(1);
			ctx.getInput().setCursorPos(1400, 900); // move the mouse away so no tooltip covers the panels
			ctx.waitTicks(5);
			ctx.takeScreenshot("promo-2-clickgui");

			// 4. HUD editor.
			ctx.setScreen(() -> new HudEditorScreen(null));
			ctx.getInput().setCursorPos(1400, 900);
			ctx.waitTicks(5);
			ctx.takeScreenshot("promo-3-hud-editor");
			ctx.setScreen(() -> null);

			ctx.runOnClient(mc -> ModuleManager.get().getModules().forEach(m -> m.setEnabled(false)));
		}
	}

	/** A small PvP arena around the player: quartz floor with an orange ring, low walls, lanterns. */
	private static void buildArena(TestServerContext server) {
		String at = "execute at @p run ";
		for (String cmd : List.of(
				"fill ~-14 ~-1 ~-14 ~14 ~-1 ~14 minecraft:smooth_quartz",
				"fill ~-9 ~-1 ~-9 ~9 ~-1 ~9 minecraft:orange_concrete",
				"fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 minecraft:smooth_quartz",
				"fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 minecraft:gold_block",
				"fill ~-14 ~ ~-14 ~14 ~1 ~-14 minecraft:quartz_bricks",
				"fill ~-14 ~ ~14 ~14 ~1 ~14 minecraft:quartz_bricks",
				"fill ~-14 ~ ~-14 ~-14 ~1 ~14 minecraft:quartz_bricks",
				"fill ~14 ~ ~-14 ~14 ~1 ~14 minecraft:quartz_bricks",
				"setblock ~-14 ~2 ~-14 minecraft:lantern", "setblock ~14 ~2 ~-14 minecraft:lantern",
				"setblock ~-14 ~2 ~14 minecraft:lantern", "setblock ~14 ~2 ~14 minecraft:lantern",
				"setblock ~ ~2 ~14 minecraft:lantern", "setblock ~ ~2 ~-14 minecraft:lantern",
				"setblock ~14 ~2 ~ minecraft:lantern", "setblock ~-14 ~2 ~ minecraft:lantern")) {
			server.runCommand(at + cmd);
		}
		server.runCommand("execute as @p at @p run tp @s ~ ~ ~ 0 0");
	}

	private static void aimAtOpponent(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> mc.level.getEntitiesOfClass(Zombie.class, mc.player.getBoundingBox().inflate(8)).stream()
				.findFirst()
				.ifPresent(z -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES,
						z.getEyePosition().subtract(0, 0.35, 0))));
	}

	private static void setMode(String module, String setting, String value) {
		Module m = ModuleManager.get().byName(module).orElseThrow();
		for (Setting<?> s : m.getSettings()) {
			if (s instanceof ModeSetting mode && mode.getName().equals(setting)) {
				mode.set(value);
			}
		}
	}
}
