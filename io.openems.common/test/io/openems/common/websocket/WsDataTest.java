package io.openems.common.websocket;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WsDataTest {

	@Test
	public void test() {
		var sut = new WsData(null);
		assertEquals("", sut.toLogString());

		sut.dispose();
	}

}
