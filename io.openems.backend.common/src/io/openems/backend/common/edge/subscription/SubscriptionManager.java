package io.openems.backend.common.edge.subscription;

import java.util.List;

import io.openems.backend.common.edge.EdgeClient;

public interface SubscriptionManager {
	/**
	 * Registers a factory that creates handlers for subscription events.
	 *
	 * @param factory the factory to register
	 */
	void registerHandlerFactory(SubscriptionHandlerFactory factory);

	/**
	 * Unregisters a previously registered handler factory.
	 *
	 * @param factory the factory to unregister
	 */
	void unregisterHandlerFactory(SubscriptionHandlerFactory factory);

	/**
	 * Returns all currently subscribed clients for the given key.
	 *
	 * @param key the subscription key
	 * @return the subscribed clients
	 */
	List<EdgeClient> getSubscribedClients(SubscriptionKey key);

	/**
	 * Subscribes a client to the given key and returns the created subscription
	 * state.
	 *
	 * @param key    the subscription key
	 * @param client the client to subscribe
	 * @return the active client subscription
	 */
	ActiveClientSubscription subscribe(SubscriptionKey key, EdgeClient client);

	/**
	 * Unsubscribes a client from the given key.
	 *
	 * @param key    the subscription key
	 * @param client the client to unsubscribe
	 * @return the removed subscription data; may be {@code null} if no subscription
	 *         existed
	 */
	SubscriptionData unsubscribe(SubscriptionKey key, EdgeClient client);

	/**
	 * Returns all subscriptions of a client.
	 *
	 * @param client the client
	 * @return all subscriptions of the client
	 */
	List<SubscriptionData> getSubscriptionsByClient(EdgeClient client);

	/**
	 * Removes all subscriptions and related resources for a client. Must be called
	 * when the client disconnects.
	 *
	 * @param client the client to clean up
	 */
	void cleanupClient(EdgeClient client);

	/**
	 * Bundles subscription data with resolved handlers for a subscribed client.
	 *
	 * @param subscriptionData the underlying subscription data
	 * @param globalHandler    the handler for global subscription events
	 * @param clientHandler    the handler for client-specific subscription events
	 */
	record ActiveClientSubscription(SubscriptionData subscriptionData, SubscriptionHandler globalHandler,
			SubscriptionHandler clientHandler) {
	}
}
