package io.openems.edge.evcs.mennekes;

import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsMennekesImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsMennekesImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0", LogVerbosity.READS_AND_WRITES)//
						// Task 2
						.withRegister(104, 6) //
						.withRegisters(206,
								// Active Power L1, L2, L3
								0, 100, 0, 200, 0, 300, //
								// Current L1, L2, L3
								0, 220, 0, 230, 0, 240)) //

				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setMaxHwPower(10_000) //
						.setMinHwPower(5_000) //
						.setPhaseRotation(L2_L3_L1) //
						.build()) //

				.next(new TestCase()//
						.output(EvcsMennekes.ChannelId.OCPP_CP_STATUS, MennekesOcppState.CHARGING) //
						.output(Evcs.ChannelId.STATUS, Status.CHARGING)) //

				.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 600) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 300) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 100) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 200) //
						.output(ElectricityMeter.ChannelId.CURRENT, 690) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 240) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 220) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 230)) //

				.deactivate();
	}

	@Test
	public void parseSoftwareVersionTest() {
		// raw value from real test
		int registerValue = 892219954;
		var firmwareVersion = EvcsMennekesImpl.parseSoftwareVersion(registerValue);
		assertEquals("5.22", firmwareVersion);
		// raw value from real test
		registerValue = 892219698;
		firmwareVersion = EvcsMennekesImpl.parseSoftwareVersion(registerValue);
		assertEquals("5.12", firmwareVersion);
	}
}
