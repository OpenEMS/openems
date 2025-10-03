package io.openems.edge.evcs.heidelberg.energy;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.PhaseRotation;

public class EvcsHeidelbergEnergyImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsHeidelbergEnergyImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMinHwCurrent(Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT) //
						.setMaxHwCurrent(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setLimitPhases(true) //
						.setReadOnly(true) //
						.build()) //
				.next(new TestCase());
	}

}