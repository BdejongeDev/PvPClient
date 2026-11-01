package com.example.pvpclient.modules.render;

import com.example.pvpclient.event.Events;
import com.example.pvpclient.module.Category;
import com.example.pvpclient.module.Module;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Shows entity hitboxes using the vanilla debug renderer (same as F3+B), so no custom
 * rendering or mixins are needed. The previous state is restored when disabled.
 */
public final class HitboxViewerModule extends Module {
	@Nullable
	private DebugScreenEntryStatus previous;

	public HitboxViewerModule() {
		super("HitboxViewer", "Show entity hitboxes (like F3+B)", Category.RENDER);
		// Applied from tick instead of onEnable: the debug entry list does not exist yet
		// while the config is being loaded at startup.
		on(Events.Tick.class, e -> {
			var entries = mc.debugEntries;
			if (entries.getStatus(DebugScreenEntries.ENTITY_HITBOXES) != DebugScreenEntryStatus.ALWAYS_ON) {
				if (previous == null) {
					previous = entries.getStatus(DebugScreenEntries.ENTITY_HITBOXES);
				}
				entries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
			}
		});
	}

	@Override
	protected void onDisable() {
		if (mc.debugEntries != null) {
			mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES,
					previous != null ? previous : DebugScreenEntryStatus.NEVER);
		}
		previous = null;
	}
}
