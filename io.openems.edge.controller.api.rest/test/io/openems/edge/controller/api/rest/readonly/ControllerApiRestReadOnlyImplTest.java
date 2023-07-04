package io.openems.edge.controller.api.rest.readonly;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyUserService;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.timedata.test.DummyTimedata;

public class ControllerApiRestReadOnlyImplTest {

	private static final String CTRL_ID = "ctrlApiRest0";

	@Test
	public void test() throws OpenemsException, Exception {
		final var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();

		new ControllerTest(new ControllerApiRestReadOnlyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("userService", new DummyUserService()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) // do not actually start server
						.setConnectionlimit(5) //
						.setDebugMode(false) //
						.setPort(port) //
						.build());
	}
}
