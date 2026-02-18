package io.openems.edge.solaredge.ess;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.solaredge.ess.charger.SolarEdgeChargerImpl;
import io.openems.edge.solaredge.ess.enums.ControlMode;

public class SolarEdgeEssImplTest {

	@Test
	public void test() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.ess.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(charger)
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
