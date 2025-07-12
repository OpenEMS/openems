package io.openems.edge.evcs.keba.p40;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsKebaP40ImplTest {

	private TimeLeapClock clock;

	@Test
	public void test() throws Exception {
		this.clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		var test = new ComponentTest(new EvcsKebaP40Impl()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(this.clock))
				.addReference("setModbus", new DummyModbusBridge("modbus0"))//
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false)//
						.setMinHwCurrent(6000)//
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setModbusId("modbus0") //
						.setModbusUnitId(255) //
						.setReadOnly(false) //
						.build()); //
		test.next(new TestCase()//
				.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 10350)// 15A
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000)
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_ENABLE, 1));//
		// no change because time hasnt passed
		test.next(new TestCase()//
				.timeleap(this.clock, 2, ChronoUnit.SECONDS)//
				.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0)// 0A
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000)
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_ENABLE, 1));
		// changes after 5 seconds have past
		test.next(new TestCase()//
				.timeleap(this.clock, 3, ChronoUnit.SECONDS)//
				.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0)// 0A
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_CHARGING_CURRENT, 0)
				.output(EvcsKebaP40.ChannelId.DEBUG_SET_ENABLE, 0));

		test.next(new TestCase()//
				.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 22080) //
				.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 4140) //
				.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE) //
		);
	}

}
