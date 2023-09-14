package io.openems.backend.alerting.scheduler;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;

/**
 * Specifies classes able to generate a fitting {@link MessageScheduler} to a
 * {@link Handler} with the same generic {@link T}.
 * 
 * @author kai.jeschek
 *
 */
public interface MessageSchedulerService {
	/**
	 * Register handler for message scheduling and return MessageScheduler, to do
	 * so.
	 *
	 * @param <T>     type of message
	 * @param handler to register
	 * @return new MessageScheduler for handler to schedule messages with
	 */
	public <T extends Message> MessageScheduler<T> register(Handler<T> handler);

	/**
	 * Unregister handler for message scheduling and remove MessageScheduler.
	 *
	 * @param <T>     type of message
	 * @param handler to unregister
	 */
	public <T extends Message> void unregister(Handler<T> handler);
	
	
}
