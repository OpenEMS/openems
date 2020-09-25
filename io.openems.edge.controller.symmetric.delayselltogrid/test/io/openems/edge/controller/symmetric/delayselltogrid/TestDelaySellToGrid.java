package io.openems.edge.controller.symmetric.delayselltogrid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDelaySellToGrid {

	@Test
	public void production_Power_Less_Than_Delay_Sell_To_GridPower() {
		int productionPower = 490_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 10_000;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	
	@Test
	public void production_Power_Equals_To_Delay_Sell_To_GridPower() {
		int productionPower = 500_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
	@Test
	public void production_Power_Greater_Than_Delay_Sell_To_GridPower_Less_Than_Charge_Power() {
		int productionPower = 1_000_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	
	@Test
	public void production_Power_Equals_To_Charge_Power() {
		int productionPower = 12_500_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
	@Test
	public void production_Power_Greater_Than_Charge_Power() {
		int productionPower = 19_500_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 7_000_000;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
	
	
	
	@Test
	public void production_Power_Less_Than_For_Different_Delay_Sell_To_GridPower() {
		int productionPower = 123_456;
		int delaySellToGridPower = 150_769;
		int chargePower = 12_500_000;

		int expected = 27_313;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	
	@Test
	public void production_Power_Equals_To_For_Different_Delay_Sell_To_GridPower() {
		int productionPower = 456_789;
		int delaySellToGridPower = 456_789;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
	@Test
	public void production_Power_Greater_Than_For_Different_Delay_Sell_To_GridPower_Less_Than_Charge_Power() {
		int productionPower = 1_000_000;
		int delaySellToGridPower = 456_256;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	
	@Test
	public void production_Power_Equals_To_For_Different_Charge_Power() {
		int productionPower = 1_500_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 1_500_000;

		int expected = 0;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
	@Test
	public void production_Power_Greater_Than__For_Different_Charge_Power() {
		int productionPower = 5_102_408;
		int delaySellToGridPower = 500_000;
		int chargePower = 1_743_126;

		int expected = 3_359_282;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}
	
}
