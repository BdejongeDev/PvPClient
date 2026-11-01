package com.example.pvpclient.modules.combat;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import com.example.pvpclient.module.setting.BoolSetting;
import com.example.pvpclient.module.setting.ModeSetting;
import com.example.pvpclient.module.setting.NumberSetting;
import com.example.pvpclient.rotation.Rotation;
import com.example.pvpclient.rotation.RotationController;
import com.example.pvpclient.targeting.TargetSelector;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Smoothly pulls the camera towards the best target. Targets are chosen once per tick;
 * the camera is turned every frame so the motion is smooth at any frame rate.
 */
public final class AimAssistModule extends Module {
	private final NumberSetting smoothness = setting(new NumberSetting("Smoothness", 30, 1, 100, 1));
	private final NumberSetting fov = setting(new NumberSetting("FOV", 60, 10, 180, 1));
	private final NumberSetting distance = setting(new NumberSetting("Max Distance", 4.0, 1.0, 6.0, 0.1));
	private final NumberSetting maxTurnSpeed = setting(new NumberSetting("Max Turn Speed", 8.0, 1.0, 30.0, 0.5));
	private final ModeSetting targets = setting(new ModeSetting("Targets", "Players", "Players", "Hostiles", "All"));
	private final BoolSetting visibilityCheck = setting(new BoolSetting("Visibility Check", true));
	private final BoolSetting requireAttack = setting(new BoolSetting("Require Attack", false));

	private final TargetSelector selector = new TargetSelector();
	private final RotationController rotation = new RotationController();
	@Nullable
	private LivingEntity target;

	public AimAssistModule() {
		super("BestOptimized Screen", "Smoothly aims towards nearby targets", Category.COMBAT);
		on(Events.Tick.class, e -> onTick());
		on(Events.Frame.class, this::onFrame);
	}

	private TargetSelector.Options options() {
		TargetSelector.Filter filter = switch (targets.get()) {
			case "Hostiles" -> TargetSelector.Filter.HOSTILES;
			case "All" -> TargetSelector.Filter.ALL;
			default -> TargetSelector.Filter.PLAYERS;
		};
		return new TargetSelector.Options(distance.get(), fov.get(), filter, visibilityCheck.get());
	}

	private boolean active() {
		if (!inGame() || mc.screen != null || mc.isPaused()) {
			return false;
		}
		return !requireAttack.get() || mc.options.keyAttack.isDown();
	}

	private void onTick() {
		target = active() ? selector.select(mc.player, options(), target) : null;
	}

	private void onFrame(Events.Frame e) {
		if (target == null || !active()) {
			return;
		}
		LocalPlayer player = mc.player;
		if (!selector.isStillValid(player, target, options(), e.partialTick())) {
			target = null;
			return;
		}
		Rotation current = new Rotation(player.getYRot(), player.getXRot());
		Rotation desired = Rotation.between(player.getEyePosition(e.partialTick()),
				selector.aimPoint(player, target, e.partialTick()));
		// Clamp to one tick so a lag spike can never cause a large jump.
		Rotation next = rotation.step(current, desired, smoothness.intValue(), maxTurnSpeed.get(),
				Math.min(e.frameTicks(), 1.0F));

		float dYaw = next.yaw() - current.yaw();
		float dPitch = next.pitch() - current.pitch();
		if (dYaw != 0 || dPitch != 0) {
			// Shift the previous-tick rotation too (like Entity#turn) so interpolation does not snap.
			player.setYRot(next.yaw());
			player.setXRot(next.pitch());
			player.yRotO += dYaw;
			player.xRotO += dPitch;
		}
	}

	@Override
	protected void onDisable() {
		target = null;
	}
}
