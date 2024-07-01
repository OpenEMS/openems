package io.openems.edge.controller.api.websocket;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiWebsocketImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiWebsocketImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("onRequestFactory", new DummyOnRequestFactory()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setApiTimeout(60) //
						.setPort(ControllerApiWebsocket.DEFAULT_PORT) //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
