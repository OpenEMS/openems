package io.openems.edge.battery.soltaro.single.versionb;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.soltaro.ModuleType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SingleRackVersionBImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private ChannelAddress minCellVoltageAdress = new ChannelAddress(BATTERY_ID, "MinCellVoltage");
	private ChannelAddress maxCellVoltageAdress = new ChannelAddress(BATTERY_ID, "MaxCellVoltage");
	private ChannelAddress capacityAdress = new ChannelAddress(BATTERY_ID, "Capacity");
	private ChannelAddress voltageAdress = new ChannelAddress(BATTERY_ID, "Voltage");
	private ChannelAddress maxDischargeCurrentAdress = new ChannelAddress(BATTERY_ID, "DischargeMaxCurrent");
	

	@Test
	public void test() throws Exception {
		new ComponentTest(new SingleRackVersionBImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setErrorLevel2Delay(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setMaxStartAppempts(0) //
						.setStartUnsuccessfulDelay(0) //
						.setMinimalCellVoltage(0) //
						.setStartStop(StartStopConfig.AUTO) //
						.setNumberOfSlaves(0) //
						.setModuleType(ModuleType.MODULE_3_5_KWH) //
						.setWatchdog(0) //
						.setSoCLowAlarm(0) //
						.setReduceTasks(false) //
						.build()) //
				
				
				.next(new TestCase("Empty case"))
				
				/* set min cell voltage below limit, capacity and voltage are needed to calculate charge current */
				.next(new TestCase("test charge necessary")
						.input(minCellVoltageAdress, 2750)
						.input(maxCellVoltageAdress, 3050)
						.input(capacityAdress, 70000)
						.input(voltageAdress, 600)	
						)
				.next(new TestCase().output(maxDischargeCurrentAdress, -2))
				
				
		;
	}

}
