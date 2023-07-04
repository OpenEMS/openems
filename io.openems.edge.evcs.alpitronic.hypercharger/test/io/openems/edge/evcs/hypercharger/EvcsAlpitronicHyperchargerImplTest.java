package io.openems.edge.evcs.hypercharger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.hypercharger.EvcsAlpitronicHypercharger.Connector;

public class EvcsAlpitronicHyperchargerImplTest {

	private static final String EVCS_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsAlpitronicHyperchargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(EVCS_ID) //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

	}
}
