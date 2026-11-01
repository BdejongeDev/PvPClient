package com.example.pvpclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

/** All events posted by the client. */
public final class Events {
	private Events() {}

	/** End of every client tick (20/s), only while in a world. */
	public record Tick(Minecraft mc) {}

	/** Once per rendered frame, before the level is drawn. */
	public record Frame(Minecraft mc, float frameTicks, float partialTick) {}

	/** Screen-space overlay drawn below the HUD (nametags, indicators). */
	public record WorldOverlay(Minecraft mc, GuiGraphics graphics, float partialTick) {}

	/** The local player attacked an entity. */
	public record AttackEntity(Minecraft mc, Entity target) {}

	/** The attack key was clicked {@code clicks} times this tick; {@code missed} if nothing was hit. */
	public record AttackClick(Minecraft mc, int clicks, boolean missed) {}
}
