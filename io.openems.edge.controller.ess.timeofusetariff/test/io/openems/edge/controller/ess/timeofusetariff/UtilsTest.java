package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyDelayDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDischargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateMaxChargeProductionPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.essPowerOrElse;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.postprocessSimulatorState;
import static io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration.QUARTER;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyMode;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
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
				/* maxChargePowerFromGrid */ 20000));

		assertEquals(-11000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 5000, //
				/* maxChargePowerFromGrid */ 20000));

		assertEquals(-1840, calculateChargeGridPower(1340, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ 500, //
				/* maxChargePowerFromGrid */ 24000));

		// Would be -3584, but limited to 5000 which is already surpassed
		// TODO if this should actually serve as blackout-protection, a positive value
		// would have to be returned
		assertEquals(0, calculateChargeGridPower(1000, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ 1000, //
				/* gridActivePower */ 9000, //
				/* maxChargePowerFromGrid */ 5000));

		assertEquals(-4340, calculateChargeGridPower(1340, //
				new DummyHybridEss("ess0") //
						.withDcDischargePower(-1500), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ -2000, //
				/* maxChargePowerFromGrid */ 24000));
	}

	@Test
	public void testCalculateDischargeGridPower() {
		assertEquals(8300, calculateDischargeGridPower(//
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ 2500, //
				/* gridActivePower */ 800));
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
	public void testEssPowerOrElse() {
		final var ess = new DummyManagedSymmetricEss("ess0");

		// No params, initial ESS
		assertEquals(0, essPowerOrElse(null, ess));

		// No params, ESS with MaxApparentPower
		withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 1000);
		assertEquals(1000, essPowerOrElse(null, ess));

		// No params, ESS with Capacity
		withValue(ess, SymmetricEss.ChannelId.CAPACITY, 15000);
		assertEquals(7500, essPowerOrElse(null, ess));

		// With given power
		assertEquals(1340, essPowerOrElse(1340, ess));
	}

	private static DifferentModes.Period<StateMachine, OptimizationContext> mockPeriod(PeriodDuration duration,
			StateMachine mode, int essChargePowerInChargeGrid) {
		return new DifferentModes.Period<StateMachine, OptimizationContext>(duration, mode, 0,
				new OptimizationContext(0, essChargePowerInChargeGrid, 0, 10), null, 0);
	}

	@Test
	public void testCalculateAutomaticMode() {
		assertEquals("Null-Check", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum(), //
						new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), null));
		assertEquals("Null-Check", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), null));

		assertEquals("BALANCING", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), null));

		assertEquals("DELAY_DISCHARGE stays DELAY_DISCHARGE", new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000), null));

		assertEquals("DELAY_DISCHARGE to BALANCING", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000), null));

		assertEquals("CHARGE_GRID stays CHARGE_GRID", new ApplyMode(CHARGE_GRID, -1400), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1500), null));

		assertEquals("CHARGE_GRID to DELAY_DISCHARGE", new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 400, //
						mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1000), null));

		assertEquals("CHARGE_GRID to BALANCING", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 0, //
						mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1000), null));
	}

	@Test
	public void testCalculateChargePowerInChargeGrid() {
		assertEquals(5745, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of())));

		assertEquals(2161, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalOptimizationContext.Period.Quarter(0, TIME, 0, 1000, 0), //
								new GlobalOptimizationContext.Period.Quarter(1, TIME, 100, 1100, 0), //
								new GlobalOptimizationContext.Period.Quarter(2, TIME, 200, 0, 0) //
						))));

		assertEquals(2232, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalOptimizationContext.Period.Quarter(0, TIME, 0, 700, 123), //
								new GlobalOptimizationContext.Period.Quarter(1, TIME, 100, 600, 123), //
								new GlobalOptimizationContext.Period.Quarter(2, TIME, 200, 500, 125), //
								new GlobalOptimizationContext.Period.Quarter(3, TIME, 300, 400, 126), //
								new GlobalOptimizationContext.Period.Quarter(4, TIME, 400, 300, 123), //
								new GlobalOptimizationContext.Period.Quarter(5, TIME, 500, 200, 122), //
								new GlobalOptimizationContext.Period.Quarter(6, TIME, 600, 100, 121), //
								new GlobalOptimizationContext.Period.Quarter(7, TIME, 700, 0, 121) //
						))));

		assertEquals(2059, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								new GlobalOptimizationContext.Period.Quarter(0, TIME, 0, 700, 120), //
								new GlobalOptimizationContext.Period.Quarter(1, TIME, 100, 600, 121), //
								new GlobalOptimizationContext.Period.Quarter(2, TIME, 200, 500, 122), //
								new GlobalOptimizationContext.Period.Quarter(3, TIME, 300, 1140, 126), //
								new GlobalOptimizationContext.Period.Quarter(4, TIME, 400, 1150, 125), //
								new GlobalOptimizationContext.Period.Quarter(5, TIME, 500, 200, 122), //
								new GlobalOptimizationContext.Period.Quarter(6, TIME, 600, 100, 121), //
								new GlobalOptimizationContext.Period.Quarter(7, TIME, 700, 0, 121) //
						))));
	}

	@Test
	public void testPostprocessSimulatorState() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 200, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();
		var goc = new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, null, null, null,
				new GlobalOptimizationContext.Ess(0, 0, 0, 0), null);
		var gsc = GlobalScheduleContext.from(goc);
		assertEquals(BALANCING, postprocessSimulatorState("ctrl0", null, gsc, ef, null, DELAY_DISCHARGE));
	}
}
