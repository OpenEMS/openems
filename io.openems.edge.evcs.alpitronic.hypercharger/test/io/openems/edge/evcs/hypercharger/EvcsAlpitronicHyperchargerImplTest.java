package io.openems.edge.evcs.hypercharger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.hypercharger.EvcsAlpitronicHypercharger.Connector;

public class EvcsAlpitronicHyperchargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsAlpitronicHyperchargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

	}
}
