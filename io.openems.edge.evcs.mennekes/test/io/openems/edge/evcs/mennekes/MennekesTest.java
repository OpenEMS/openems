package io.openems.edge.evcs.mennekes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;

public class MennekesTest {

	private static final String EVCS_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsMennekesImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)//
						// Task 2
						.withRegister(104, 6))//

				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(EVCS_ID) //
						.setModbusUnitId(1) //
						.setMaxHwPower(10_000) //
						.setMinHwPower(5_000) //
						.build()) //

				.next(new TestCase()//
						.output(new ChannelAddress(EVCS_ID, EvcsMennekes.ChannelId.OCPP_CP_STATUS.id()),
								MennekesOcppState.CHARGING)
						.output(new ChannelAddress(EVCS_ID, Evcs.ChannelId.STATUS.id()), Status.CHARGING));
	}

	@Test
	public void parseSoftwareVersionTest() {
		// raw value from fems4 test
		int registerValue = 892219954;
		var firmwareVersion = EvcsMennekesImpl.parseSoftwareVersion(registerValue);
		assertEquals("5.22", firmwareVersion);
		// raw value from fems4 test
		registerValue = 892219698;
		firmwareVersion = EvcsMennekesImpl.parseSoftwareVersion(registerValue);
		assertEquals("5.12", firmwareVersion);
	}
}
