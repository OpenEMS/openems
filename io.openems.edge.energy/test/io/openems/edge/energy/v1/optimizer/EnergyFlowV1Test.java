package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.postprocessSimulatorState;
import static java.lang.Math.max;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.function.TriFunction;
import io.openems.edge.energy.v1.optimizer.ParamsV1.OptimizePeriod;

@SuppressWarnings("deprecation")
public class EnergyFlowV1Test {

	public static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
	public static final EnergyFlowV1 NO_FLOW = new EnergyFlowV1(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	private static void assertEnergyFlow(EnergyFlowV1 ef) {
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

	private static ParamsV1.Builder P;

	@Before
	public void prepareParams() {
		P = ParamsV1.create() //
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

	private static EnergyFlowV1 execute(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function, int essInitial,
			ParamsV1.Builder pb) {
		var p = pb.build();
		return function.apply(p, p.optimizePeriods().get(0), essInitial);
	}

	private static EnergyFlowV1 charge(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 10000, P //
				.setProductions(2500) //
				.setConsumptions(500));
	}

	private static EnergyFlowV1 chargeFull(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 19_600, P //
				.setProductions(3000) //
				.setConsumptions(100));
	}

	private static EnergyFlowV1 discharge(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 10000, P //
				.setProductions(500) //
				.setConsumptions(2500));
	}

	private static EnergyFlowV1 dischargeEmpty(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 2800, P //
				.setProductions(500) //
				.setConsumptions(4500));
	}

	private static EnergyFlowV1 chargeMoreThanEssMaxEnergy(
			TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 10000, P //
				.setProductions(2500) //
				.setConsumptions(500) //
				.setEssMaxChargeEnergy(900) //
				.setEssMaxDischargeEnergy(900));
	}

	private static EnergyFlowV1 dischargeMoreThanEssMaxEnergy(
			TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		return execute(function, 10000, P //
				.setProductions(500) //
				.setConsumptions(2500) //
				.setEssMaxChargeEnergy(900) //
				.setEssMaxDischargeEnergy(900));
	}

	private static void testBalancingCharge(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
		var e = charge(function);
		assertEnergyFlow(e);
		assertEquals(-2000, e.ess());
		assertEquals(0, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToEss());
	}

	private static void testBalancingChargeFull(TriFunction<ParamsV1, OptimizePeriod, Integer, EnergyFlowV1> function) {
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
		testBalancingCharge(EnergyFlowV1::withBalancing);
	}

	@Test
	public void testBalancingAndChargeFull() {
		testBalancingChargeFull(EnergyFlowV1::withBalancing);
	}

	@Test
	public void testBalancingAndDischarge() {
		var e = discharge(EnergyFlowV1::withBalancing);
		assertEnergyFlow(e);
		assertEquals(2000, e.ess());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.essToConsumption());
	}

	@Test
	public void testBalancingAndDischargeEmpty() {
		var e = dischargeEmpty(EnergyFlowV1::withBalancing);
		assertEnergyFlow(e);
		assertEquals(1800, e.ess());
		assertEquals(1800, e.essToConsumption());
		assertEquals(2200, e.grid());
		assertEquals(2200, e.gridToConsumption());
		assertEquals(500, e.productionToConsumption());
	}

	@Test
	public void testBalancingAndChargeMoreThanEssMaxEnergy() {
		var e = chargeMoreThanEssMaxEnergy(EnergyFlowV1::withBalancing);
		assertEnergyFlow(e);
		assertEquals(-900, e.ess());
		assertEquals(-1100, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(900, e.productionToEss());
		assertEquals(1100, e.productionToGrid());
	}

	@Test
	public void testBalancingAndDischargeAboveEssMaxEnergy() {
		var e = dischargeMoreThanEssMaxEnergy(EnergyFlowV1::withBalancing);
		assertEnergyFlow(e);
		assertEquals(900, e.ess());
		assertEquals(1100, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(900, e.essToConsumption());
		assertEquals(1100, e.gridToConsumption());
	}

	@Test
	public void testBalancingAndAboveGridMaxEnergy() {
		var e = execute(EnergyFlowV1::withBalancing, 3000, P //
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
		testBalancingCharge(EnergyFlowV1::withDelayDischarge);
	}

	@Test
	public void testDelayDischargeAndChargeFull() {
		testBalancingChargeFull(EnergyFlowV1::withDelayDischarge);
	}

	@Test
	public void testDelayDischargeAndWouldDischarge() {
		var e = discharge(EnergyFlowV1::withDelayDischarge);
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
		var e = charge(EnergyFlowV1::withChargeGrid);
		assertEnergyFlow(e);
		assertEquals(-4375, e.ess());
		assertEquals(2375, e.grid());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToEss());
	}

	@Test
	public void testChargeGridAndChargeFull() {
		var e = execute(EnergyFlowV1::withChargeGrid, 16_600, P //
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
		var e = execute(EnergyFlowV1::withChargeGrid, 10000, P //
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

	private static EnergyFlowV1 withDischargeGrid(ParamsV1 p, OptimizePeriod op, int essInitial) {
		// This is just for completeness; not actually used yet
		return EnergyFlowV1.create(p, op, essInitial, //
				p.essTotalEnergy(), // Does not matter here
				// Same as Balancing + Discharge-To-Grid
				max(0, op.consumption() - op.production()) + 3000 /* static for tests */);
	}

	@Test
	public void testDischargeGridAndCharge() {
		var e = charge(EnergyFlowV1Test::withDischargeGrid);
		assertEnergyFlow(e);
		assertEquals(3000, e.ess());
		assertEquals(-5000, e.grid());
		assertEquals(-3000, e.gridToEss());
		assertEquals(500, e.productionToConsumption());
		assertEquals(2000, e.productionToGrid());
	}

	/*
	 * Utils
	 */

	@Test
	public void testUtilsPostprocessPeriodState() {
		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessSimulatorState(BALANCING, //
						NO_FLOW, NO_FLOW, NO_FLOW));

		assertEquals("DELAY_DISCHARGE stays DELAY_DISCHARGE", //
				DELAY_DISCHARGE, postprocessSimulatorState(DELAY_DISCHARGE, //
						NO_FLOW, charge(EnergyFlowV1::withDelayDischarge), NO_FLOW));
		assertEquals("DELAY_DISCHARGE to BALANCING", //
				BALANCING, postprocessSimulatorState(DELAY_DISCHARGE, //
						NO_FLOW, NO_FLOW, NO_FLOW));

		assertEquals("CHARGE_GRID stays CHARGE_GRID", //
				CHARGE_GRID, postprocessSimulatorState(CHARGE_GRID, //
						NO_FLOW, NO_FLOW, charge(EnergyFlowV1::withChargeGrid)));
		assertEquals("CHARGE_GRID to BALANCING", //
				BALANCING, postprocessSimulatorState(CHARGE_GRID, //
						NO_FLOW, NO_FLOW, NO_FLOW));
	}
}
