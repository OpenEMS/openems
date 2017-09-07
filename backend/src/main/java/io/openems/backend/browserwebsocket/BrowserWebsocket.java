package io.openems.backend.browserwebsocket;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocket {

	private static BrowserWebsocketSingleton instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) throws Exception {
		BrowserWebsocket.instance = new BrowserWebsocketSingleton(port);
		BrowserWebsocket.instance.start();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized BrowserWebsocketSingleton instance() {
		return BrowserWebsocket.instance;
	}
}