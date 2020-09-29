package io.openems.edge.controller.ess.delayedselltogrid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDelayedSellToGrid {

	@Test
	public void meter_Power_Less_Than_Delay_Sell_To_GridPower() {
		int meterPower = 490_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 10_000;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_Delay_Sell_To_GridPower() {
		int meterPower = 500_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_Delay_Sell_To_GridPower_Less_Than_Charge_Power() {
		int meterPower = 1_000_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_Charge_Power() {
		int productionPower = 12_500_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(productionPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_Charge_Power() {
		int meterPower = 19_500_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 12_500_000;

		int expected = 7_000_000;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Less_Than_For_Different_Delay_Sell_To_GridPower() {
		int meterPower = 123_456;
		int delayedSellToGridPower = 150_769;
		int chargePower = 12_500_000;

		int expected = 27_313;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Equals_To_For_Different_Delay_Sell_To_GridPower() {
		int meterPower = 456_789;
		int delayedSellToGridPower = 456_789;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than_For_Different_Delay_Sell_To_GridPower_Less_Than_Charge_Power() {
		int meterPower = 1_000_000;
		int delayedSellToGridPower = 456_256;
		int chargePower = 12_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void production_Power_Equals_To_For_Different_Charge_Power() {
		int meterPower = 1_500_000;
		int delayedSellToGridPower = 500_000;
		int chargePower = 1_500_000;

		int expected = 0;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

	@Test
	public void meter_Power_Greater_Than__For_Different_Charge_Power() {
		int meterPower = 5_102_408;
		int delayedSellToGridPower = 500_000;
		int chargePower = 1_743_126;

		int expected = 3_359_282;
		int actual = DelayedSellToGridImpl.calculatePower(meterPower, delayedSellToGridPower, chargePower);

		assertEquals(expected, actual);
	}

}
