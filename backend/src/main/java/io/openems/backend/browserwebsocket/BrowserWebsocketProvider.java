package io.openems.backend.browserwebsocket;

import io.openems.backend.openemswebsocket.OpenemsWebsocketServer;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketProvider {

	private static BrowserWebsocketServer instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) throws Exception {
		BrowserWebsocketProvider.instance = new BrowserWebsocketServer(port);
		BrowserWebsocketProvider.instance.start();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized OpenemsWebsocketServer getInstance() {
		return BrowserWebsocketServer.instance;
	}
}