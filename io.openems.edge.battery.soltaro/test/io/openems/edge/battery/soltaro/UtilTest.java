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
	public void testBatteryIsChargedUntilFinalDischargeIsReached() { 
		// Battery has to be charged
		int maxDischargeCurrentFromBMS = 0;
		int maxChargeCurrentFromBMS = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV);

		SetAllowedCurrents.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
		
		int expectedMaxChargeCurrent = maxChargeCurrentFromBMS;
		int actualMaxChargeCurrent = battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		int expectedMaxDischargeCurrent = - (int) Math.max(1, battery.getCapacity().get() * 0.02 / battery.getVoltage().get());
		int actualMaxDischargeCurrent = battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		
		boolean expectedChargeForce = true;
		boolean actualChargeForce = battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		
		boolean expectedDischargeForce = false;
		boolean actualdischargeForce = battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
		
		// Min Voltage has risen above force level, but is still under final discharge level minus tolerance 
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - SetAllowedCurrents.TOLERANCE_MV - 1);
		SetAllowedCurrents.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
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
		
		// Min Voltage has risen above final discharge level  
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV + 1);
		SetAllowedCurrents.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
		battery.getChargeMaxCurrentChannel().nextProcessImage();
		battery.getForceDischargeActiveChannel().nextProcessImage();
		battery.getDischargeMaxCurrentChannel().nextProcessImage();
		battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedMaxChargeCurrent = maxChargeCurrentFromBMS;
		actualMaxChargeCurrent = battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		expectedMaxDischargeCurrent = maxDischargeCurrentFromBMS;
		actualMaxDischargeCurrent = battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		
		expectedChargeForce = false;
		actualChargeForce = battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		
		expectedDischargeForce = false;
		actualdischargeForce = battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
	}
	
	@Test
	public void testSetMaxAllowedCurrents() { 
		// Nothing is necessary
		int maxDischargeCurrentFromBMS = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		int maxChargeCurrentFromBMS = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		SetAllowedCurrents.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
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
		
		SetAllowedCurrents.setMaxAllowedCurrents(cellCharacteristic, maxChargeCurrentFromBMS, maxDischargeCurrentFromBMS, battery);
				
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
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, battery);		
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
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, battery);
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
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, battery);
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
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, battery);		
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
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, battery);
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
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, battery);
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
		
		assertTrue(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV - 1));		
		assertTrue(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1));		
		assertFalse(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery));
	}

	@Test
	public void testIsVoltageAboveFinalChargingVoltage() {
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV - 1));		
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1));		
		assertTrue(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsVoltageHigherThanForceChargeVoltage() {
		assertTrue(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1));		
		assertFalse(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV + 1));		
		assertTrue(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsVoltageBelowFinalDischargingVoltage() {
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);		
		assertTrue(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV + 1));		
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery));	
	}

	@Test
	public void testIsFurtherDischargingNecessary() {
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setForceDischargeActive(false);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setForceDischargeActive(true);
		assertTrue(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertTrue(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
		
		battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(cellCharacteristic, battery));
	}

	@Test
	public void testIsDischargingAlready() {
		assertFalse(SetAllowedCurrents.isDischargingAlready(battery));
		
		battery.setForceDischargeActive(true);
		assertTrue(SetAllowedCurrents.isDischargingAlready(battery));
		
		battery.setForceDischargeActive(false);
		assertFalse(SetAllowedCurrents.isDischargingAlready(battery));
	}

	@Test
	public void testCalculateForceCurrent() {		
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(battery));
		
		int newCapacity = 200_000;
		battery.setCapacity(newCapacity);
		expected = - (int) Math.max(1, newCapacity * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 5.333 => 5
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(battery));
		
		int newVoltage = 850;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 4.706 => 4
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(battery));
		
		newCapacity =  30_000;
		newVoltage = 700;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 0.857 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(battery));
		
		newCapacity =  10_000;
		battery.setCapacity(newCapacity);
		battery.setVoltage(newVoltage);
		expected = - (int) Math.max(1, newCapacity * 0.02 / newVoltage); // 0.286 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(battery));
	}
	
	
	@Test
	public void testCalculateForceDischargeCurrent() {
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceDischargeCurrent(battery));		
	}
	
	@Test
	public void testCalculateForceChargeCurrent() {		
		int expected = - (int) Math.max(1, DummyBattery.DEFAULT_CAPACITY * 0.02 / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceChargeCurrent(battery));
	}

	@Test
	public void testIsFurtherChargingNecessary() {
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setForceChargeActive(false);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setForceChargeActive(true);
		assertTrue(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - SetAllowedCurrents.TOLERANCE_MV - 1);
		assertTrue(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
		
		battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - SetAllowedCurrents.TOLERANCE_MV);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(cellCharacteristic, battery));
	}

	@Test
	public void testIsChargingAlready() {
		assertFalse(SetAllowedCurrents.isChargingAlready(battery));
		
		battery.setForceChargeActive(true);
		assertTrue(SetAllowedCurrents.isChargingAlready(battery));
		
		battery.setForceChargeActive(false);
		assertFalse(SetAllowedCurrents.isChargingAlready(battery));
	}

	@Test
	public void testAreApiValuesPresent() {
		assertTrue(SetAllowedCurrents.areApiValuesPresent(battery));
		
		 battery.setCapacityToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(battery));
		 
		 battery.setCapacity(DummyBattery.DEFAULT_CAPACITY);
		 battery.setVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(battery));
		 
		 battery.setVoltage(DummyBattery.DEFAULT_VOLTAGE);
		 battery.setMinimalCellVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(battery));
		 
		 battery.setMinimalCellVoltage(DummyBattery.DEFAULT_MIN_CELL_VOLTAGE);
		 battery.setMaximalCellVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(battery));
		 
		 battery.setMaximalCellVoltage(DummyBattery.DEFAULT_MAX_CELL_VOLTAGE);
		 assertTrue(SetAllowedCurrents.areApiValuesPresent(battery));
	}

}
