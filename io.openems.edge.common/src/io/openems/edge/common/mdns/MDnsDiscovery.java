package io.openems.edge.common.mdns;

import java.net.Inet4Address;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface MDnsDiscovery {

	public sealed interface MDnsEvent {

		/**
		 * Returns the service name associated with this event.
		 * 
		 * @return the service name
		 */
		String serviceName();

		/**
		 * Returns the timestamp when this event occurred.
		 * 
		 * @return the event timestamp
		 */
		Instant timestamp();

		public record ServiceAdded(String serviceName, Instant timestamp) implements MDnsEvent {
		}

		public record ServiceRemoved(String serviceName, Instant timestamp) implements MDnsEvent {
		}

		public record ServiceResolved(String serviceName, Instant timestamp, List<Inet4Address> addresses,
				Map<String, String> properties) implements MDnsEvent {
		}
	}

	/**
	 * Subscribe to mDNS service events for a specific service type.
	 * 
	 * @param serviceType the service type, e.g., "_http._tcp.local."
	 * @param onChange    callback to handle mDNS events
	 * @return an AutoCloseable to unsubscribe from the service events
	 */
	AutoCloseable subscribeService(String serviceType, Consumer<MDnsEvent> onChange);

	/**
	 * Subscribe to mDNS service events for a specific service type and name.
	 * 
	 * @param serviceType the service type, e.g., "_http._tcp.local."
	 * @param serviceName the service name, e.g., "My Service"
	 * @param onChange    callback to handle mDNS events
	 * @return an AutoCloseable to unsubscribe from the service events
	 */
	AutoCloseable subscribeService(String serviceType, String serviceName, Consumer<MDnsEvent> onChange);

}
