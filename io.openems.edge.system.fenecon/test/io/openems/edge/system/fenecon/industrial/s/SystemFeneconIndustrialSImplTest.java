package io.openems.edge.system.fenecon.industrial.s;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.system.fenecon.industrial.s.enums.CoolingUnitMode;

public class SystemFeneconIndustrialSImplTest {

	private static final String COMPONENT_ID = "system0";
	private static final String IO_ID = "io0";
	private static final String ESS_ID = "ess0";
	private static final String BATTERY11_ID = "battery11";
	private static final String BATTERY12_ID = "battery12";
	private static final String BATTERY21_ID = "battery21";
	private static final String BATTERY22_ID = "battery22";
	private static final ChannelAddress COOLING_UNIT_ERROR = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress COOLING_UNIT_ENABLE = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress EMERGENCY_STOP_STATE = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress ACKNOWLEDGE_EMERGENCY_STOP = new ChannelAddress(IO_ID, "InputOutput3");
	private static final ChannelAddress SPD_STATE = new ChannelAddress(IO_ID, "InputOutput4");
	private static final ChannelAddress FUSE_STATE = new ChannelAddress(IO_ID, "InputOutput5");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var dummyIo = new DummyInputOutput(IO_ID);
		new ComponentTest(new SystemFeneconIndustrialSImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin())//
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addComponent(dummyIo) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setStartStopConfig(StartStopConfig.START) //
						.setCoolingUnitMode(CoolingUnitMode.AUTO)//
						.setEssId(ESS_ID)//
						.setBatteryIds(BATTERY11_ID, BATTERY12_ID, BATTERY21_ID, BATTERY22_ID)//
						.setCoolingUnitError(COOLING_UNIT_ERROR.toString())//
						.setCoolingUnitEnable(COOLING_UNIT_ENABLE.toString())//
						.setEmergencyStopState(EMERGENCY_STOP_STATE.toString()) //
						.setAcknowledgeEmergencyStop(ACKNOWLEDGE_EMERGENCY_STOP.toString())//
						.setSpdState(SPD_STATE.toString())//
						.setFuseState(FUSE_STATE.toString())//
						.build())//
		;
	}
}
