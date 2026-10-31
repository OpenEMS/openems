package io.openems.backend.common.edge.subscription;

import io.openems.backend.common.edge.EdgeClient;

public interface SubscriptionHandlerFactory {
	/**
	 * Must return true if the factory is responsible for the given
	 * {@link SubscriptionKey}.
	 *
	 * <p>
	 * Warning! Make sure that only a single registered factory is responsible for
	 * specific SubscriptionKeys. If there are multiple factory that handle the same
	 * subscription keys, undefined behavior will occur.
	 *
	 * @param key key to check against
	 * @return true if the current factory is responsible for the given key
	 */
	boolean isHandling(SubscriptionKey key);

	/**
	 * Creates a new {@link SubscriptionHandler} instance for global handling. This
	 * is called only the first time a client subscribes. It is only called when
	 * isHandling() returns true.
	 * 
	 * @param key                 Key to create the global handler for.
	 * @param subscriptionManager Subscription manager instance. You can use it to
	 *                            get subscribed clients and more.
	 * @return Newly created handler instance. Can be null.
	 */
	SubscriptionHandler createGlobalHandler(SubscriptionKey key, SubscriptionManager subscriptionManager);

	/**
	 * Creates a new {@link ClientHandlerFactory} instance. You can use this
	 * instance to create client-specific handlers. If no client-specific handlers
	 * are needed, null can be returned.
	 *
	 * <p>
	 * This is called only the first time a client subscribes and isHandling() must
	 * be true.
	 *
	 * @param key                 Key to create the client handler factory for.
	 * @param globalHandler       The return value of createGlobalHandler().
	 * @param subscriptionManager Subscription manager instance. You can use it to
	 *                            get subscribed clients and more.
	 * @return Newly created handler factory instance. Can be null.
	 */
	ClientHandlerFactory createClientHandlerFactory(SubscriptionKey key, SubscriptionHandler globalHandler,
			SubscriptionManager subscriptionManager);

	interface ClientHandlerFactory {
		/**
		 * Creates a new {@link SubscriptionHandler} instance for client-specific
		 * handling. This is called for every client that subscribes.
		 *
		 * @param client The client to create the subscription handler for.
		 * @return Newly created handler instance. Can be null.
		 */
		SubscriptionHandler createClientSpecificHandler(EdgeClient client);
	}
}
