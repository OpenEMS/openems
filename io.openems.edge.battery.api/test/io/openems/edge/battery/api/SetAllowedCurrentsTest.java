package io.openems.edge.battery.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.battery.api.SetAllowedCurrents;
import io.openems.edge.battery.api.Settings;

public class SetAllowedCurrentsTest {

	private DummyBattery battery;
	private DummyCellCharacteristic cellCharacteristic; 
	private Settings settings;
	
	private static int MAX_INCREASE_MILLI_AMPERE = 300;
	private static double MIN_CURRENT_AMPERE = 1;
	private static int TOLERANCE_MILLI_VOLT = 10;
	private static double POWER_FACTOR = 0.02;
	
	@Before
	public void setUp() throws Exception {
		this.battery = new DummyBattery();
		this.cellCharacteristic = new DummyCellCharacteristic();
		this.settings = new SettingsImpl(TOLERANCE_MILLI_VOLT, MIN_CURRENT_AMPERE, POWER_FACTOR, MAX_INCREASE_MILLI_AMPERE);
	}

	@Test
	public void testBatteryIsChargedUntilFinalDischargeIsReached() { 
		// Battery has to be charged
		int maxDischargeCurrentFromBms = 0;
		int maxChargeCurrentFromBms = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV);

		SetAllowedCurrents.setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBms, maxDischargeCurrentFromBms);
				
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		int expectedMaxChargeCurrent = maxChargeCurrentFromBms;
		int actualMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		int expectedMaxDischargeCurrent = - (int) Math.max(MIN_CURRENT_AMPERE, this.battery.getCapacity().get() * POWER_FACTOR / this.battery.getVoltage().get());
		int actualMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		
		boolean expectedChargeForce = true;
		boolean actualChargeForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		
		boolean expectedDischargeForce = false;
		boolean actualdischargeForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
		
		// Min Voltage has risen above force level, but is still under final discharge level minus tolerance 
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - this.settings.getToleranceMilliVolt() - 1);
		SetAllowedCurrents.setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBms, maxDischargeCurrentFromBms);
				
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedMaxChargeCurrent = maxChargeCurrentFromBms;
		actualMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		expectedMaxDischargeCurrent = - (int) Math.max(MIN_CURRENT_AMPERE, this.battery.getCapacity().get() * POWER_FACTOR / this.battery.getVoltage().get());
		actualMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		
		expectedChargeForce = true;
		actualChargeForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		
		expectedDischargeForce = false;
		actualdischargeForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
		
		// Min Voltage has risen above final discharge level  
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV + 1);
		SetAllowedCurrents.setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBms, maxDischargeCurrentFromBms);
				
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedMaxChargeCurrent = maxChargeCurrentFromBms;
		actualMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		expectedMaxDischargeCurrent = maxDischargeCurrentFromBms;
		actualMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		
		expectedChargeForce = false;
		actualChargeForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		
		expectedDischargeForce = false;
		actualdischargeForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
	}
	
	@Test
	public void testSetMaxAllowedCurrents() { 
		// Nothing is necessary
		int maxDischargeCurrentFromBms = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		int maxChargeCurrentFromBms = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		SetAllowedCurrents.setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBms, maxDischargeCurrentFromBms);
				
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		int expectedMaxChargeCurrent = maxChargeCurrentFromBms;
		int actualMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		int expectedMaxDischargeCurrent = maxDischargeCurrentFromBms;
		int actualMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		boolean expectedChargeForce = false;
		boolean actualChargeForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		boolean expectedDischargeForce = false;
		boolean actualdischargeForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
		
		// Battery has to be charged
		maxDischargeCurrentFromBms = 0;
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV);
		
		SetAllowedCurrents.setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBms, maxDischargeCurrentFromBms);
				
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedMaxChargeCurrent = maxChargeCurrentFromBms;
		actualMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedMaxChargeCurrent, actualMaxChargeCurrent);
		
		expectedMaxDischargeCurrent = - (int) Math.max(MIN_CURRENT_AMPERE, this.battery.getCapacity().get() * POWER_FACTOR / this.battery.getVoltage().get());
		actualMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedMaxDischargeCurrent, actualMaxDischargeCurrent);
		expectedChargeForce = true;
		actualChargeForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedChargeForce, actualChargeForce);
		expectedDischargeForce = false;
		actualdischargeForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedDischargeForce, actualdischargeForce);
	}

	@Test
	public void testSetChannelsForCharge() {
		int expectedCurrent = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		int actualCurrent = this.battery.getChargeMaxCurrent().get();
		assertEquals(expectedCurrent, actualCurrent);
		
		// Battery can be charged, no discharge necessary
		int maxChargeCurrent = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT + 1;
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, this.battery);		
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
		
		expectedCurrent = maxChargeCurrent;
		actualCurrent = this.battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
		
		boolean expectedForce = false;
		boolean actualForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, no discharge necessary
		maxChargeCurrent = 0;
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, this.battery);
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxChargeCurrent;
		actualCurrent = this.battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = false;
		actualForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, must be discharged
		maxChargeCurrent = -8;
		SetAllowedCurrents.setChannelsForCharge(maxChargeCurrent, this.battery);
		this.battery.getChargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceDischargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxChargeCurrent;
		actualCurrent = this.battery.getChargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = true;
		actualForce = this.battery.getForceDischargeActive().get();
		assertEquals(expectedForce, actualForce);
	}

	@Test
	public void testSetChannelsForDischarge() {
		int expectedCurrent = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		int actualCurrent = this.battery.getDischargeMaxCurrent().get();
		assertEquals(expectedCurrent, actualCurrent);
		
		// Battery can be discharged, no charge necessary
		int maxDischargeCurrent = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT + 1;
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, this.battery);		
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
		
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = this.battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
		
		boolean expectedForce = false;
		boolean actualForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be discharged, no charge necessary
		maxDischargeCurrent = 0;
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, this.battery);
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = this.battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = false;
		actualForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);
		
		// Battery cannot be charged, must be charged
		maxDischargeCurrent = -8;
		SetAllowedCurrents.setChannelsForDischarge(maxDischargeCurrent, this.battery);
		this.battery.getDischargeMaxCurrentChannel().nextProcessImage();
		this.battery.getForceChargeActiveChannel().nextProcessImage();
				
		expectedCurrent = maxDischargeCurrent;
		actualCurrent = this.battery.getDischargeMaxCurrent().get();		
		assertEquals(expectedCurrent, actualCurrent);
				
		expectedForce = true;
		actualForce = this.battery.getForceChargeActive().get();
		assertEquals(expectedForce, actualForce);	
	}

	
	@Test
	public void testIsVoltageLowerThanForceDischargeVoltage() {
		
		assertTrue(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV - 1));		
		assertTrue(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1));		
		assertFalse(SetAllowedCurrents.isVoltageLowerThanForceDischargeVoltage(this.cellCharacteristic, this.battery));
	}

	@Test
	public void testIsVoltageAboveFinalChargingVoltage() {
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV - 1));		
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage((DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1));		
		assertTrue(SetAllowedCurrents.isVoltageAboveFinalChargingVoltage(this.cellCharacteristic, this.battery));	
	}

	@Test
	public void testIsVoltageHigherThanForceChargeVoltage() {
		assertTrue(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1));		
		assertFalse(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage((DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV + 1));		
		assertTrue(SetAllowedCurrents.isVoltageHigherThanForceChargeVoltage(this.cellCharacteristic, this.battery));	
	}

	@Test
	public void testIsVoltageBelowFinalDischargingVoltage() {
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);		
		assertTrue(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV));		
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(this.cellCharacteristic, this.battery));
		
		this.battery.setMinimalCellVoltage((DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV + 1));		
		assertFalse(SetAllowedCurrents.isVoltageBelowFinalDischargingVoltage(this.cellCharacteristic, this.battery));	
	}

	@Test
	public void testIsFurtherDischargingNecessary() {
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage(DummyCellCharacteristic.FORCE_DISCHARGE_CELL_VOLTAGE_MV + 1);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setForceDischargeActive(false);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setForceDischargeActive(true);
		assertTrue(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV + 1);
		assertTrue(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
		
		this.battery.setMaximalCellVoltage(DummyCellCharacteristic.FINAL_CELL_CHARGE_VOLTAGE_MV);
		assertFalse(SetAllowedCurrents.isFurtherDischargingNecessary(this.cellCharacteristic, this.battery));
	}

	@Test
	public void testIsDischargingAlready() {
		assertFalse(SetAllowedCurrents.isDischargingAlready(this.battery));
		
		this.battery.setForceDischargeActive(true);
		assertTrue(SetAllowedCurrents.isDischargingAlready(this.battery));
		
		this.battery.setForceDischargeActive(false);
		assertFalse(SetAllowedCurrents.isDischargingAlready(this.battery));
	}

	@Test
	public void testCalculateForceCurrent() {		
		int expected = - (int) Math.max(MIN_CURRENT_AMPERE, DummyBattery.DEFAULT_CAPACITY * POWER_FACTOR / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(this.battery, this.settings));
		
		int newCapacity = 200_000;
		this.battery.setCapacity(newCapacity);
		expected = - (int) Math.max(MIN_CURRENT_AMPERE, newCapacity * POWER_FACTOR / DummyBattery.DEFAULT_VOLTAGE); // 5.333 => 5
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(this.battery, this.settings));
		
		int newVoltage = 850;
		this.battery.setCapacity(newCapacity);
		this.battery.setVoltage(newVoltage);
		expected = - (int) Math.max(MIN_CURRENT_AMPERE, newCapacity * POWER_FACTOR / newVoltage); // 4.706 => 4
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(this.battery, this.settings));
		
		newCapacity =  30_000;
		newVoltage = 700;
		this.battery.setCapacity(newCapacity);
		this.battery.setVoltage(newVoltage);
		expected = - (int) Math.max(MIN_CURRENT_AMPERE, newCapacity * POWER_FACTOR / newVoltage); // 0.857 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(this.battery, this.settings));
		
		newCapacity =  10_000;
		this.battery.setCapacity(newCapacity);
		this.battery.setVoltage(newVoltage);
		expected = - (int) Math.max(MIN_CURRENT_AMPERE, newCapacity * POWER_FACTOR / newVoltage); // 0.286 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceCurrent(this.battery, this.settings));
	}
	
	
	@Test
	public void testCalculateForceDischargeCurrent() {
		int expected = - (int) Math.max(MIN_CURRENT_AMPERE, DummyBattery.DEFAULT_CAPACITY * POWER_FACTOR / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceDischargeCurrent(this.battery, this.settings));		
	}
	
	@Test
	public void testCalculateForceChargeCurrent() {		
		int expected = - (int) Math.max(MIN_CURRENT_AMPERE, DummyBattery.DEFAULT_CAPACITY * POWER_FACTOR / DummyBattery.DEFAULT_VOLTAGE); // 1.333 => 1
		assertEquals(expected, SetAllowedCurrents.calculateForceDischargeCurrent(this.battery, this.settings));
	}

	@Test
	public void testIsFurtherChargingNecessary() {
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - 1);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FORCE_CHARGE_CELL_VOLTAGE_MV - 1);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setForceChargeActive(false);
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setForceChargeActive(true);
		assertTrue(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - this.settings.getToleranceMilliVolt() - 1);
		assertTrue(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
		
		this.battery.setMinimalCellVoltage(DummyCellCharacteristic.FINAL_CELL_DISCHARGE_VOLTAGE_MV - this.settings.getToleranceMilliVolt());
		assertFalse(SetAllowedCurrents.isFurtherChargingNecessary(this.battery, this.cellCharacteristic, this.settings));
	}

	@Test
	public void testIsChargingAlready() {
		assertFalse(SetAllowedCurrents.isChargingAlready(this.battery));
		
		this.battery.setForceChargeActive(true);
		assertTrue(SetAllowedCurrents.isChargingAlready(this.battery));
		
		this.battery.setForceChargeActive(false);
		assertFalse(SetAllowedCurrents.isChargingAlready(this.battery));
	}

	@Test
	public void testAreApiValuesPresent() {
		assertTrue(SetAllowedCurrents.areApiValuesPresent(this.battery));
		
		 this.battery.setCapacityToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(this.battery));
		 
		 this.battery.setCapacity(DummyBattery.DEFAULT_CAPACITY);
		 this.battery.setVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(this.battery));
		 
		 this.battery.setVoltage(DummyBattery.DEFAULT_VOLTAGE);
		 this.battery.setMinimalCellVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(this.battery));
		 
		 this.battery.setMinimalCellVoltage(DummyBattery.DEFAULT_MIN_CELL_VOLTAGE);
		 this.battery.setMaximalCellVoltageToUndefined();
		 assertFalse(SetAllowedCurrents.areApiValuesPresent(this.battery));
		 
		 this.battery.setMaximalCellVoltage(DummyBattery.DEFAULT_MAX_CELL_VOLTAGE);
		 assertTrue(SetAllowedCurrents.areApiValuesPresent(this.battery));
	}
}