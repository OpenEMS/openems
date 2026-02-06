package io.openems.edge.controller.ess.sohcycle;

import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.IS_BATTERY_BALANCED;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;

import org.junit.Test;

import io.openems.common.test.TestUtils;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class CheckBalancingHandlerTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	@Test
	public void testBalancingSuccessWhenVoltageDeltaBelowThreshold() throws Exception {
		final var setup = this.createTestSetup();
		final var test = this.createControllerTest(setup.clock(), setup.ess(),
				ControllerEssSohCycleImpl.startIn(StateMachine.State.CHECK_BALANCING));
		test.addReference("sum", new DummySum());
		setup.power().addEss(setup.ess());

		test.next(new TestCase("Battery balanced: delta 30mV < threshold 50mV")
				.input(ESS_ID, MIN_CELL_VOLTAGE, 3200) // 3200 mV
				.input(ESS_ID, MAX_CELL_VOLTAGE, 3230)) // 3230 mV -> delta = 30 mV
				.next(new TestCase()
				.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING)
				.output(IS_BATTERY_BALANCED, BatteryBalanceStatus.BALANCED))
				.deactivate();
	}


	@Test
	public void testBalancingWithExactThresholdValue() throws Exception {
		final var setup = this.createTestSetup();
		final var test = this.createControllerTest(setup.clock(), setup.ess(),
				ControllerEssSohCycleImpl.startIn(StateMachine.State.CHECK_BALANCING));
		test.addReference("sum", new DummySum());
		setup.power().addEss(setup.ess());

		test.next(new TestCase("Voltage delta exactly at threshold (50mV = 50mV)")
				.input(ESS_ID, MIN_CELL_VOLTAGE, 3200)
				.input(ESS_ID, MAX_CELL_VOLTAGE, 3250)) // delta = 50 mV
				.next(new TestCase()
				.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING)
				.output(IS_BATTERY_BALANCED, BatteryBalanceStatus.BALANCED))
				.deactivate();
	}

	@Test
	public void testBalancingMissingDataDefaultsToNotMeasured() throws Exception {
		final var setup = this.createTestSetup();
		final var test = this.createControllerTest(setup.clock(), setup.ess(),
				ControllerEssSohCycleImpl.startIn(StateMachine.State.CHECK_BALANCING));
		test.addReference("sum", new DummySum());
		setup.power().addEss(setup.ess());

		test.next(new TestCase("Missing cell voltage data yields NOT_MEASURED"))
				.next(new TestCase()
				.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING)
				.output(IS_BATTERY_BALANCED, BatteryBalanceStatus.NOT_MEASURED))
				.deactivate();
	}

	private TestSetup createTestSetup() {
		final var clock = TestUtils.createDummyClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = new DummyManagedSymmetricEss(ESS_ID)
				.withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER)
				.withCapacity(DEFAULT_CAPACITY);
		return new TestSetup(clock, power, ess);
	}

	private record TestSetup(TimeLeapClock clock, DummyPower power, DummyManagedSymmetricEss ess) {
	}

	private ControllerTest createControllerTest(TimeLeapClock clock, DummyManagedSymmetricEss ess,
			ControllerEssSohCycleImpl controller) throws Exception {
		return new ControllerTest(controller)
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("ess", ess)
				.activate(MyConfig.create()
						.setId(CONTROLLER_ID)
						.setEssId(ESS_ID)
						.setMode(Mode.MANUAL_ON)
						.build());
	}
}

