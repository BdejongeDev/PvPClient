package com.example.pvpclient.util;

import com.example.pvpclient.event.EventBus;
import com.example.pvpclient.event.Events;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts attack-key clicks in a sliding one-second window. */
public final class CpsTracker {
	private static CpsTracker instance;
	private final Deque<Long> clicks = new ArrayDeque<>();

	public CpsTracker(EventBus bus) {
		instance = this;
		bus.subscribe(Events.AttackClick.class, e -> {
			long now = System.currentTimeMillis();
			for (int i = 0; i < e.clicks(); i++) {
				clicks.addLast(now);
			}
		});
	}

	public static CpsTracker get() {
		return instance;
	}

	public int getCps() {
		long cutoff = System.currentTimeMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
