package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeEnergyInChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateEssChargeInChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateMaxChargeProductionPower;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.EshContext;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

	public static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
	public static final ZonedDateTime TIME = ZonedDateTime.now(CLOCK);

	@Test
	public void testCalculateChargeGridPower() {
		assertEquals(-10000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 10000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-4200, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 10000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ true));

		assertEquals(-11000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 5000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-5860, calculateChargeGridPower(1340, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ 500, //
				/* maxChargePowerFromGrid */ 24000, //
				/* limitChargePowerFor14aEnWG */ false));

		// Would be -3584, but limited to 5000 which is already surpassed
		// TODO if this should actually serve as blackout-protection, a positive value
		// would have to be returned
		assertEquals(0, calculateChargeGridPower(1000, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ 1000, //
				/* gridActivePower */ 9000, //
				/* maxChargePowerFromGrid */ 5000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-8360, calculateChargeGridPower(1340, //
				new DummyHybridEss("ess0") //
						.withDcDischargePower(-1500), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ -2000, //
				/* maxChargePowerFromGrid */ 24000, //
				/* limitChargePowerFor14aEnWG */ false));
	}

	@Test
	public void testCalculateChargeProduction() {
		assertEquals(-500, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(500)) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum()) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(-100 /* wrong */)) //
				.intValue());
	}

	@Test
	public void testCalculateDelayDischarge() {
		// DC-PV
		assertEquals(500, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-500) //
						.withDcDischargePower(-1000)));

		// Never negative
		assertEquals(0, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-1500) //
						.withDcDischargePower(-1000)));

		// AC-PV
		assertEquals(0, calculateDelayDischargePower(//
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(-1500)));
	}

	@Test
	public void testCalculateMaxChargeGridPower() {
		final var ess = new DummyManagedSymmetricEss("ess0");

		// No params, initial ESS
		assertEquals(0, calculateEssChargeInChargeGridPower(null, ess));

		// No params, ESS with MaxApparentPower
		withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 1000);
		assertEquals(250, calculateEssChargeInChargeGridPower(null, ess));

		// No params, ESS with Capacity
		withValue(ess, SymmetricEss.ChannelId.CAPACITY, 15000);
		assertEquals(7500, calculateEssChargeInChargeGridPower(null, ess));

		// With params (22 kWh; but few Consumption)
		assertEquals(5360, calculateEssChargeInChargeGridPower(1340, ess));
	}

	private static EnergyScheduleHandler.WithDifferentStates.Period<StateMachine, EshContext> mockPeriod(
			StateMachine state, int essChargeInChargeGrid) {
		return new EnergyScheduleHandler.WithDifferentStates.Period<StateMachine, EshContext>(state, 0,
				new EshContext(null, null, 0, false, 0, essChargeInChargeGrid), null, 0);
	}

	@Test
	public void testCalculateAutomaticMode() {
		assertEquals("Null-Check", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum(), //
						new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(BALANCING, /* essChargeInChargeGrid */ 1000)));
		assertEquals("Null-Check", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(BALANCING, /* essChargeInChargeGrid */ 1000)));

		assertEquals("BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(BALANCING, /* essChargeInChargeGrid */ 1000)));

		assertEquals("DELAY_DISCHARGE stays DELAY_DISCHARGE", new ApplyState(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000)));

		assertEquals("DELAY_DISCHARGE to BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000)));

		assertEquals("CHARGE_GRID stays CHARGE_GRID", new ApplyState(CHARGE_GRID, -1400), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(CHARGE_GRID, /* essChargeInChargeGrid */ 1000)));

		assertEquals("CHARGE_GRID to DELAY_DISCHARGE", new ApplyState(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 400, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(CHARGE_GRID, /* essChargeInChargeGrid */ 1000)));

		assertEquals("CHARGE_GRID to BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 0, //
						/* limitChargePowerFor14aEnWG */ true, //
						mockPeriod(CHARGE_GRID, /* essChargeInChargeGrid */ 1000)));
	}

	@Test
	public void testCalculateChargeEnergyInChargeGrid() {
		assertEquals(1375, calculateChargeEnergyInChargeGrid(//
				new GlobalSimulationsContext(CLOCK, new AtomicInteger(), TIME, ImmutableList.of(), //
						new GlobalSimulationsContext.Grid(0, 20000), //
						new GlobalSimulationsContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of())));

		assertEquals(525, calculateChargeEnergyInChargeGrid(//
				new GlobalSimulationsContext(CLOCK, new AtomicInteger(), TIME, ImmutableList.of(), //
						new GlobalSimulationsContext.Grid(0, 20000), //
						new GlobalSimulationsContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalSimulationsContext.Period.Quarter(TIME, 0, 1000, 0), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 100, 1100, 0), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 200, 0, 0) //
						))));

		assertEquals(538, calculateChargeEnergyInChargeGrid(//
				new GlobalSimulationsContext(CLOCK, new AtomicInteger(), TIME, ImmutableList.of(), //
						new GlobalSimulationsContext.Grid(0, 20000), //
						new GlobalSimulationsContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalSimulationsContext.Period.Quarter(TIME, 0, 700, 123), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 100, 600, 123), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 200, 500, 125), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 300, 400, 126), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 400, 300, 123), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 500, 200, 122), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 600, 100, 121), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 700, 0, 121) //
						))));

		assertEquals(499, calculateChargeEnergyInChargeGrid(//
				new GlobalSimulationsContext(CLOCK, new AtomicInteger(), TIME, ImmutableList.of(), //
						new GlobalSimulationsContext.Grid(0, 20000), //
						new GlobalSimulationsContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalSimulationsContext.Period.Quarter(TIME, 0, 700, 120), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 100, 600, 121), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 200, 500, 122), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 300, 1140, 126), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 400, 1150, 125), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 500, 200, 122), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 600, 100, 121), //
								new GlobalSimulationsContext.Period.Quarter(TIME, 700, 0, 121) //
						))));
	}

}
