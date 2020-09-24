package io.openems.edge.controller.symmetric.delayselltogrid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDelaySellToGrid {

	@Test
	public void production_Power_Gretaer_Than_Delay_Sell_To_GridPower() {
		int productionPower = 5_000;
		int delaySellToGridPower = 10_000;
		int chargePower = 12_500_000;

		int expected = 5000;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void production_Power_equals_To_Delay_Sell_To_GridPower() {
		int productionPower = 490_000;
		int delaySellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 10_000;
		int actual = DelaySellToGrid.calculatePower(productionPower, delaySellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

}
