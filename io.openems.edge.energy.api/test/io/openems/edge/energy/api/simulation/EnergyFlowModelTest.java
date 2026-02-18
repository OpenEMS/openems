package io.openems.edge.energy.api.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class EnergyFlowModelTest {

	@Test
	public void testConstructor_ShouldThrowException_WhenNotSolvable() {
		// Case 1: Too much production
		var exception1 = assertThrows(OpenemsException.class, () -> {
			new EnergyFlow.Model(//
					1200, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
		});
		assertEquals("Initial setup not solvable", exception1.getMessage());

		// Case 2: Too much consumption
		var exception2 = assertThrows(OpenemsException.class, () -> {
			new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					100, // essMaxDischarge
					100, // gridMaxBuy
					10_000 // gridMaxSell
			);
		});
		assertEquals("Initial setup not solvable", exception2.getMessage());
	}

	@Test
	public void testSetEssMaxCharge() throws Exception {
		// Case 1: state = UNSET, minRequiredCharge = 300
		var m1 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				500 // gridMaxSell
		);
		assertEquals(1_000, m1.setEssMaxCharge(1_000));
		assertEquals(1_000, m1.setEssMaxCharge(3_000));
		assertEquals(300, m1.setEssMaxCharge(50));

		// Case 2: state = UNSET, minRequiredCharge = 0
		var m2 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(0, m2.setEssMaxCharge(0));
		assertEquals(0, m2.setEssMaxCharge(200));

		// Case 3: state = ESS_SET, minRequiredCharge = 50
		var m3 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				300, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m3.setEss(-50);
		assertEquals(300, m3.setEssMaxCharge(600));
		assertEquals(200, m3.setEssMaxCharge(200));
		assertEquals(50, m3.setEssMaxCharge(0));

		// Case 4: state = GRID_SET, minRequiredCharge = 600
		var m4 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				800, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m4.setGrid(-200);
		assertEquals(800, m4.setEssMaxCharge(1_000));
		assertEquals(600, m4.setEssMaxCharge(600));
		assertEquals(600, m4.setEssMaxCharge(0));
	}

	@Test
	public void testSetEssMaxDischarge() throws Exception {
		// Case 1: state = UNSET, minRequiredDischarge = 300
		var m1 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				500, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(1000, m1.setEssMaxDischarge(1000));
		assertEquals(1000, m1.setEssMaxDischarge(3000));
		assertEquals(300, m1.setEssMaxDischarge(50));

		// Case 2: state = UNSET, minRequiredDischarge = 0
		var m2 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(0, m2.setEssMaxDischarge(0));
		assertEquals(0, m2.setEssMaxDischarge(200));

		// Case 3: state = ESS_SET, minRequiredDischarge = 50
		var m3 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				300, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m3.setEss(50);
		assertEquals(300, m3.setEssMaxDischarge(600));
		assertEquals(200, m3.setEssMaxDischarge(200));
		assertEquals(50, m3.setEssMaxDischarge(0));

		// Case 4: state = GRID_SET, minRequiredDischarge = 600
		var m4 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				800, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m4.setGrid(200);
		assertEquals(800, m4.setEssMaxDischarge(1_000));
		assertEquals(600, m4.setEssMaxDischarge(600));
		assertEquals(600, m4.setEssMaxDischarge(0));
	}

	@Test
	public void testSetGridMaxSell() throws Exception {
		// Case 1: state = UNSET, minRequiredSell = 300
		var m1 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				500, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(1000, m1.setGridMaxSell(1000));
		assertEquals(1000, m1.setGridMaxSell(3000));
		assertEquals(300, m1.setGridMaxSell(50));

		// Case 2: state = UNSET, minRequiredSell = 0
		var m2 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(0, m2.setGridMaxSell(0));
		assertEquals(0, m2.setGridMaxSell(200));

		// Case 3: state = GRID_SET, minRequiredSell = 50
		var m3 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				1_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				300 // gridMaxSell
		);
		m3.setGrid(-50);
		assertEquals(300, m3.setGridMaxSell(600));
		assertEquals(200, m3.setGridMaxSell(200));
		assertEquals(50, m3.setGridMaxSell(0));

		// Case 4: state = ESS_SET, minRequiredSell = 600
		var m4 = new EnergyFlow.Model(//
				1_000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				800 // gridMaxSell
		);
		m4.setEss(-200);
		assertEquals(800, m4.setGridMaxSell(1_000));
		assertEquals(600, m4.setGridMaxSell(600));
		assertEquals(600, m4.setGridMaxSell(0));
	}

	@Test
	public void setGridMaxBuy() throws Exception {
		// Case 1: state = UNSET, minRequiredBuy = 300
		var m1 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				500, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(1000, m1.setGridMaxBuy(1000));
		assertEquals(1000, m1.setGridMaxBuy(3000));
		assertEquals(300, m1.setGridMaxBuy(50));

		// Case 2: state = UNSET, minRequiredBuy = 0
		var m2 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(0, m2.setGridMaxBuy(0));
		assertEquals(0, m2.setGridMaxBuy(200));

		// Case 3: state = GRID_SET, minRequiredBuy = 50
		var m3 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				300, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m3.setGrid(50);
		assertEquals(300, m3.setGridMaxBuy(600));
		assertEquals(200, m3.setGridMaxBuy(200));
		assertEquals(50, m3.setGridMaxBuy(0));

		// Case 4: state = ESS_SET, minRequiredBuy = 600
		var m4 = new EnergyFlow.Model(//
				200, // production
				1_000, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				800, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m4.setEss(200);
		assertEquals(800, m4.setGridMaxBuy(1_000));
		assertEquals(600, m4.setGridMaxBuy(600));
		assertEquals(600, m4.setGridMaxBuy(0));
	}

	@Test
	public void testAddManagedConsumption() throws Exception {
		// Case 1: state = UNSET, maxPossibleManagedConsumption = 4000
		var m1 = new EnergyFlow.Model(//
				1000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				2000, // essMaxDischarge
				1200, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(2000, m1.addManagedConsumption("cons1", 2000));
		assertEquals(1800, m1.addManagedConsumption("cons2", 1800));
		assertEquals(200, m1.addManagedConsumption("cons3", 1000));
		assertEquals(4200, m1.getConsumption());
		assertEquals(4000, m1.getManagedConsumption());
		assertEquals(-3200, m1.getSurplus());

		// Case 2: state = ESS_SET, maxPossibleManagedConsumption = 4000
		var m2 = new EnergyFlow.Model(//
				1000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				2200, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m2.setEss(1000);
		assertEquals(2000, m2.addManagedConsumption("cons1", 2000));
		assertEquals(1800, m2.addManagedConsumption("cons2", 1800));
		assertEquals(200, m2.addManagedConsumption("cons3", 1000));
		assertEquals(2200, m2.getGrid());
		assertEquals(4200, m2.getConsumption());
		assertEquals(4000, m2.getManagedConsumption());
		assertEquals(-3200, m2.getSurplus());

		// Case 3: state = GRID_SET, maxPossibleManagedConsumption = 4000
		var m3 = new EnergyFlow.Model(//
				1000, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				2200, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		m3.setGrid(1000);
		assertEquals(2000, m3.addManagedConsumption("cons1", 2000));
		assertEquals(1800, m3.addManagedConsumption("cons2", 1800));
		assertEquals(200, m3.addManagedConsumption("cons3", 1000));
		assertEquals(2200, m3.getEss());
		assertEquals(4200, m3.getConsumption());
		assertEquals(4000, m3.getManagedConsumption());
		assertEquals(-3200, m3.getSurplus());
	}

	@Test
	public void testSetEss() throws Exception {
		// Case 1: Try charge 1000
		var m1 = new EnergyFlow.Model(//
				1200, // production
				200, // unmanagedConsumption
				800, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(-800, m1.setEss(-1000));
		assertEquals(-800, m1.setEss(0));
		assertEquals(-200, m1.getGrid());

		// Case 2: Try charge 1000, but only discharge possible
		var m2 = new EnergyFlow.Model(//
				200, // production
				1200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				400, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(600, m2.setEss(-1000));
		assertEquals(400, m2.getGrid());

		// Case 3: Try discharge 1000
		var m3 = new EnergyFlow.Model(//
				200, // production
				1200, // unmanagedConsumption
				10_000, // essMaxCharge
				800, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(800, m3.setEss(1000));
		assertEquals(800, m3.setEss(0));
		assertEquals(200, m3.getGrid());

		// Case 3: Try discharge 1000, but only charge possible
		var m4 = new EnergyFlow.Model(//
				1200, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				400 // gridMaxSell
		);
		assertEquals(-600, m4.setEss(1000));
		assertEquals(-400, m4.getGrid());
	}

	@Test
	public void testSetGrid() throws Exception {
		// Case 1: Try sell 1000
		var m1 = new EnergyFlow.Model(//
				1200, // production
				200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				800 // gridMaxSell
		);
		assertEquals(-800, m1.setGrid(-1000));
		assertEquals(-800, m1.setGrid(0));
		assertEquals(-200, m1.getEss());

		// Case 2: Try sell 1000, but only buying possible
		var m2 = new EnergyFlow.Model(//
				200, // production
				1200, // unmanagedConsumption
				10_000, // essMaxCharge
				400, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(600, m2.setGrid(-1000));
		assertEquals(400, m2.getEss());

		// Case 3: Try buy 1000
		var m3 = new EnergyFlow.Model(//
				200, // production
				1200, // unmanagedConsumption
				10_000, // essMaxCharge
				10_000, // essMaxDischarge
				800, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(800, m3.setGrid(1000));
		assertEquals(800, m3.setGrid(0));
		assertEquals(200, m3.getEss());

		// Case 4: Try buy 1000, but only sell possible
		var m4 = new EnergyFlow.Model(//
				1200, // production
				200, // unmanagedConsumption
				400, // essMaxCharge
				10_000, // essMaxDischarge
				10_000, // gridMaxBuy
				10_000 // gridMaxSell
		);
		assertEquals(-600, m4.setGrid(1000));
		assertEquals(-400, m4.getEss());
	}
}
