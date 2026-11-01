package com.example.pvpclient.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Minimal type-keyed event bus. Listeners are called in registration order.
 * Exception isolation is handled by the subscriber (see {@code ModuleManager}).
 */
public final class EventBus {
	private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <E> void subscribe(Class<E> type, Consumer<? super E> listener) {
		listeners.computeIfAbsent(type, k -> new ArrayList<>()).add((Consumer<Object>) listener);
	}

	public <E> E post(E event) {
		List<Consumer<Object>> list = listeners.get(event.getClass());
		if (list != null) {
			for (Consumer<Object> listener : list) {
				listener.accept(event);
			}
		}
		return event;
	}
}
