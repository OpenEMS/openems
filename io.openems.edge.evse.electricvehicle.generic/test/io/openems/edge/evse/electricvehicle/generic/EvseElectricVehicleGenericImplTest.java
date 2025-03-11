package io.openems.edge.evse.electricvehicle.generic;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvseElectricVehicleGenericImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseElectricVehicleGenericImpl()) //
				.activate(MyConfig.create() //
						.setId("evseElectricVehicle0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
