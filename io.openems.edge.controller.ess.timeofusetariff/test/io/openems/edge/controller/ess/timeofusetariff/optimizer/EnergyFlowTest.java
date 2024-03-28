package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static java.lang.Math.max;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.function.TriFunction;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Params.OptimizePeriod;

public class EnergyFlowTest {

	public static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
	public static final EnergyFlow NO_FLOW = new EnergyFlow(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	private static void assertEnergyFlow(EnergyFlow ef) {
		assertTrue("Production is positive", ef.production() >= 0);
		assertTrue("Consumption is positive", ef.consumption() >= 0);
		assertTrue("ProductionToConsumption is positive", ef.productionToConsumption() >= 0);
		assertTrue("ProductionToGrid is positive", ef.productionToGrid() >= 0);
		assertTrue("ProductionToEss is positive", ef.productionToEss() >= 0);
		assertTrue("GridToConsumption is positive", ef.gridToConsumption() >= 0);
		assertTrue("EssToConsumption is positive", ef.essToConsumption() >= 0);

		assertEquals("Sum of Grid", 0, ef.grid() + ef.productionToGrid() - ef.gridToConsumption() - ef.gridToEss());
		assertEquals("Sum of Ess", 0, ef.ess() + ef.productionToEss() - ef.essToConsumption() + ef.gridToEss());
		assertEquals("Sum of Production", 0,
				ef.production() - ef.productionToConsumption() - ef.productionToEss() - ef.productionToGrid());
		assertEquals("Sum of Consumption", 0,
				ef.consumption() - ef.essToConsumption() - ef.gridToConsumption() - ef.productionToConsumption());
		assertEquals("Overall Sum", 0, ef.grid() + ef.ess() + ef.production() - ef.consumption());
	}

	private static Params.Builder P;

	@Before
	public void prepareParams() {
		P = Params.create() //
				.setTime(TIME) //
				.setEssMinSocEnergy(1000) //
				.setEssTotalEnergy(22000) //
				.setEssMaxSocEnergy(20000) //
				.setEssMaxChargeEnergy(5000) //
				.setEssMaxDischargeEnergy(5000) //
				.seMaxBuyFromGrid(4000) //
				.setPrices(0);

		// essChargeInChargeGrid = 2375
	}

	private static EnergyFlow execute(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function, int essInitial,
			Params.Builder pb) {
		var p = pb.build();
		return function.apply(p, p.optimizePeriods().get(0), essInitial);
	}

	private static EnergyFlow charge(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 10000, P //
				.setProductions(2500) //
				.setConsumptions(500));
	}

