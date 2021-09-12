package io.openems.edge.sma;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class SunnyIsland6EssTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new SunnyIsland6Ess()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhase(SinglePhase.L1) //
						.build()) //
		;
	}

}
