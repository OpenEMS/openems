package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.PEAK_SHAVING;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
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
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

	public static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
	public static final ZonedDateTime TIME = ZonedDateTime.now(CLOCK);

	@Test
	public void testCalculateChargeGrid() {
		assertEquals(new ApplyMode(CHARGE_GRID, -10000), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(10000), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(-6000), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));

		assertEquals(new ApplyMode(CHARGE_GRID, -11000), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(5000), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(-6000), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));

		assertEquals(new ApplyMode(CHARGE_GRID, -1840), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(500), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(-1000), //
						/* maxChargePowerFromGrid */ 24000, //
						/* period */ mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1340), //
						/* forceMode */ null));

		assertEquals(new ApplyMode(PEAK_SHAVING, 5000), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(9000), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(1000), //
						/* maxChargePowerFromGrid */ 5000, //
						/* period */ mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));

		assertEquals(new ApplyMode(CHARGE_GRID, -4340), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(-2000), //
						/* ess */ new DummyHybridEss("ess0") //
								.withActivePower(-1000) //
								.withDcDischargePower(-1500), //
						/* maxChargePowerFromGrid */ 24000, //
						/* period */ mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1340), //
						/* forceMode */ null));
	}

	@Test
	public void testCalculateDischargeGrid() {
		assertEquals(new ApplyMode(DISCHARGE_GRID, 8300), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(800), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(2500), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, DISCHARGE_GRID, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));
	}

	@Test
	public void testCalculateDelayDischarge() {
		// DC-PV
		assertEquals(new ApplyMode(DELAY_DISCHARGE, 500), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(0), //
						/* ess */ new DummyHybridEss("ess0") //
								.withActivePower(-500) //
								.withDcDischargePower(-1000), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));

		// Never negative
		assertEquals(new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(0), //
						/* ess */ new DummyHybridEss("ess0") //
								.withActivePower(-1500) //
								.withDcDischargePower(-1000), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));

		// AC-PV
		assertEquals(new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(0), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(-1500), //
						/* maxChargePowerFromGrid */ 20000, //
						/* period */ mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));

		// Peak-Shaving to gridSoftLimit
		assertEquals(new ApplyMode(PEAK_SHAVING, 500), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(7000), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(-1500), //
						/* maxChargePowerFromGrid */ 5000, //
						/* period */ mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 10000), //
						/* forceMode */ null));
	}

	private static DifferentModes.Period<StateMachine, OptimizationContext> mockPeriod(PeriodDuration duration,
			StateMachine mode, int essChargePowerInChargeGrid) {
		return new DifferentModes.Period<StateMachine, OptimizationContext>(duration, mode, 0.,
				new OptimizationContext(0, 0, essChargePowerInChargeGrid), null, 0);
	}

	@Test
	public void testCalculateAutomaticMode() {
		assertEquals("Null-Check", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						/* sum */ new DummySum(), //
						/* ess */ new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));
		assertEquals("Null-Check", new ApplyMode(BALANCING, null), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(100), //
						/* ess */ new DummyManagedSymmetricEss("ess0"), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));

		assertEquals("BALANCING", new ApplyMode(BALANCING, 600), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(100), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, BALANCING, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));

		assertEquals("DELAY_DISCHARGE stays DELAY_DISCHARGE", new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(100), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));

		assertEquals("DELAY_DISCHARGE to BALANCING", new ApplyMode(BALANCING, 0), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(-500), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, DELAY_DISCHARGE, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));

		assertEquals("CHARGE_GRID stays CHARGE_GRID", new ApplyMode(CHARGE_GRID, -1400), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(100), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 2000, //
						mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 30500), //
						/* forceMode */ null));

		assertEquals("CHARGE_GRID to DELAY_DISCHARGE", new ApplyMode(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						/* sum */ new DummySum() //
								.withGridActivePower(100), //
						/* ess */ new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* maxChargePowerFromGrid */ 600, //
						mockPeriod(QUARTER, CHARGE_GRID, /* essChargeInChargeGrid */ 1000), //
						/* forceMode */ null));
	}

	@Test
	public void testCalculateChargePowerInChargeGrid() {
		assertEquals(5745, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of()),
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(4336, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								GlobalOptimizationContext.Period.Quarter.from(0, TIME, 0, 1000, 0.), //
								GlobalOptimizationContext.Period.Quarter.from(1, TIME, 100, 1100, 0.), //
								GlobalOptimizationContext.Period.Quarter.from(2, TIME, 200, 0, 0.) //
						)), //
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(3182, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								GlobalOptimizationContext.Period.Quarter.from(0, TIME, 0, 700, 123.), //
								GlobalOptimizationContext.Period.Quarter.from(1, TIME, 100, 600, 123.), //
								GlobalOptimizationContext.Period.Quarter.from(2, TIME, 200, 500, 125.), //
								GlobalOptimizationContext.Period.Quarter.from(3, TIME, 300, 400, 126.), //
								GlobalOptimizationContext.Period.Quarter.from(4, TIME, 400, 300, 123.), //
								GlobalOptimizationContext.Period.Quarter.from(5, TIME, 500, 200, 122.), //
								GlobalOptimizationContext.Period.Quarter.from(6, TIME, 600, 100, 121.), //
								GlobalOptimizationContext.Period.Quarter.from(7, TIME, 700, 0, 121.) //
						)), //
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(3818, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						ImmutableList.of(//
								GlobalOptimizationContext.Period.Quarter.from(0, TIME, 0, 700, 120.), //
								GlobalOptimizationContext.Period.Quarter.from(1, TIME, 100, 600, 121.), //
								GlobalOptimizationContext.Period.Quarter.from(2, TIME, 200, 500, 122.), //
								GlobalOptimizationContext.Period.Quarter.from(3, TIME, 300, 1140, 126.), //
								GlobalOptimizationContext.Period.Quarter.from(4, TIME, 400, 1150, 125.), //
								GlobalOptimizationContext.Period.Quarter.from(5, TIME, 500, 200, 122.), //
								GlobalOptimizationContext.Period.Quarter.from(6, TIME, 600, 100, 121.), //
								GlobalOptimizationContext.Period.Quarter.from(7, TIME, 700, 0, 121.) //
						)), //
				/* maxEnergyInChargeGrid */ 11490));
	}
}
