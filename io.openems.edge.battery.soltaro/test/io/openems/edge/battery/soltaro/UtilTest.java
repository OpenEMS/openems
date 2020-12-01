package io.openems.edge.battery.soltaro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class UtilTest {

	private DummyBattery battery;
	private DummyCellCharacteristic cellCharacteristic; 
	
	@Before
	public void setUp() throws Exception {
		battery = new DummyBattery();
		cellCharacteristic = new DummyCellCharacteristic();
	}

	@Test
	public void testSetMaxAllowedCurrents() { 
		// Nothing is necessary
		int maxDischargeCurrentFromBMS = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		int maxChargeCurrentFromBMS = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		Util.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
		
		int expectedMaxChargeCurrent = maxChargeCurrentFromBMS;
		int actualMaxChargeCurrent = battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		int expectedMaxDischargeCurrent = maxDischargeCurrentFromBMS;
		int actualMaxDischargeCurrent = battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		boolean expectedChargeForce = false;
		boolean actualChargeForce = battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		boolean expectedDischargeForce = false;
		boolean actualdischargeForce = battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
		
		// Battery has to be charged
		maxDischargeCurrentFromBMS = 0;
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV);
		
		Util.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedMaxChargeCurrent = maxChargeCurrentFromBMS;
		actualMaxChargeCurrent = battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		expectedMaxDischargeCurrent = - (int) Math.max(1, battery.getCapacity().get() * 0.02 / battery.getVoltage().get());
		actualMaxDischargeCurrent = battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		expectedChargeForce = true;
		actualChargeForce = battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		expectedDischargeForce = false;
		actualdischargeForce = battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
	}

	@Test
	public void testSetChannelsForCharge() {
		int expectedCurrent = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		int actualCurrent = battery.getChargeMaxCurrent().get();
		assertEquals(expectedCurrent, actualCurrent);
		
		// Battery can be charged, no discharge necessary
		int maxChargeCurrent = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT + 1;
		Util.setChannelsForCharge(maxChargeCurrent, battery);		
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
		
		expectedCurrent = maxChargeCurrent;
		actualCurrent = battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
		
		boolean expectedForce = false;
		boolean actualForce = battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, no discharge necessary
		maxChargeCurrent = 0;
		Util.setChannelsForCharge(maxChargeCurrent, battery);
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxChargeCurrent;
		actualCurrent = battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = false;
		actualForce = battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, must be discharged
		maxChargeCurrent = -8;
		Util.setChannelsForCharge(maxChargeCurrent, battery);
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxChargeCurrent;
		actualCurrent = battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = true;
		actualForce = battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
	}

	@Test
	public void testSetChannelsForDischarge() {
		int expectedCurrent = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		int actualCurrent = battery.getDischargeMaxCurrent().get();
		assertEquals(expectedCurrent, actualCurrent);
		
		// Battery can be discharged, no charge necessary
		int maxDischargeCurrent = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT + 1;
		Util.setChannelsForDischarge(maxDischargeCurrent, battery);		
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
		
		boolean expectedForce = false;
		boolean actualForce = battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be discharged, no charge necessary
		maxDischargeCurrent = 0;
		Util.setChannelsForDischarge(maxDischargeCurrent, battery);
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = false;
		actualForce = battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, must be charged
		maxDischargeCurrent = -8;
		Util.setChannelsForDischarge(maxDischargeCurrent, battery);
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = true;
		actualForce = battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);	
	}

	
	@Test
	public void testIsVoltageLowerThanForceDischargeVoltage() {
		
		assertTrue(Util.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV - 1));		
		assertTrue(Util.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV));		
		assertFalse(Util.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1));		
		assertFalse(Util.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
	}

	@Test
	public void testIsVoltageAboveFinalChargingVoltage() {
		assertFalse(Util.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV - 1));		
		assertFalse(Util.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV));		
		assertFalse(Util.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1));		
		assertTrue(Util.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsVoltageHigherThanForceChargeVoltage() {
		assertTrue(Util.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1));		
		assertFalse(Util.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV));		
		assertFalse(Util.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV + 1));		
		assertTrue(Util.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsVoltageBelowFinalDischargingVoltage() {
		assertFalse(Util.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);		
		assertTrue(Util.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV));		
		assertFalse(Util.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV + 1));		
		assertFalse(Util.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsFurtherDischargingNecessary() {
		assertFalse(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertFalse(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1);
		assertFalse(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setForceDischargeActive(false);
		assertFalse(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setForceDischargeActive(true);
		assertTrue(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertTrue(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV);
		assertFalse(Util.isFurtherDischargingNecessary(cellCharacteristic, battery));
	}

	@Test
	public void testIsDischargingAlready() {
		assertFalse(Util.isDischargingAlready(battery));
		
		battery.setForceDischargeActive(true);
		assertTrue(Util.isDischargingAlready(battery));
		
		battery.setForceDischargeActive(false);
		assertFalse(Util.isDischargingAlready(battery));
	}

	@Test
	public void testCalculateForceCurrent() {		
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, Util.calculateForceCurrent(battery));
		
		int newCapacity = 200_000;
		battery.setCapacity(newCapacity);
		expected = - (int) Math.max(1, newCapacity * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 5.333 => 5
		assertEquals(expected, Util.calculateForceCurrent(battery));
		
		int newVoltage = 850;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 4.706 => 4
		assertEquals(expected, Util.calculateForceCurrent(battery));
		
		newCapacity =  30_000;
		newVoltage = 700;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 0.857 => 1
		assertEquals(expected, Util.calculateForceCurrent(battery));
		
		newCapacity =  10_000;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 0.286 => 1
		assertEquals(expected, Util.calculateForceCurrent(battery));
	}
	
	
	@Test
	public void testCalculateForceDischargeCurrent() {
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, Util.calculateForceDischargeCurrent(battery));		
	}
	
	@Test
	public void testCalculateForceChargeCurrent() {		
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, Util.calculateForceChargeCurrent(battery));
	}

	@Test
	public void testIsFurtherChargingNecessary() {
		assertFalse(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);
		assertFalse(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1);
		assertFalse(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setForceChargeActive(false);
		assertFalse(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setForceChargeActive(true);
		assertTrue(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);
		assertTrue(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV);
		assertFalse(Util.isFurtherChargingNecessary(cellCharacteristic, battery));
	}

	@Test
	public void testIsChargingAlready() {
		assertFalse(Util.isChargingAlready(battery));
		
		battery.setForceChargeActive(true);
		assertTrue(Util.isChargingAlready(battery));
		
		battery.setForceChargeActive(false);
		assertFalse(Util.isChargingAlready(battery));
	}

	@Test
	public void testAreApiValuesPresent() {
		assertTrue(Util.areApiValuesPresent(battery));
		
		 battery.setCapacityToUndefined();
		 assertFalse(Util.areApiValuesPresent(battery));
		 
		 battery.setCapacity(DummyBattery.DEFAULT_CAPACITY);
		 battery.setVoltageToUndefined();
		 assertFalse(Util.areApiValuesPresent(battery));
		 
		 battery.setVoltage(DummyBattery.DEFAULT_VOLTAGE);
		 battery.setMinimalCellVoltageToUndefined();
		 assertFalse(Util.areApiValuesPresent(battery));
		 
		 battery.setMinimalCellVoltage(DummyBattery.DEFAULT_MIN_CELL_VOLTAGE);
		 battery.setMaximalCellVoltageToUndefined();
		 assertFalse(Util.areApiValuesPresent(battery));
		 
		 battery.setMaximalCellVoltage(DummyBattery.DEFAULT_MAX_CELL_VOLTAGE);
		 assertTrue(Util.areApiValuesPresent(battery));
	}

}
