package io.openems.edge.controller.api.rest.readonly;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyUserService;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.api.rest.DummyJsonRpcRestHandlerFactory;
import io.openems.edge.controller.api.rest.JsonRpcRestHandler;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiRestReadOnlyImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();

		new ControllerTest(new ControllerApiRestReadOnlyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("userService", new DummyUserService()) //
				.addReference("restHandlerFactory", new DummyJsonRpcRestHandlerFactory(JsonRpcRestHandler::new)) //
				.activate(MyConfig.create() //
						.setId("ctrlApiRest0") //
						.setEnabled(false) // do not actually start server
						.setConnectionlimit(5) //
						.setDebugMode(false) //
						.setPort(port) //
						.build()) //
				.deactivate();
	}
}
