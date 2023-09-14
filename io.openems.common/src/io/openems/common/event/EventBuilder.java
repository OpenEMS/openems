package io.openems.common.event;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public final class EventBuilder {
	private final String eventTopic;
	private final Map<String, Object> eventArgs;
	private final EventAdmin eventAdmin;

	private EventBuilder(EventAdmin eventAdmin, String eventTopic) {
		this.eventAdmin = eventAdmin;
		this.eventTopic = eventTopic;
		this.eventArgs = new HashMap<>();
	}

	/**
	 * Create new EventBuilder.
	 *
	 * @param eventAdmin used eventAdmin
	 * @param eventTopic topic of event
	 * @return new EventBuilder
	 */
	public static EventBuilder from(EventAdmin eventAdmin, String eventTopic) {
		return new EventBuilder(eventAdmin, eventTopic);
	}

	/**
	 * Build event without arguments and post it via given EventAdmin.
	 *
	 * @param eventAdmin used eventAdmin
	 * @param eventTopic topic of event
	 * @throws SecurityException If the caller does not have
	 *                           TopicPermission[topic,PUBLISH] for the topic
	 *                           specified in the event.
	 * @see org.osgi.service.event.EventAdmin#postEvent(Event)
	 */
	public static void post(EventAdmin eventAdmin, String eventTopic) {
		eventAdmin.postEvent(new Event(eventTopic, Map.of()));
	}

	/**
	 * Build event and post it via given EventAdmin.
	 *
	 * @throws SecurityException If the caller does not have
	 *                           TopicPermission[topic,PUBLISH] for the topic
	 *                           specified in the event.
	 * @see org.osgi.service.event.EventAdmin#postEvent(Event)
	 */
	public void post() throws SecurityException {
		this.eventAdmin.postEvent(this.build());
	}

	/**
	 * Build event without arguments and send it via given EventAdmin.
	 *
	 * @param eventAdmin used eventAdmin
	 * @param eventTopic topic of event
	 * @throws SecurityException If the caller does not have
	 *                           TopicPermission[topic,PUBLISH] for the topic
	 *                           specified in the event.
	 * @see org.osgi.service.event.EventAdmin#sendEvent(Event)
	 */
	public static void send(EventAdmin eventAdmin, String eventTopic) {
		eventAdmin.sendEvent(new Event(eventTopic, Map.of()));
	}

	/**
	 * Build event and send it via given EventAdmin.
	 *
	 * @throws SecurityException If the caller does not have
	 *                           TopicPermission[topic,PUBLISH] for the topic
	 *                           specified in the event.
	 * @see org.osgi.service.event.EventAdmin#sendEvent(Event)
	 */
	public void send() throws SecurityException {
		this.eventAdmin.sendEvent(this.build());
	}
	
	/**
	 * Build event and return it.
	 * 
	 * @return built {@link Event}
	 */
	public Event build() {
		return new Event(this.eventTopic, this.eventArgs);
	}

	/**
	 * Add an argument to EventBuilder.
	 *
	 * @param <T>        type of constant
	 * @param identifier the identifier
	 * @param arg        the actual argument
	 * @return same instance
	 */
	public <T> EventBuilder addArg(String identifier, T arg) {
		this.eventArgs.put(identifier, arg);
		return this;
	}

}
