package com.example.pvpclient.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

public final class RenderUtil {
	private RenderUtil() {}

	/**
	 * Projects a world position to GUI coordinates, or null when it is behind the camera.
	 * Uses the camera's current view-projection matrix, so call it while drawing the HUD.
	 */
	@Nullable
	public static double[] worldToScreen(GuiGraphicsExtractor g, Vec3 pos) {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.mainCamera();
		if (!camera.isInitialized()) {
			return null;
		}
		Vec3 rel = pos.subtract(camera.position());
		Vector3fc forward = camera.forwardVector();
		if (rel.x * forward.x() + rel.y * forward.y() + rel.z * forward.z() <= 0.05) {
			return null;
		}
		// Normalized device coordinates: -1..1 with y pointing up.
		Vec3 ndc = mc.gameRenderer.projectPointToScreen(pos);
		double x = (ndc.x + 1.0) / 2.0 * g.guiWidth();
		double y = (1.0 - ndc.y) / 2.0 * g.guiHeight();
		return new double[]{x, y};
	}

	/** Interpolated position of the top of an entity plus a vertical offset. */
	public static Vec3 aboveHead(Entity entity, float partialTick, double offset) {
		return entity.getPosition(partialTick).add(0, entity.getBbHeight() + offset, 0);
	}

	/** Visible to the local player: in line of sight and not invisible (no seeing through walls). */
	public static boolean canSee(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !entity.isInvisible() && mc.player.hasLineOfSight(entity);
	}

	/** Red to yellow to green depending on fraction (0..1). */
	public static int healthColor(float fraction) {
		fraction = Math.max(0, Math.min(1, fraction));
		int r = (int) (255 * Math.min(1, 2 * (1 - fraction)));
		int g = (int) (255 * Math.min(1, 2 * fraction));
		return 0xFF000000 | (r << 16) | (g << 8) | 0x30;
	}

	public static void outline(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y1 + 1, color);
		g.fill(x1, y2 - 1, x2, y2, color);
		g.fill(x1, y1, x1 + 1, y2, color);
		g.fill(x2 - 1, y1, x2, y2, color);
	}
}
