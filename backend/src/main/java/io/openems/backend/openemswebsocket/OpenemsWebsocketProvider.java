package io.openems.backend.openemswebsocket;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class OpenemsWebsocketProvider {

	private static OpenemsWebsocketServer instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) throws Exception {
		OpenemsWebsocketProvider.instance = new OpenemsWebsocketServer(port);
		OpenemsWebsocketProvider.instance.start();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized OpenemsWebsocketServer getInstance() {
		return OpenemsWebsocketProvider.instance;
	}
}