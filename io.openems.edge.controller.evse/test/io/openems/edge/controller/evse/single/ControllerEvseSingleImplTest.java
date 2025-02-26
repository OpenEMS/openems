package io.openems.edge.controller.evse.single;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;

public class ControllerEvseSingleImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEvseSingleImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("chargePoint", new DummyEvseChargePoint("chargePoint0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setDebugMode(true) //
						.setMode(Mode.MINIMUM) //
						.setChargePointId("chargePoint0") //
						.setElectricVehicleId("electricVehicle0") //
						.setSmartConfig("") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void smartTest() throws Exception {
		final var clock = createDummyClock();
		final var sut = new ControllerEvseSingleImpl(clock);
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("chargePoint", new DummyEvseChargePoint("chargePoint0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setMode(Mode.SMART) //
						.setChargePointId("chargePoint0") //
						.setElectricVehicleId("electricVehicle0") //
						.setSmartConfig("""
								[{
								  "@type": "Task",
								  "uid": "00000000-0000-0000-0000-000000000000",
								  "updated": "2020-01-01T00:00:00Z",
								  "start": "2024-06-17T00:00:00",
								  "recurrenceRules": [
								    {
								      "frequency": "weekly",
								      "byDay": [
								        "sa",
								        "su"
								      ]
								    }
								  ],
								  "openems.io:payload": {
								    "sessionEnergy": 10001
								  }
								}]""") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
