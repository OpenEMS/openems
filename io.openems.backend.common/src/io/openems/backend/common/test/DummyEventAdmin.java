package io.openems.backend.common.test;

import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class DummyEventAdmin implements EventAdmin {

	private final Function<Event, Boolean> eventFilter;
	private final Consumer<Event> onEvent;

	public DummyEventAdmin(Consumer<Event> onEvent) {
		this.eventFilter = (e) -> true;
		this.onEvent = onEvent;
	}

	public DummyEventAdmin(Function<Event, Boolean> eventFilter, Consumer<Event> onEvent) {
		this.eventFilter = eventFilter;
		this.onEvent = onEvent;
	}

	@Override
	public void postEvent(Event event) {
		this.handleEvent(event);
	}

	@Override
	public void sendEvent(Event event) {
		this.handleEvent(event);
	}

	private void handleEvent(Event event) {
		if (this.eventFilter.apply(event)) {
			this.onEvent.accept(event);
		}
	}
}
