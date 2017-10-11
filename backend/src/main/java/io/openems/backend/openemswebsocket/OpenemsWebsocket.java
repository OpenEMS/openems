package io.openems.backend.openemswebsocket;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class OpenemsWebsocket {

	private static OpenemsWebsocketSingleton instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) {
		OpenemsWebsocket.instance = new OpenemsWebsocketSingleton(port);
		OpenemsWebsocket.instance.start();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized OpenemsWebsocketSingleton instance() {
		return OpenemsWebsocket.instance;
	}
}