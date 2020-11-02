package io.openems.edge.battery.soltaro.single.versionc;

import org.junit.Test;

import io.openems.edge.battery.soltaro.ModuleType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SingleRackVersionCImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SingleRackVersionCImpl()) //
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
						.setSocLowAlarm(0) //
						.build()) //
		;
	}

}
