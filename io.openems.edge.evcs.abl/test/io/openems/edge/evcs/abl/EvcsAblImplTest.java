package io.openems.edge.evcs.abl;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.PhaseRotation;

public class EvcsAblImplTest {

	@Test
	public void plug1Test() throws Exception {
		new ComponentTest(new EvcsAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setPlug(Plug.PLUG_1) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMinHwCurrent(Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT) //
						.setMaxHwCurrent(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setDebugMode(false) //
						.setReadOnly(true) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void plug2Test() throws Exception {
		new ComponentTest(new EvcsAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setPlug(Plug.PLUG_2) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMinHwCurrent(Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT) //
						.setMaxHwCurrent(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setDebugMode(false) //
						.setReadOnly(true) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
