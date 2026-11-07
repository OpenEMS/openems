package io.openems.backend.common.edge.subscription;

/**
 * The handler interface for subscription management. It can be attached to
 * specific subscription keys and is created via
 * {@link SubscriptionHandlerFactory}.
 *
 * <p>
 * The instance is in case of a global handler shared between all clients, in
 * case of a client specific handler every client has it's own instance.
 */
public interface SubscriptionHandler {
	/**
	 * In case of a client specific handler, this method is called when the client
	 * subscribes. In case of global handler, this method is called the very first
	 * time some client subscribes.
	 */
	void subscribe();

	/**
	 * The same as subscibe(), but called when the subscription is unsubscribed or
	 * the user disconnects. In case of global handler, it is called when the last
	 * client unsubscribes.
	 */
	void unsubscribe();
}
