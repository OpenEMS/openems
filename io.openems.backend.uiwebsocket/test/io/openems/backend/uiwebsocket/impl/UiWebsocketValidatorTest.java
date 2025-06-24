package io.openems.backend.uiwebsocket.impl;

import io.openems.common.websocket.DummyWebsocketServer;

public class UiWebsocketValidatorTest {

	/**
	 * This tests the {@link UiWebsocketValidator}.
	 * 
	 * @param args args
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		var sut = new UiWebsocketValidator();
		try (final var server = DummyWebsocketServer.create() //
				.build()) {
			sut.start(server);

			Thread.sleep(60_000);
		}
	}

}
