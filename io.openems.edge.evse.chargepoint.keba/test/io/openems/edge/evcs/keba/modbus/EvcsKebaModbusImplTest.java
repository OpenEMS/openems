package io.openems.edge.evcs.keba.modbus;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testDeprecatedEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testElectricityMeterChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testManagedEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.EvcsKebaTest.testEvcsKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.prepareKebaModbus;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.testKebaModbusChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaTest.testKebaChannels;
import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

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
import io.openems.edge.evse.chargepoint.keba.common.Keba;

public class EvcsKebaModbusImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new EvcsKebaModbusImpl()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false)//
						.setMinHwCurrent(6000)//
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setModbusId("modbus0") //
						.setModbusUnitId(255) //
						.setReadOnly(false) //
						.build()) //

				.next(new TestCase()//
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 10350) // 15A
						.output(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000))

				.next(new TestCase("no change because time hasnt passed") //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0) // 0A
						.output(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000))

				.next(new TestCase("changes after 5 seconds have passed") //
						.timeleap(clock, 3, ChronoUnit.SECONDS) //
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0) // 0A
						.output(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT, 0))

				.next(new TestCase()//
						.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 22080) //
						.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 4140) //
						.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE)) //

				.deactivate();
	}

	@Test
	public void test2() throws Exception {
		final var sut = new EvcsKebaModbusImpl();
		final var tc = new TestCase();
		testEvcsChannels(tc);
		testManagedEvcsChannels(tc);
		testDeprecatedEvcsChannels(tc);
		testElectricityMeterChannels(tc);
		testKebaChannels(tc);
		testKebaModbusChannels(tc);
		testEvcsKebaChannels(tc);

		prepareKebaModbus(sut) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false)//
						.setMinHwCurrent(6000)//
						.setPhaseRotation(L2_L3_L1) //
						.setModbusId("modbus0") //
						.setModbusUnitId(255) //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase(), 20) //
				.next(tc) //
				.deactivate();

		assertEquals("L:5678 W|SetCurrent:UNDEFINED|SetEnable:-1:Undefined", sut.debugLog());
	}
}
