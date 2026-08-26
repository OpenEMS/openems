package io.openems.edge.controller.api.websocket;

import static io.openems.edge.controller.api.websocket.ControllerApiWebsocket.DEFAULT_PORT;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiWebsocketImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiWebsocketImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("onRequestFactory", new DummyOnRequestFactory()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setApiTimeout(60) //
						.setPort(DEFAULT_PORT) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
