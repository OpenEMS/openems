package io.openems.edge.evcs.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class AbstractManagedEvcsTest {

	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyEvcsPower EVCS_POWER_WITH_FILTER = new DummyEvcsPower(new RampFilter());
	private static final DummyManagedEvcs EVCS0 = new DummyManagedEvcs("evcs0", EVCS_POWER);
	private static final DummyManagedEvcs EVCS1 = new DummyManagedEvcs("evcs1", EVCS_POWER);
	private static final DummyManagedEvcs evcs2 = new DummyManagedEvcs("evcs2", EVCS_POWER_WITH_FILTER);
	private static final int MINIMUM = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	private static final int MAXIMUM = Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;

	// Channel Addresses EVCS0
	private static ChannelAddress evcs0Status = new ChannelAddress("evcs0", "Status");
	private static ChannelAddress evcs0ChargeState = new ChannelAddress("evcs0", "ChargeState");
	private static ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
	private static ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");

	// Channel Addresses EVCS1
	private static ChannelAddress evcs1Status = new ChannelAddress("evcs1", "Status");
	private static ChannelAddress evcs1ChargeState = new ChannelAddress("evcs1", "ChargeState");
	private static ChannelAddress evcs1ChargePower = new ChannelAddress("evcs1", "ChargePower");
	private static ChannelAddress evcs1SetChargePowerLimit = new ChannelAddress("evcs1", "SetChargePowerLimit");
	private static ChannelAddress evcs1SetEnergyLimit = new ChannelAddress("evcs1", "SetEnergyLimit");
	private static ChannelAddress evcs1EnergySession = new ChannelAddress("evcs1", "EnergySession");

	// Channel Addresses EVCS 2
	private static ChannelAddress evcs2Status = new ChannelAddress("evcs2", "Status");
	private static ChannelAddress evcs2ChargeState = new ChannelAddress("evcs2", "ChargeState");
	private static ChannelAddress evcs2ChargePower = new ChannelAddress("evcs2", "ChargePower");
	private static ChannelAddress evcs2SetChargePowerLimitWithFilter = new ChannelAddress("evcs2",
			"SetChargePowerLimitWithFilter");

	/*
	 * ATTENTION: The test could fail if you run it in Debug mode and e.g. the test
	 * is expecting an output within the "getMinimumTimeTillCharingLimitTaken" time.
	 */

	@Test
	public void abstractManagedEvcsTest() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(1010);

		ComponentTest test = new ComponentTest(EVCS0).addComponent(EVCS0);

		// Initial charge
		test.next(new TestCase("Initial charge") //

				.input(evcs0SetChargePowerLimit, 15000) //
				.input(evcs0ChargePower, 0) //
				.input(evcs0Status, Status.READY_FOR_CHARGING) //

				.output(evcs0ChargePower, 15000) //
				.output(evcs0ChargeState, ChargeState.INCREASING)); //

		/*
		 * Cannot check the nextValue of SetChargePowerLimit as output, because the test
		 * validator checks the write value (.output(evcs0SetChargePowerLimit, 15000))
		 */
		// Check set charge limit
		assertEquals("Check next value of setChargePowerLimit", 15000, //
				((IntegerReadChannel) EVCS0.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT)).getNextValue()
						.orElse(0).intValue());

		// Wait till charge limit is taken
		test.next(new TestCase("Check ChargeState after 'getMinimumTimeTillCharingLimitTaken'") //
				/*
				 * Timeleap is not used, to avoid using a clock in the ChargeSatusHandler and
				 * therefore a ClockProvider function in every EVCS (.timeleap(clock, 31,
				 * ChronoUnit.SECONDS))
				 */
				.onAfterProcessImage(sleep) //
				.input(evcs0ChargePower, 15000) //
				.output(evcs0Status, Status.CHARGING) //
				.output(evcs0ChargeState, ChargeState.CHARGING)); //

		// Decrease power
		test.next(new TestCase("Decrease Power") //
				.input(evcs0ChargePower, 15000) //
				.input(evcs0SetChargePowerLimit, 8000) //
				.output(evcs0ChargePower, 8000) //
				.output(evcs0Status, Status.CHARGING) //
				.output(evcs0ChargeState, ChargeState.DECREASING)); //

		// Enough power to increase, but 'MinimumTimeTillCharingLimitTaken' is not
		// expired
		test.next(new TestCase("Stay in decreasing charge state") //
				.input(evcs0ChargePower, 8000) //
				.input(evcs0SetChargePowerLimit, 20000) //
				.output(evcs0ChargePower, 8000) //
				.output(evcs0Status, Status.CHARGING) //
				.output(evcs0ChargeState, ChargeState.DECREASING)); //

		// MinimumTimeTillCharingLimitTaken passed
		test.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
				.onAfterProcessImage(sleep) //
				.input(evcs0ChargePower, 8000) //
				.input(evcs0SetChargePowerLimit, 20000) //
				.output(evcs0ChargePower, 20000) //
				.output(evcs0Status, Status.CHARGING) //
				.output(evcs0ChargeState, ChargeState.INCREASING)); //

		// Charge power is increasing but decrease has higher priority than pause
		test.next(new TestCase("Decrease has highest priority") //
				.input(evcs0ChargePower, 20000) //
				.input(evcs0SetChargePowerLimit, 0) //
				.output(evcs0ChargePower, 0) //
				.output(evcs0Status, Status.CHARGING_REJECTED) //
				.output(evcs0ChargeState, ChargeState.DECREASING)); //
	}

	@Test
	public void abstractManagedEvcsStateChangesTest() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(1010);

		ComponentTest test = new ComponentTest(EVCS1).addComponent(EVCS1);

		// Initial charge
		test.next(new TestCase("Initial charge") //

				.input(evcs1SetChargePowerLimit, 15000) //
				.input(evcs1ChargePower, 0) //
				.input(evcs1Status, Status.READY_FOR_CHARGING) //
				.input(evcs1EnergySession, 9999) //
				.input(evcs1SetEnergyLimit, 10000) //
				.output(evcs1ChargePower, 15000) //
				.output(evcs1ChargeState, ChargeState.INCREASING)); //

		// EnergyLimit Reached
		test.next(new TestCase("Energy limit reached") //
				.input(evcs1ChargePower, 15000) //
				.input(evcs1EnergySession, 10000) //
				.input(evcs1Status, Status.CHARGING) //
				.output(evcs1ChargeState, ChargeState.DECREASING) //
				.output(evcs1Status, Status.ENERGY_LIMIT_REACHED) //
				.output(evcs1ChargePower, 0)); //

		// EnergyLimit increased - still in pause
		test.next(new TestCase("Energy limit increased - still in pause") //
				.input(evcs1ChargePower, 0) //
				.input(evcs1EnergySession, 10000) //
				.input(evcs1SetEnergyLimit, 20000) //
				.input(evcs1SetChargePowerLimit, 15000) //
				.output(evcs1ChargePower, 0) //
				.output(evcs1ChargeState, ChargeState.DECREASING)); //

		// EnergyLimit increased - after pause
		test.next(new TestCase("Energy limit increased - after pause") //
				.onAfterProcessImage(sleep) //
				.input(evcs1ChargePower, 0) //
				.input(evcs1EnergySession, 10000) //
				.input(evcs1SetEnergyLimit, 20000) //
				.input(evcs1SetChargePowerLimit, 15000) //
				.output(evcs1ChargePower, 15000) //
				.output(evcs1ChargeState, ChargeState.INCREASING)); //

		// Decrease power
		test.next(new TestCase("Decrease Power") //
				.input(evcs1ChargePower, 15000) //
				.input(evcs1SetChargePowerLimit, 8000) //
				.output(evcs1ChargePower, 8000) //
				.output(evcs1Status, Status.CHARGING) //
				.output(evcs1ChargeState, ChargeState.DECREASING)); //

		// Enough power to increase, but 'MinimumTimeTillCharingLimitTaken' is not
		// expired
		test.next(new TestCase("Stay in decreasing charge state") //
				.input(evcs1ChargePower, 8000) //
				.input(evcs1SetChargePowerLimit, 20000) //
				.output(evcs1ChargePower, 8000) //
				.output(evcs1Status, Status.CHARGING) //
				.output(evcs1ChargeState, ChargeState.DECREASING)); //

		// MinimumTimeTillCharingLimitTaken passed
		test.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
				.onAfterProcessImage(sleep) //
				.input(evcs1ChargePower, 8000) //
				.input(evcs1SetChargePowerLimit, 20000) //
				.output(evcs1ChargePower, 20000) //
				.output(evcs1Status, Status.CHARGING) //
				.output(evcs1ChargeState, ChargeState.INCREASING)); //

		// Charge power is increasing but decrease has higher priority than pause
		test.next(new TestCase("Decrease has highest priority") //
				.input(evcs1ChargePower, 20000) //
				.input(evcs1SetChargePowerLimit, 0) //
				.output(evcs1ChargePower, 0) //
				.output(evcs1Status, Status.CHARGING_REJECTED) //
				.output(evcs1ChargeState, ChargeState.DECREASING)); //
	}

	@Test
	public void abstractManagedEvcsWithFilterTest() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(1010);

		ComponentTest test = new ComponentTest(evcs2).addComponent(evcs2);

		// Initial charge
		int initialResult = (int) (MINIMUM + MAXIMUM * evcs2.getEvcsPower().getIncreaseRate()); // 5244
		test.next(new TestCase("Initial charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 15000) //
				.input(evcs2ChargePower, 0) //
				.input(evcs2Status, Status.READY_FOR_CHARGING) //
				.output(evcs2ChargePower, initialResult) //
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		/*
		 * Cannot check the nextValue of SetChargePowerLimit as output, because the test
		 * validator checks the write value (.output(evcs0SetChargePowerLimit,
		 * initialResult))
		 */
		// Check set charge limit
		assertEquals("Check next value of setChargePowerLimit", initialResult, //
				((IntegerReadChannel) evcs2.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT)).getNextValue()
						.orElse(0).intValue());

		// Further charge
		int increasingValue = (int) (MAXIMUM * evcs2.getEvcsPower().getIncreaseRate());
		test.next(new TestCase("Further charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 15000) //
				.input(evcs2ChargePower, 5244) //
				.input(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargePower, (int) (initialResult + increasingValue)) // 6348 W
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Further charge
		test.next(new TestCase("Further charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 15000) //
				.input(evcs2ChargePower, 6348) //
				.input(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargePower, initialResult + increasingValue * 2) // 7452 W
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Further charge
		test.next(new TestCase("Further charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 15000) //
				.input(evcs2ChargePower, 6348) //
				.input(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargePower, initialResult + increasingValue * 3) // 8556 W
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Further charge
		test.next(new TestCase("Further charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 15000) //
				.input(evcs2ChargePower, 6348) //
				.input(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargePower, initialResult + increasingValue * 4) // 9660 W
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Further charge - reached target
		test.next(new TestCase("Further charge") //

				.input(evcs2SetChargePowerLimitWithFilter, 10000) //
				.input(evcs2ChargePower, 6348) //
				.input(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargePower, 10_000) // 10000 W
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Wait till charge limit is taken
		test.next(new TestCase("Check ChargeState after 'getMinimumTimeTillCharingLimitTaken'") //
				.onAfterProcessImage(sleep) //
				.input(evcs2ChargePower, 10000) //
				.output(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargeState, ChargeState.CHARGING)); //

		// Decrease power
		test.next(new TestCase("Decrease Power") //
				.input(evcs2ChargePower, 10000) //
				.input(evcs2SetChargePowerLimitWithFilter, 8000) //
				.output(evcs2ChargePower, 8000) //
				.output(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargeState, ChargeState.DECREASING)); //

		// Enough power to increase, but 'MinimumTimeTillCharingLimitTaken' is not
		// expired
		test.next(new TestCase("Stay in decreasing charge state") //
				.input(evcs2ChargePower, 8000) //
				.input(evcs2SetChargePowerLimitWithFilter, 20000) //
				.output(evcs2ChargePower, 8000) //
				.output(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargeState, ChargeState.DECREASING)); //

		// MinimumTimeTillCharingLimitTaken passed
		test.next(new TestCase("MinimumTimeTillCharginglimitTaken passed") //
				.onAfterProcessImage(sleep) //
				.input(evcs2ChargePower, 8000) //
				.input(evcs2SetChargePowerLimitWithFilter, 20000) //
				.output(evcs2ChargePower, 8000 + increasingValue) // 9104
				.output(evcs2Status, Status.CHARGING) //
				.output(evcs2ChargeState, ChargeState.INCREASING)); //

		// Charge power is increasing but decrease has higher priority than pause
		test.next(new TestCase("Decrease has highest priority") //
				.input(evcs2ChargePower, 20000) //
				.input(evcs2SetChargePowerLimitWithFilter, 0) //
				.output(evcs2ChargePower, 0) //
				.output(evcs2Status, Status.CHARGING_REJECTED) //
				.output(evcs2ChargeState, ChargeState.DECREASING)); //

		// Charging stopped - still in pause state
		test.next(new TestCase("Charging stopped - still in pause state") //
				.input(evcs2ChargePower, 0) //
				.input(evcs2SetChargePowerLimitWithFilter, 0) //
				.output(evcs2ChargePower, 0) //
				.output(evcs2Status, Status.CHARGING_REJECTED) //
				.output(evcs2ChargeState, ChargeState.DECREASING)); //

		// Charging stopped
		test.next(new TestCase("Charging stopped") //
				.onAfterProcessImage(sleep) //
				.input(evcs2ChargePower, 0) //
				.input(evcs2SetChargePowerLimitWithFilter, 0) //
				.output(evcs2ChargePower, 0) //
				.output(evcs2Status, Status.CHARGING_REJECTED) //
				.output(evcs2ChargeState, ChargeState.NOT_CHARGING)); //
	}
}
