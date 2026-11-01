package com.example.pvpclient;

import com.example.pvpclient.config.ConfigManager;
import com.example.pvpclient.event.EventBus;
import com.example.pvpclient.event.Events;
import com.example.pvpclient.gui.HudEditorScreen;
import com.example.pvpclient.input.KeybindManager;
import com.example.pvpclient.module.HudModule;
import com.example.pvpclient.module.ModuleManager;
import com.example.pvpclient.modules.combat.ComboCounterModule;
import com.example.pvpclient.modules.combat.CpsModule;
import com.example.pvpclient.modules.combat.HitTimingModule;
import com.example.pvpclient.modules.combat.TargetHudModule;
import com.example.pvpclient.modules.hud.ArmorStatusModule;
import com.example.pvpclient.modules.hud.CoordinatesModule;
import com.example.pvpclient.modules.hud.FpsModule;
import com.example.pvpclient.modules.hud.PingModule;
import com.example.pvpclient.modules.hud.PotionEffectsModule;
import com.example.pvpclient.modules.misc.ClickGuiModule;
import com.example.pvpclient.modules.movement.AutoSprintModule;
import com.example.pvpclient.modules.movement.MovementHudModule;
import com.example.pvpclient.modules.movement.StrafeTrainerModule;
import com.example.pvpclient.modules.movement.ToggleSprintModule;
import com.example.pvpclient.modules.player.AutoRespawnModule;
import com.example.pvpclient.modules.render.CrosshairModule;
import com.example.pvpclient.modules.render.DamageIndicatorModule;
import com.example.pvpclient.modules.render.HitboxViewerModule;
import com.example.pvpclient.modules.render.NametagsModule;
import com.example.pvpclient.modules.render.TargetIndicatorModule;
import com.example.pvpclient.util.CombatTracker;
import com.example.pvpclient.util.CpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client entrypoint: builds the core services, registers all modules and bridges Fabric
 * events onto the internal {@link EventBus}. No mixins are used.
 */
public final class PvPClient implements ClientModInitializer {
	public static final String MOD_ID = "pvpclient";
	public static final String DISCORD_URL = "https://discord.gg/EwdtxCMZSh";

	private static ConfigManager config;

	private final EventBus bus = new EventBus();
	private final ModuleManager modules = new ModuleManager(bus);
	private final KeybindManager keybinds = new KeybindManager(modules);

	public static ConfigManager config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		// Shared trackers first so they receive events before modules read them.
		new CombatTracker(bus);
		new CpsTracker(bus);

		// Combat
		registerOptional("com.example.pvpclient.modules.combat.AimAssistModule");
		modules.register(new ComboCounterModule());
		modules.register(new CpsModule());
		modules.register(new TargetHudModule());
		modules.register(new HitTimingModule());
		// Movement
		modules.register(new AutoSprintModule());
		modules.register(new ToggleSprintModule());
		modules.register(new StrafeTrainerModule());
		modules.register(new MovementHudModule());
		// Render
		modules.register(new CrosshairModule());
		modules.register(new HitboxViewerModule());
		modules.register(new NametagsModule());
		modules.register(new TargetIndicatorModule());
		modules.register(new DamageIndicatorModule());
		// Player
		modules.register(new AutoRespawnModule());
		// HUD
		modules.register(new FpsModule());
		modules.register(new PingModule());
		modules.register(new CoordinatesModule());
		modules.register(new ArmorStatusModule());
		modules.register(new PotionEffectsModule());
		// Misc
		modules.register(new ClickGuiModule());

		config = new ConfigManager(modules);
		config.load();

		registerEvents();
		registerHud();
	}

	/**
	 * Registers a module that may be left out of some builds. Loaded by name so this class
	 * compiles either way.
	 */
	private void registerOptional(String className) {
		try {
			modules.register((com.example.pvpclient.module.Module) Class.forName(className).getDeclaredConstructor().newInstance());
		} catch (ClassNotFoundException e) {
			// Not part of this build.
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not create " + className, e);
		}
	}

	private void registerEvents() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level != null) {
				bus.post(new Events.Tick(mc));
			}
			keybinds.onTick(mc);
			config.saveIfDirty();
		});

		// Once per rendered frame, at the start of level rendering.
		WorldRenderEvents.START_MAIN.register(context -> {
			Minecraft mc = Minecraft.getInstance();
			bus.post(new Events.Frame(mc, mc.getDeltaTracker().getRealtimeDeltaTicks(),
					mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
		});

		// Fires every tick while the attack key is held; clickCount > 0 only on actual clicks.
		ClientPreAttackCallback.EVENT.register((mc, player, clickCount) -> {
			if (clickCount > 0) {
				boolean missed = !(mc.hitResult instanceof EntityHitResult);
				bus.post(new Events.AttackClick(mc, clickCount, missed));
			}
			return false; // never cancel vanilla handling
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			Minecraft mc = Minecraft.getInstance();
			if (level.isClientSide() && player == mc.player) {
				bus.post(new Events.AttackEntity(mc, entity));
			}
			return InteractionResult.PASS;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> config.save());
	}

	private void registerHud() {
		// Custom crosshair: wrap the vanilla element and only fall back to it when the module is off.
		CrosshairModule crosshair = modules.get(CrosshairModule.class);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, delta) -> {
			if (!crosshair.isEnabled()) {
				vanilla.render(g, delta);
			} else if (crosshair.shouldDraw()) {
				modules.renderSafely(crosshair, () -> crosshair.draw(g));
			}
		});

		// World-anchored overlays (nametags, indicators) go below everything else.
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(MOD_ID, "world_overlay"), (g, delta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				bus.post(new Events.WorldOverlay(mc, g, delta.getGameTimeDeltaPartialTick(false)));
			}
		});

		// Movable HUD panels on top.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (g, delta) -> {
			Minecraft mc = Minecraft.getInstance();
			boolean preview = mc.screen instanceof HudEditorScreen;
			for (HudModule hud : modules.getHudModules()) {
				if (hud.isEnabled()) {
					modules.renderSafely(hud, () -> hud.render(g, preview));
				}
			}
		});
	}
}
