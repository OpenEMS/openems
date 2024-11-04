package io.openems.edge.energy.api.simulation;

import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.applyBalancing;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.applyChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.applyDelayDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.applyDischargeGrid;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnergyFlowTest {

	/*
	 * BALANCING
	 */

	@Test
	public void testBalancingAndCharge() {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(500, ef.getCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2500, ef.getProd());
		assertEquals(500, ef.getProdToCons());
		assertEquals(2000, ef.getProdToEss());

		assertEquals(-2000, ef.getEss());
		assertEquals(2000, ef.getProdToEss());

		assertEquals(0, ef.getGrid());
		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndChargeFull() {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 2400, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(100, ef.getCons());
		assertEquals(100, ef.getProdToCons());

		assertEquals(3000, ef.getProd());
		assertEquals(100, ef.getProdToCons());
		assertEquals(2400, ef.getProdToEss());
		assertEquals(500, ef.getProdToGrid());

		assertEquals(-2400, ef.getEss());
		assertEquals(2400, ef.getProdToEss());

		assertEquals(-500, ef.getGrid());
		assertEquals(500, ef.getProdToGrid());

		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndDischarge() {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(2500, ef.getCons());
		assertEquals(2000, ef.getEssToCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(500, ef.getProd());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2000, ef.getEss());
		assertEquals(2000, ef.getEssToCons());

		assertEquals(0, ef.getGrid());
		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndDischargeEmpty() {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 4500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 1800, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(4500, ef.getCons());
		assertEquals(2200, ef.getGridToCons());
		assertEquals(1800, ef.getEssToCons());

		assertEquals(500, ef.getProd());
		assertEquals(500, ef.getProdToCons());

		assertEquals(1800, ef.getEss());
		assertEquals(1800, ef.getEssToCons());

		assertEquals(2200, ef.getGrid());
		assertEquals(2200, ef.getGridToCons());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndChargeMoreThanEssMaxEnergy() {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 900, //
				/* essMaxDischarge */ 900, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(500, ef.getCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2500, ef.getProd());
		assertEquals(500, ef.getProdToCons());
		assertEquals(900, ef.getProdToEss());
		assertEquals(1100, ef.getProdToGrid());

		assertEquals(-900, ef.getEss());
		assertEquals(900, ef.getProdToEss());

		assertEquals(-1100, ef.getGrid());
		assertEquals(1100, ef.getProdToGrid());

		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndDischargeAboveEssMaxEnergy() {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 900, //
				/* essMaxDischarge */ 900, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(2500, ef.getCons());
		assertEquals(900, ef.getEssToCons());
		assertEquals(1100, ef.getGridToCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(500, ef.getProd());
		assertEquals(500, ef.getProdToCons());

		assertEquals(900, ef.getEss());
		assertEquals(900, ef.getEssToCons());

		assertEquals(1100, ef.getGrid());
		assertEquals(1100, ef.getGridToCons());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testBalancingAndAboveGridMaxEnergy() {
		var m = new EnergyFlow.Model(//
				/* production */ 1000, //
				/* consumption */ 4900, //
				/* essMaxCharge */ 1600, //
				/* essMaxDischarge */ 2000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(4900, ef.getCons());
		assertEquals(2000, ef.getEssToCons());
		assertEquals(1900, ef.getGridToCons());
		assertEquals(1000, ef.getProdToCons());

		assertEquals(1000, ef.getProd());
		assertEquals(1000, ef.getProdToCons());

		assertEquals(2000, ef.getEss());
		assertEquals(2000, ef.getEssToCons());

		assertEquals(1900, ef.getGrid());
		assertEquals(1900, ef.getGridToCons());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getGridToEss());
	}

	/*
	 * DELAY DISCHARGE
	 */

	@Test
	public void testDelayDischargeAndCharge() {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(500, ef.getCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2500, ef.getProd());
		assertEquals(2000, ef.getProdToEss());
		assertEquals(500, ef.getProdToCons());

		assertEquals(-2000, ef.getEss());
		assertEquals(2000, ef.getProdToEss());

		assertEquals(0, ef.getGrid());
		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testDelayDischargeAndChargeFull() {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 2400, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(100, ef.getCons());
		assertEquals(100, ef.getProdToCons());

		assertEquals(3000, ef.getProd());
		assertEquals(100, ef.getProdToCons());
		assertEquals(500, ef.getProdToGrid());
		assertEquals(2400, ef.getProdToEss());

		assertEquals(-2400, ef.getEss());
		assertEquals(2400, ef.getProdToEss());

		assertEquals(-500, ef.getGrid());
		assertEquals(500, ef.getProdToGrid());

		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	@Test
	public void testDelayDischargeAndWouldDischarge() {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(2500, ef.getCons());
		assertEquals(500, ef.getProdToCons());
		assertEquals(2000, ef.getGridToCons());

		assertEquals(500, ef.getProd());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2000, ef.getGrid());
		assertEquals(2000, ef.getGridToCons());

		assertEquals(0, ef.getEss());
		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToEss());
	}

	/*
	 * CHARGE GRID
	 */

	@Test
	public void testChargeGridAndCharge() {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(500, ef.getCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2500, ef.getProd());
		assertEquals(2000, ef.getProdToEss());
		assertEquals(500, ef.getProdToCons());

		assertEquals(-4500, ef.getEss());
		assertEquals(2500, ef.getGridToEss());
		assertEquals(2000, ef.getProdToEss());

		assertEquals(2500, ef.getGrid());
		assertEquals(2500, ef.getGridToEss());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
	}

	@Test
	public void testChargeGridAndChargeFull() {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 3400, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(100, ef.getCons());
		assertEquals(100, ef.getProdToCons());

		assertEquals(3000, ef.getProd());
		assertEquals(100, ef.getProdToCons());
		assertEquals(2900, ef.getProdToEss());

		assertEquals(-3400, ef.getEss());
		assertEquals(500, ef.getGridToEss());
		assertEquals(2900, ef.getProdToEss());

		assertEquals(500, ef.getGrid());
		assertEquals(500, ef.getGridToEss());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getGridToCons());
		assertEquals(0, ef.getEssToCons());
	}

	@Test
	public void testChargeGridAndAboveGridMaxEnergy() {
		var m = new EnergyFlow.Model(//
				/* production */ 1000, //
				/* consumption */ 2000, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 1600, //
				/* gridMaxSell */ 10000);
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(2000, ef.getCons());
		assertEquals(1000, ef.getProdToCons());
		assertEquals(1000, ef.getGridToCons());

		assertEquals(1000, ef.getProd());
		assertEquals(1000, ef.getGridToCons());

		assertEquals(-600, ef.getEss());
		assertEquals(600, ef.getGridToEss());

		assertEquals(1600, ef.getGrid());
		assertEquals(1000, ef.getGridToCons());
		assertEquals(600, ef.getGridToEss());

		assertEquals(0, ef.getProdToGrid());
		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getEssToCons());
	}

	/*
	 * DISCHARGE GRID - just for completeness
	 */

	@Test
	public void testDischargeGridAndCharge() {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 1600, //
				/* gridMaxSell */ 10000);
		applyDischargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(500, ef.getCons());
		assertEquals(500, ef.getProdToCons());

		assertEquals(2500, ef.getProd());
		assertEquals(500, ef.getProdToCons());
		assertEquals(2000, ef.getProdToGrid());

		assertEquals(2500, ef.getEss());
		assertEquals(-2500, ef.getGridToEss());

		assertEquals(-4500, ef.getGrid());
		assertEquals(2000, ef.getProdToGrid());
		assertEquals(-2500, ef.getGridToEss());

		assertEquals(0, ef.getProdToEss());
		assertEquals(0, ef.getEssToCons());
		assertEquals(0, ef.getGridToCons());
	}

	@Test
	public void testLog() {
		// No actual test. Would have to mock Logger
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		m.logConstraints();
		m.logMinMaxValues();
	}
}
