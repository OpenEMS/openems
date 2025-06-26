package io.openems.common.websocket;

import org.junit.Test;

public class DummyWebsocketServerTest {

	@Test
	public void test() {
		var sut = DummyWebsocketServer.create() //
				.build();
		sut.createWsData(null);
		sut.stop();
	}

}
