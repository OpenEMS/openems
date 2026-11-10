package io.openems.common.websocket;

import java.time.Duration;

import org.eclipse.jetty.websocket.core.Configuration;

/**
 * Common Jetty WebSocket {@link Configuration} for servers and clients.
 */
final class JettyConfiguration {

	private JettyConfiguration() {
	}

	/**
	 * Applies the common settings.
	 *
	 * <ul>
	 * <li>No idle timeout: lost connections are detected by pings, see
	 * {@link JettyWebSocket#checkConnectionLost(Duration)}
	 * <li>No limit for frame and message size, as OpenEMS sends large JSON-RPC
	 * messages, e.g. the EdgeConfig
	 * </ul>
	 *
	 * @param configuration the {@link Configuration}
	 */
	static void apply(Configuration configuration) {
		configuration.setIdleTimeout(Duration.ZERO);
		configuration.setMaxFrameSize(Integer.MAX_VALUE);
		configuration.setMaxTextMessageSize(Integer.MAX_VALUE);
		configuration.setMaxBinaryMessageSize(Integer.MAX_VALUE);
	}
}
