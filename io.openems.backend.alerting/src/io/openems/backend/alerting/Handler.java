package io.openems.backend.alerting;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

import io.openems.common.event.EventReader;

public interface Handler<T extends Message> {

	/**
	 * Stop the Handler.
	 */
	public void stop();

	/**
	 * Send the messages.
	 *
	 * @param sentAt   TimeStamp at with sending was initiated
	 * @param messages which to send
	 */
	public void send(ZonedDateTime sentAt, List<T> messages);

	/**
	 * Return generic type of handler as Class object.
	 *
	 * @return GenericType of handler
	 */
	public Class<T> getGeneric();

	/**
	 * Handle given event.
	 *
	 * @param eventTopic to handle
	 * @return {@link Consumer} to be scheduled in executor
	 */
	public Consumer<EventReader> getEventHandler(String eventTopic);
}
