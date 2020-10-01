package io.openems.edge.controller.ess.delayedselltogrid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDelayedSellToGrid {

	@Test
	public void meter_Power_Less_Than_Continuous_Sell_To_Grid() {
		int gridPower = -490_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 10_000;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_Continuous_Sell_To_Grid() {
		int gridPower = -500_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_Continuous_Sell_To_Grid_Less_Than_Sell_To_Grid_Limit() {
		int gridPower = -1_000_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_Sell_To_Grid_Limit() {
		int gridPower = -12_500_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_Sell_To_Grid_Limit() {
		int gridPower = -19_500_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 12_500_000;

		int expected = -7_000_000;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Less_Than_For_Different_Continuous_Sell_To_Grid() {
		int gridPower = -123_456;
		int continuousSellToGridPower = 150_769;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 27_313;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_For_Different_Continuous_Sell_To_Grid() {
		int gridPower = -456_789;
		int continuousSellToGridPower = 456_789;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_For_Different_Continuous_Sell_To_Grid_Less_Than_Sell_To_Grid_Limit() {
		int gridPower = -1_000_000;
		int continuousSellToGridPower = 456_256;
		int sellToGridPowerLimit = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void production_Power_Equals_To_For_Different_Sell_To_Grid_Limit() {
		int gridPower = -1_500_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 1_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_For_Different_Sell_To_Grid_Limit() {
		int gridPower = -5_102_408;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 1_743_126;

		int expected = -3_359_282;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}
	
	@Test
	public void meter_Power_Buy_From_Grid_Case() {
		int gridPower = 1_500_000;
		int continuousSellToGridPower = 500_000;
		int sellToGridPowerLimit = 1_250_000;

		int expected = -1_000_000;
		int actual = DelayedSellToGridImpl.calculatePower(gridPower, continuousSellToGridPower, sellToGridPowerLimit);

		assertEquals(expected, actual);
	}

}
