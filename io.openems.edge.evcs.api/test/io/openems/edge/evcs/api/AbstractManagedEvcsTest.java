package io.openems.edge.evcs.api;

import static io.openems.edge.evcs.api.Evcs.ChannelId.ENERGY_SESSION;
import static io.openems.edge.evcs.api.Evcs.ChannelId.STATUS;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.CHARGE_STATE;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_ENERGY_LIMIT;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class AbstractManagedEvcsTest {

	/**
	 * Sleep between every TestCase to make sure that the Channel Values are added
	 * to the pastValues Map. This is required because the Channel Value timestamp
	 * does not consider the mocked Clock.
	 * 
	 * <p>
	 * Timeleap is not used, to avoid using a clock in the ChargeSatusHandler and
	 * therefore a ClockProvider function in every EVCS (.timeleap(clock, 31,
	 * ChronoUnit.SECONDS))
	 */
	private static final ThrowingRunnable<Exception> SLEEP = () -> Thread.sleep(1010);

	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyEvcsPower EVCS_POWER_WITH_FILTER = new DummyEvcsPower(new RampFilter());
	private static final DummyManagedEvcs EVCS0 = new DummyManagedEvcs("evcs0", EVCS_POWER);
	private static final DummyManagedEvcs EVCS1 = new DummyManagedEvcs("evcs1", EVCS_POWER);
	private static final DummyManagedEvcs EVCS2 = new DummyManagedEvcs("evcs2", EVCS_POWER_WITH_FILTER);
	private static final int MINIMUM = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	private static final int MAXIMUM = Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;

	/*
	 * ATTENTION: The test could fail if you run it in Debug mode and e.g. the test
	 * is expecting an output within the "getMinimumTimeTillCharingLimitTaken" time.
	 */
	@Test
	public void abstractManagedEvcsTest() throws Exception {
		var test = new ComponentTest(EVCS0) //
				.addComponent(EVCS0) //

				.next(new TestCase("Initial charge") //
						.input(SET_CHARGE_POWER_LIMIT, 15000) //
						.input(ACTIVE_POWER, 0) //
						.input(STATUS, Status.READY_FOR_CHARGING) //
						.output(ACTIVE_POWER, 15000) //
						.output(CHARGE_STATE, ChargeState.INCREASING)); //

		// Cannot check the nextValue of SetChargePowerLimit as output, because the test
		// validator checks the write value
		// (.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 15000))
		assertEquals("Check next value of setChargePowerLimit", 15000, //
				(EVCS0.<IntegerReadChannel>channel(SET_CHARGE_POWER_LIMIT)).getNextValue()
						.orElse(0).intValue());

		test //
				.next(new TestCase("Check ChargeState after 'getMinimumTimeTillCharingLimitTaken'") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 15000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.CHARGING))

				.next(new TestCase("Decrease Power") //
						.input(ACTIVE_POWER, 15000) //
						.input(SET_CHARGE_POWER_LIMIT, 8000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				// Enough power to increase, but 'MinimumTimeTillCharingLimitTaken' is not
				// expired
				.next(new TestCase("Stay in decreasing charge state") //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT, 20000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT, 20000) //
						.output(ACTIVE_POWER, 20000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.INCREASING))

				// Charge power is increasing but decrease has higher priority than pause
				.next(new TestCase("Decrease has highest priority") //
						.input(ACTIVE_POWER, 20000) //
						.input(SET_CHARGE_POWER_LIMIT, 0) //
						.output(ACTIVE_POWER, 0) //
						.output(STATUS, Status.CHARGING_REJECTED) //
						.output(CHARGE_STATE, ChargeState.DECREASING)); //
	}

	@Test
	public void abstractManagedEvcsStateChangesTest() throws Exception {
		new ComponentTest(EVCS1) //
				.addComponent(EVCS1) //

				.next(new TestCase("Initial charge") //
						.input(SET_CHARGE_POWER_LIMIT, 15000) //
						.input(ACTIVE_POWER, 0) //
						.input(STATUS, Status.READY_FOR_CHARGING) //
						.input(ENERGY_SESSION, 9999) //
						.input(SET_ENERGY_LIMIT, 10000) //
						.output(ACTIVE_POWER, 15000) //
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Energy limit reached") //
						.input(ACTIVE_POWER, 15000) //
						.input(ENERGY_SESSION, 10000) //
						.input(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING) //
						.output(STATUS, Status.ENERGY_LIMIT_REACHED) //
						.output(ACTIVE_POWER, 0))

				.next(new TestCase("Energy limit increased - still in pause") //
						.input(ACTIVE_POWER, 0) //
						.input(ENERGY_SESSION, 10000) //
						.input(SET_ENERGY_LIMIT, 20000) //
						.input(SET_CHARGE_POWER_LIMIT, 15000) //
						.output(ACTIVE_POWER, 0) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("Energy limit increased - after pause") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 0) //
						.input(ENERGY_SESSION, 10000) //
						.input(SET_ENERGY_LIMIT, 20000) //
						.input(SET_CHARGE_POWER_LIMIT, 15000) //
						.output(ACTIVE_POWER, 15000) //
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Decrease Power") //
						.input(ACTIVE_POWER, 15000) //
						.input(SET_CHARGE_POWER_LIMIT, 8000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("Stay in decreasing charge state; 'MinimumTimeTillCharingLimitTaken' is not expired") //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT, 20000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT, 20000) //
						.output(ACTIVE_POWER, 20000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Charge power is increasing, but decrease has highest priority") //
						.input(ACTIVE_POWER, 20000) //
						.input(SET_CHARGE_POWER_LIMIT, 0) //
						.output(ACTIVE_POWER, 0) //
						.output(STATUS, Status.CHARGING_REJECTED) //
						.output(CHARGE_STATE, ChargeState.DECREASING)); //
	}

	@Test
	public void abstractManagedEvcsWithFilterTest() throws Exception {
		ComponentTest test = new ComponentTest(EVCS2) //
				.addComponent(EVCS2);

		// Initial charge
		int initialResult = (int) (MINIMUM + MAXIMUM * EVCS2.getEvcsPower().getIncreaseRate()); // 5244

		test //
				.next(new TestCase("Initial charge") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 15000) //
						.input(ACTIVE_POWER, 0) //
						.input(STATUS, Status.READY_FOR_CHARGING) //
						.output(ACTIVE_POWER, initialResult) //
						.output(CHARGE_STATE, ChargeState.INCREASING)); //

		// Cannot check the nextValue of SetChargePowerLimit as output, because the test
		// validator checks the write value
		// (.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT,
		// initialResult))
		assertEquals("Check next value of setChargePowerLimit", initialResult, //
				(EVCS2.<IntegerReadChannel>channel(SET_CHARGE_POWER_LIMIT)).getNextValue()
						.orElse(0).intValue());

		int increasingValue = (int) (MAXIMUM * EVCS2.getEvcsPower().getIncreaseRate());
		test //
				.next(new TestCase("Further charge") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 15000) //
						.input(ACTIVE_POWER, 5244) //
						.input(STATUS, Status.CHARGING) //
						// 6348 W
						.output(ACTIVE_POWER, (int) (initialResult + increasingValue))
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Further charge") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 15000) //
						.input(ACTIVE_POWER, 6348) //
						.input(STATUS, Status.CHARGING) //
						.output(ACTIVE_POWER, initialResult + increasingValue * 2) // 7452 W
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Further charge") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 15000) //
						.input(ACTIVE_POWER, 6348) //
						.input(STATUS, Status.CHARGING) //
						.output(ACTIVE_POWER, initialResult + increasingValue * 3) // 8556 W
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Further charge") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 15000) //
						.input(ACTIVE_POWER, 6348) //
						.input(STATUS, Status.CHARGING) //
						.output(ACTIVE_POWER, initialResult + increasingValue * 4) // 9660 W
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase("Further charge - reached target") //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 10000) //
						.input(ACTIVE_POWER, 6348) //
						.input(STATUS, Status.CHARGING) //
						.output(ACTIVE_POWER, 10_000) // 10000 W
						.output(CHARGE_STATE, ChargeState.INCREASING))

				.next(new TestCase(
						"Wait till charge limit is taken. Check ChargeState after 'getMinimumTimeTillCharingLimitTaken'") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 10000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.CHARGING))

				.next(new TestCase("Decrease Power") //
						.input(ACTIVE_POWER, 10000) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 8000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				// Enough power to increase, but 'MinimumTimeTillCharingLimitTaken' is not
				// expired
				.next(new TestCase("Stay in decreasing charge state") //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 20000) //
						.output(ACTIVE_POWER, 8000) //
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 8000) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 20000) //
						.output(ACTIVE_POWER, 8000 + increasingValue) // 9104
						.output(STATUS, Status.CHARGING) //
						.output(CHARGE_STATE, ChargeState.INCREASING))

				// Charge power is increasing but decrease has higher priority than pause
				.next(new TestCase("Decrease has highest priority") //
						.input(ACTIVE_POWER, 20000) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 0) //
						.output(ACTIVE_POWER, 0) //
						.output(STATUS, Status.CHARGING_REJECTED) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("Charging stopped - still in pause state") //
						.input(ACTIVE_POWER, 0) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 0) //
						.output(ACTIVE_POWER, 0) //
						.output(STATUS, Status.CHARGING_REJECTED) //
						.output(CHARGE_STATE, ChargeState.DECREASING))

				.next(new TestCase("Charging stopped") //
						.onAfterProcessImage(SLEEP) //
						.input(ACTIVE_POWER, 0) //
						.input(SET_CHARGE_POWER_LIMIT_WITH_FILTER, 0) //
						.output(ACTIVE_POWER, 0) //
						.output(STATUS, Status.CHARGING_REJECTED) //
						.output(CHARGE_STATE, ChargeState.NOT_CHARGING)); //
	}
}
