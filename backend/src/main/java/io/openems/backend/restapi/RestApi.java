package io.openems.backend.restapi;

import io.openems.common.exceptions.OpenemsException;

/**
 * Provider for RestApiSingleton singleton
 *
 * @author stefan.feilmeier
 *
 */
public class RestApi {

	private static RestApiSingleton instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) throws OpenemsException {
		RestApi.instance = new RestApiSingleton(port);
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized RestApiSingleton instance() {
		return RestApi.instance;
	}
}