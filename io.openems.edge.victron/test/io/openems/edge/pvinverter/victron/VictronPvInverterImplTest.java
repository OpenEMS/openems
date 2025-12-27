package io.openems.edge.pvinverter.victron;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.victron.pvinverter.VictronPvInverter;
import io.openems.edge.victron.pvinverter.VictronPvInverterImpl;
import io.openems.edge.common.test.ComponentTest;

public class VictronPvInverterImplTest {

	private static final String PV_INVERTER_ID = "pvInverter0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(PV_INVERTER_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(PV_INVERTER_ID, "ActivePowerL2");
	private static final ChannelAddress ACTIVE_POWER_L3 = new ChannelAddress(PV_INVERTER_ID, "ActivePowerL3");
	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(PV_INVERTER_ID, "VoltageL1");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(1026,
								// POSITION (register 1026)
								0,
								// VOLTAGE_L1 (register 1027) - 23000 = 230.00V
								23000,
								// CURRENT_L1 (register 1028) - 500 = 5.00A
								500,
								// ACTIVE_POWER_L1 (register 1029) - 1000W
								1000,
								// DUMMY (register 1030)
								0,
								// VOLTAGE_L2 (register 1031) - 23100 = 231.00V
								23100,
								// CURRENT_L2 (register 1032) - 520 = 5.20A
								520,
								// ACTIVE_POWER_L2 (register 1033) - 1200W
								1200,
								// DUMMY (register 1034)
								0,
								// VOLTAGE_L3 (register 1035) - 22900 = 229.00V
								22900,
								// CURRENT_L3 (register 1036) - 350 = 3.50A
								350,
								// ACTIVE_POWER_L3 (register 1037) - 800W
								800,
								// DUMMY (register 1038)
								0,
								// SERIAL_NUMBER (registers 1039-1045) - 7 words
								0x4142, 0x4344, 0x4546, 0x4748, 0x494A, 0x4B4C, 0x4D00,
								// ACTIVE_PRODUCTION_ENERGY_L1 (registers 1046-1047)
								0, 10000,
								// ACTIVE_PRODUCTION_ENERGY_L2 (registers 1048-1049)
								0, 12000,
								// ACTIVE_PRODUCTION_ENERGY_L3 (registers 1050-1051)
								0, 8000)) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setAlias("Victron PV Inverter") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(20) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER_L1, 1000) //
						.output(ACTIVE_POWER_L2, 1200) //
						.output(ACTIVE_POWER_L3, 800) //
						.output(VOLTAGE_L1, 2300000));
	}

	@Test
	public void testChannelIds() {
		var channelIds = VictronPvInverter.ChannelId.values();
		assertNotNull(channelIds);
	}

	@Test
	public void testConstructor() throws Exception {
		var pvInverter = new VictronPvInverterImpl();
		assertNotNull(pvInverter);
	}

}