	private static EnergyFlow chargeFull(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 19_600, P //
				.setProductions(3000) //
				.setConsumptions(100));
	}

	private static EnergyFlow discharge(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 10000, P //
				.setProductions(500) //
				.setConsumptions(2500));
	}

	private static EnergyFlow dischargeEmpty(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 2800, P //
				.setProductions(500) //
				.setConsumptions(4500));
	}

	private static EnergyFlow chargeMoreThanEssMaxEnergy(
			TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 10000, P //
				.setProductions(2500) //
				.setConsumptions(500) //
				.setEssMaxChargeEnergy(900) //
				.setEssMaxDischargeEnergy(900));
	}

	private static EnergyFlow dischargeMoreThanEssMaxEnergy(
			TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		return execute(function, 10000, P //
				.setProductions(500) //
				.setConsumptions(2500) //
				.setEssMaxChargeEnergy(900) //
				.setEssMaxDischargeEnergy(900));
	}

	private static void testBalancingCharge(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		var e = charge(function);
		assertEnergyFlow(e);
		assertEquals(-2000, e.ess());
		assertEquals(0, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToEss());
	}

	private static void testBalancingChargeFull(TriFunction<Params, OptimizePeriod, Integer, EnergyFlow> function) {
		var e = chargeFull(function);
		assertEnergyFlow(e);
		assertEquals(-2400, e.ess()); // expect 2900, but limited by essTotalEnergy
		assertEquals(-500, e.grid()); // expect 0, but ess is limited by 500 -> sell-to-grid
		assertEquals(100, e.productionToConsumption());
		assertEquals(2400, e.productionToEss());
		assertEquals(500, e.productionToGrid());
	}

	/*
	 * BALANCING
	 */

	@Test
	public void testBalancingAndCharge() {
		testBalancingCharge(EnergyFlow::withBalancing);
	}

	@Test
	public void testBalancingAndChargeFull() {
		testBalancingChargeFull(EnergyFlow::withBalancing);
	}

	@Test
	public void testBalancingAndDischarge() {
		var e = discharge(EnergyFlow::withBalancing);
		assertEnergyFlow(e);
		assertEquals(2000, e.ess());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.essToConsumption());
	}

	@Test
	public void testBalancingAndDischargeEmpty() {
		var e = dischargeEmpty(EnergyFlow::withBalancing);
		assertEnergyFlow(e);
		assertEquals(1800, e.ess());
		assertEquals(1800, e.essToConsumption());
		assertEquals(2200, e.grid());
		assertEquals(2200, e.gridToConsumption());
		assertEquals(500, e.productionToConsumption());
	}

	@Test
	public void testBalancingAndChargeMoreThanEssMaxEnergy() {
		var e = chargeMoreThanEssMaxEnergy(EnergyFlow::withBalancing);
		assertEnergyFlow(e);
		assertEquals(-900, e.ess());
		assertEquals(-1100, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(900, e.productionToEss());
		assertEquals(1100, e.productionToGrid());
	}

	@Test
	public void testBalancingAndDischargeAboveEssMaxEnergy() {
		var e = dischargeMoreThanEssMaxEnergy(EnergyFlow::withBalancing);
		assertEnergyFlow(e);
		assertEquals(900, e.ess());
		assertEquals(1100, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(900, e.essToConsumption());
		assertEquals(1100, e.gridToConsumption());
	}

	@Test
	public void testBalancingAndAboveGridMaxEnergy() {
		var e = execute(EnergyFlow::withBalancing, 3000, P //
				.setProductions(1000) //
				.setConsumptions(4900) //
				.seMaxBuyFromGrid(1600));
		assertEnergyFlow(e);
		assertEquals(2000, e.ess());
		assertEquals(1900, e.grid()); // ESS Limit has higher priority
		assertEquals(1000, e.productionToConsumption());
		assertEquals(1900, e.gridToConsumption());
		assertEquals(2000, e.essToConsumption());
	}

	/*
	 * DELAY DISCHARGE
	 */

	@Test
	public void testDelayDischargeAndCharge() {
		testBalancingCharge(EnergyFlow::withDelayDischarge);
	}

	@Test
	public void testDelayDischargeAndChargeFull() {
		testBalancingChargeFull(EnergyFlow::withDelayDischarge);
	}

	@Test
	public void testDelayDischargeAndWouldDischarge() {
		var e = discharge(EnergyFlow::withDelayDischarge);
		assertEnergyFlow(e);
		assertEquals(2000, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.gridToConsumption());
	}

	/*
	 * CHARGE GRID
	 */

	@Test
	public void testChargeGridAndCharge() {
		var e = charge(EnergyFlow::withChargeGrid);
		assertEnergyFlow(e);
		assertEquals(-4375, e.ess());
		assertEquals(2375, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToEss());
	}

	@Test
	public void testChargeGridAndChargeFull() {
		var e = execute(EnergyFlow::withChargeGrid, 16_600, P //
				.setProductions(3000) //
				.setConsumptions(100));
		assertEnergyFlow(e);
		assertEquals(-3400, e.ess()); // expect 5275, but limited by essTotalEnergy
		assertEquals(500, e.grid()); // expect 2375, but production has priority
		assertEquals(100, e.productionToConsumption());
		assertEquals(2900, e.productionToEss());
		assertEquals(500, e.gridToEss());
	}

	@Test
	public void testChargeGridAndAboveGridMaxEnergy() {
		var e = execute(EnergyFlow::withChargeGrid, 10000, P //
				.setProductions(1000) //
				.setConsumptions(2000) //
				.seMaxBuyFromGrid(1600));
		assertEnergyFlow(e);
		assertEquals(-600, e.ess());
		assertEquals(1600, e.grid()); // Limited by maxBuyFromGrid
		assertEquals(1000, e.productionToConsumption());
		assertEquals(1000, e.gridToConsumption());
		assertEquals(600, e.gridToEss());
	}

	/*
	 * DISCHARGE GRID - just for completeness
	 */
	private static EnergyFlow withDischargeGrid(Params p, OptimizePeriod op, int essInitial) {
		// This is just for completeness; not actually used yet
		return EnergyFlow.create(p, op, essInitial, //
				p.essTotalEnergy(), // Does not matter here
				// Same as Balancing + Discharge-To-Grid
				max(0, op.consumption() - op.production()) + 3000 /* static for tests */);
	}

	@Test
	public void testDischargeGridAndCharge() {
		var e = charge(EnergyFlowTest::withDischargeGrid);
		assertEnergyFlow(e);
		assertEquals(3000, e.ess());
		assertEquals(-5000, e.grid());
		assertEquals(-3000, e.gridToEss());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToGrid());
	}
}
