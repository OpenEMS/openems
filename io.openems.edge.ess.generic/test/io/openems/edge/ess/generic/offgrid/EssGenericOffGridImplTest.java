package io.openems.edge.ess.generic.offgrid;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyOffGridBatteryInverter;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyOffGridSwitch;

public class EssGenericOffGridImplTest {

	private static final String ESS_ID = "ess0";
	private static final String BATTERY_ID = "battery0";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String OFF_GRID_SWITCH_ID = "offGridSwitch0";

	@Test
	public void testStart() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new EssGenericOffGridImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("batteryInverter", new DummyOffGridBatteryInverter(BATTERY_INVERTER_ID)) //
				.addReference("battery", new DummyBattery(BATTERY_ID)) //
				.addReference("offGridSwitch", new DummyOffGridSwitch(OFF_GRID_SWITCH_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setBatteryId(BATTERY_ID) //
						.setOffGridSwitchId(OFF_GRID_SWITCH_ID) //
						.build()) //
		;
	}
}
