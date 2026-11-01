package com.example.pvpclient.module;

public enum Category {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	RENDER("Render"),
	PLAYER("Player"),
	HUD("HUD"),
	MISC("Misc");

	public final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}
}
