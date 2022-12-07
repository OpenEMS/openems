package io.openems.backend.alerting;

import java.time.ZonedDateTime;
import java.util.List;

import org.osgi.service.event.EventHandler;

public interface Handler<T extends Message> extends EventHandler {

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
}
