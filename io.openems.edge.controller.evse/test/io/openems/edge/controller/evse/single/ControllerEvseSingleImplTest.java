package io.openems.edge.controller.evse.single;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.chargepoint.Mode;

public class ControllerEvseSingleImplTest {

	@Test(expected = ReflectionUtils.ReflectionException.class) // TODO
	public void test() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEvseSingleImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setDebugMode(true) //
						.setMode(Mode.SMART) //
						.setChargePointId("chargePoint0") //
						.setElectricVehicleId("electricVehicle0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
