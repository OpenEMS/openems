package io.openems.edge.energy.api.simulation;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyBalancing;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyDelayDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyDischargeGrid;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnergyFlowTest {

	/*
	 * BALANCING
	 */

	@Test
	public void testBalancingAndCharge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 200, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		m.addManagedConsumption("ctrl0", 300);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(-2000, ef.getEss());
		assertEquals(0, ef.getGrid());
	}

	@Test
	public void testBalancingAndAddConsumption() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 200, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		m.addManagedConsumption("ctrl0", 300);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(-2000, ef.getEss());
		assertEquals(0, ef.getGrid());
	}

	@Test
	public void testBalancingAndChargeFull() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 2400, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(100, ef.getConsumption());
		assertEquals(3000, ef.getProduction());
		assertEquals(-2400, ef.getEss());
		assertEquals(-500, ef.getGrid());
	}

	@Test
	public void testBalancingAndDischarge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(2500, ef.getConsumption());
		assertEquals(500, ef.getProduction());
		assertEquals(2000, ef.getEss());
		assertEquals(0, ef.getGrid());
	}

	@Test
	public void testBalancingAndDischargeEmpty() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 4500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 1800, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(4500, ef.getConsumption());
		assertEquals(500, ef.getProduction());
		assertEquals(1800, ef.getEss());
		assertEquals(2200, ef.getGrid());
	}

	@Test
	public void testBalancingAndChargeMoreThanEssMaxEnergy() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 900, //
				/* essMaxDischarge */ 900, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(-900, ef.getEss());
		assertEquals(-1100, ef.getGrid());
	}

	@Test
	public void testBalancingAndDischargeAboveEssMaxEnergy() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 900, //
				/* essMaxDischarge */ 900, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(2500, ef.getConsumption());
		assertEquals(500, ef.getProduction());
		assertEquals(900, ef.getEss());
		assertEquals(1100, ef.getGrid());
	}

	@Test
	public void testBalancingAndAboveGridMaxEnergy() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 1000, //
				/* consumption */ 4900, //
				/* essMaxCharge */ 1600, //
				/* essMaxDischarge */ 2000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(m);
		var ef = m.solve();

		assertEquals(4900, ef.getConsumption());
		assertEquals(1000, ef.getProduction());
		assertEquals(2000, ef.getEss());
		assertEquals(1900, ef.getGrid());
	}

	/*
	 * DELAY DISCHARGE
	 */

	@Test
	public void testDelayDischargeAndCharge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(-2000, ef.getEss());
		assertEquals(0, ef.getGrid());
	}

	@Test
	public void testDelayDischargeAndChargeFull() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 2400, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(100, ef.getConsumption());
		assertEquals(3000, ef.getProduction());
		assertEquals(-2400, ef.getEss());
		assertEquals(-500, ef.getGrid());
	}

	@Test
	public void testDelayDischargeAndWouldDischarge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 500, //
				/* consumption */ 2500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyDelayDischarge(m);
		var ef = m.solve();

		assertEquals(2500, ef.getConsumption());
		assertEquals(500, ef.getProduction());
		assertEquals(2000, ef.getGrid());
		assertEquals(0, ef.getEss());
	}

	/*
	 * CHARGE GRID
	 */

	@Test
	public void testChargeGridAndCharge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(-4500, ef.getEss());
		assertEquals(2500, ef.getGrid());
	}

	@Test
	public void testChargeGridAndChargeFull() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 3000, //
				/* consumption */ 100, //
				/* essMaxCharge */ 3400, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		// var consumption = m.finalizeConsumption();
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(100, ef.getConsumption());
		assertEquals(3000, ef.getProduction());
		assertEquals(-3400, ef.getEss());
		assertEquals(500, ef.getGrid());
	}

	@Test
	public void testChargeGridAndAboveGridMaxEnergy() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 1000, //
				/* consumption */ 2000, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 1600, //
				/* gridMaxSell */ 10000);
		applyChargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(2000, ef.getConsumption());
		assertEquals(1000, ef.getProduction());
		assertEquals(-600, ef.getEss());
		assertEquals(1600, ef.getGrid());
	}

	/*
	 * DISCHARGE GRID
	 */

	@Test
	public void testDischargeGridAndCharge() throws Exception {
		var m = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 5000, //
				/* gridMaxBuy */ 1600, //
				/* gridMaxSell */ 10000);
		applyDischargeGrid(m, 2500);
		var ef = m.solve();

		assertEquals(500, ef.getConsumption());
		assertEquals(2500, ef.getProduction());
		assertEquals(2500, ef.getEss());
		assertEquals(-4500, ef.getGrid());
	}
}
