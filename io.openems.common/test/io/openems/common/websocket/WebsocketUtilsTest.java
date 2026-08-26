package io.openems.common.websocket;

import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WebsocketUtilsTest {

	@Test
	public void test() {
		assertEquals("", generateWsDataString(null));
	}

}
