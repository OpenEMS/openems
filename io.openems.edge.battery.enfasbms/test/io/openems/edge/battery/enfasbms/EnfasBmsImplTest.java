package io.openems.edge.battery.enfasbms;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class EnfasBmsImplTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String BATTERY_ID = "battery0";

	private static final ChannelAddress MODULE_0_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module0MaxCellVoltage");
	private static final ChannelAddress MODULE_1_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module1MaxCellVoltage");
	private static final ChannelAddress MODULE_2_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module2MaxCellVoltage");
	private static final ChannelAddress MODULE_3_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module3MaxCellVoltage");
	private static final ChannelAddress MODULE_4_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module4MaxCellVoltage");
	private static final ChannelAddress MODULE_5_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module5MaxCellVoltage");

	private static final ChannelAddress MODULE_0_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module0MinCellVoltage");
	private static final ChannelAddress MODULE_1_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module1MinCellVoltage");
	private static final ChannelAddress MODULE_2_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module2MinCellVoltage");
	private static final ChannelAddress MODULE_3_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module3MinCellVoltage");
	private static final ChannelAddress MODULE_4_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module4MinCellVoltage");
	private static final ChannelAddress MODULE_5_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			"Module5MinCellVoltage");

	private static final ChannelAddress MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID, "MaxCellVoltage");
	private static final ChannelAddress MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID, "MinCellVoltage");

	@Test
	public void test() throws Exception {
		new ComponentTest(new EnfasBmsImpl()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("componentManager", new DummyComponentManager())
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase("1") //
						.input(MODULE_0_MAX_CELL_VOLTAGE, 90) //
						.input(MODULE_1_MAX_CELL_VOLTAGE, 90) //
						.input(MODULE_2_MAX_CELL_VOLTAGE, 90) //
						.input(MODULE_3_MAX_CELL_VOLTAGE, 90) //
						.input(MODULE_4_MAX_CELL_VOLTAGE, 100) //
						.input(MODULE_5_MAX_CELL_VOLTAGE, 90) //
						.output(MAX_CELL_VOLTAGE, 100) //
						.output(MIN_CELL_VOLTAGE, null))
				.next(new TestCase() //
						.input(MODULE_5_MAX_CELL_VOLTAGE, 120) //
						.output(MAX_CELL_VOLTAGE, 120) //
						.output(MIN_CELL_VOLTAGE, null))
				.next(new TestCase() //
						.input(MODULE_0_MIN_CELL_VOLTAGE, 120) //
						.input(MODULE_2_MIN_CELL_VOLTAGE, 120) //
						.input(MODULE_0_MIN_CELL_VOLTAGE, 120) //
						.input(MODULE_3_MIN_CELL_VOLTAGE, 120) //
						.input(MODULE_4_MIN_CELL_VOLTAGE, 120) //
						.input(MODULE_5_MIN_CELL_VOLTAGE, 50) //
						.input(MODULE_1_MIN_CELL_VOLTAGE, 600) //
						.output(MAX_CELL_VOLTAGE, 120) //
						.output(MIN_CELL_VOLTAGE, 50))

		;
	}

}
