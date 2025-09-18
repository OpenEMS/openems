package io.openems.edge.evcs.hypercharger;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for firmware version compatibility.
 */
public class VersionCompatibilityTest {

	@Test
	public void testFirmwareVersionDetection() {
		// Test v1.8
		FirmwareVersion v18 = new FirmwareVersion(1, 8, 14);
		assertTrue("Should detect v1.8", v18.isVersion18());
		assertFalse("Should not detect v2.3", v18.isVersion23());
		assertFalse("Should not detect v2.4", v18.isVersion24());
		assertFalse("Should not detect v2.5+", v18.isVersion25OrLater());
		assertFalse("v1.8 should not support total charged energy", v18.supportsTotalChargedEnergy());
		assertFalse("v1.8 should not support max charging power AC", v18.supportsMaxChargingPowerAC());
		assertFalse("v1.8 should not use apparent power", v18.usesApparentPowerForStation());
		
		// Test v2.3
		FirmwareVersion v23 = new FirmwareVersion(2, 3, 5);
		assertFalse("Should not detect v1.8", v23.isVersion18());
		assertTrue("Should detect v2.3", v23.isVersion23());
		assertFalse("Should not detect v2.4", v23.isVersion24());
		assertFalse("Should not detect v2.5+", v23.isVersion25OrLater());
		assertTrue("v2.3 should support total charged energy", v23.supportsTotalChargedEnergy());
		assertFalse("v2.3 should not support max charging power AC", v23.supportsMaxChargingPowerAC());
		assertFalse("v2.3 should not use apparent power", v23.usesApparentPowerForStation());
		
		// Test v2.4
		FirmwareVersion v24 = new FirmwareVersion(2, 4, 10);
		assertFalse("Should not detect v1.8", v24.isVersion18());
		assertFalse("Should not detect v2.3", v24.isVersion23());
		assertTrue("Should detect v2.4", v24.isVersion24());
		assertFalse("Should not detect v2.5+", v24.isVersion25OrLater());
		assertTrue("v2.4 should support total charged energy", v24.supportsTotalChargedEnergy());
		assertTrue("v2.4 should support max charging power AC", v24.supportsMaxChargingPowerAC());
		assertFalse("v2.4 should not use apparent power", v24.usesApparentPowerForStation());
		
		// Test v2.5
		FirmwareVersion v25 = new FirmwareVersion(2, 5, 0);
		assertFalse("Should not detect v1.8", v25.isVersion18());
		assertFalse("Should not detect v2.3", v25.isVersion23());
		assertFalse("Should not detect v2.4", v25.isVersion24());
		assertTrue("Should detect v2.5+", v25.isVersion25OrLater());
		assertTrue("v2.5 should support total charged energy", v25.supportsTotalChargedEnergy());
		assertTrue("v2.5 should support max charging power AC", v25.supportsMaxChargingPowerAC());
		assertTrue("v2.5 should use apparent power", v25.usesApparentPowerForStation());
		assertTrue("v2.5 should support extended connector types", v25.supportsExtendedConnectorTypes());
	}
	
	@Test
	public void testVersionComparison() {
		FirmwareVersion v18 = new FirmwareVersion(1, 8, 0);
		FirmwareVersion v25 = new FirmwareVersion(2, 5, 3);
		FirmwareVersion v30 = new FirmwareVersion(3, 0, 0);
		
		// Test isAtLeast
		assertTrue("v1.8 should be at least 1.8", v18.isAtLeast(1, 8));
		assertFalse("v1.8 should not be at least 2.0", v18.isAtLeast(2, 0));
		
		assertTrue("v2.5 should be at least 2.0", v25.isAtLeast(2, 0));
		assertTrue("v2.5 should be at least 2.5", v25.isAtLeast(2, 5));
		assertFalse("v2.5 should not be at least 3.0", v25.isAtLeast(3, 0));
		
		assertTrue("v3.0 should be at least 2.5", v30.isAtLeast(2, 5));
	}
	
	@Test
	public void testConnectorTypeMapping() {
		// Test that connector type enums exist with unique values
		assertNotNull(SelectedConnector.CCS_DC);
		assertEquals(10, SelectedConnector.CCS_DC.getValue());
		
		assertNotNull(SelectedConnector.CHA_DEMO);
		assertEquals(20, SelectedConnector.CHA_DEMO.getValue());
		
		assertNotNull(SelectedConnector.CCS_AC);
		assertEquals(30, SelectedConnector.CCS_AC.getValue());
		
		assertNotNull(SelectedConnector.GBT);
		assertEquals(40, SelectedConnector.GBT.getValue());
		
		assertNotNull(SelectedConnector.CCS2);
		assertEquals(12, SelectedConnector.CCS2.getValue());
		
		assertNotNull(SelectedConnector.CCS1);
		assertEquals(11, SelectedConnector.CCS1.getValue());
		
		assertNotNull(SelectedConnector.MCS);
		assertEquals(60, SelectedConnector.MCS.getValue());
		
		assertNotNull(SelectedConnector.NACS);
		assertEquals(70, SelectedConnector.NACS.getValue());
		
		// Test version-specific mapping
		// v1.8-2.4 mapping
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.fromRawValue(1, false));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(2, false));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(3, false));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(4, false));
		
		// v2.5+ mapping
		assertEquals(SelectedConnector.CCS2, SelectedConnector.fromRawValue(1, true));
		assertEquals(SelectedConnector.CCS1, SelectedConnector.fromRawValue(2, true));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(3, true));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(4, true));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(5, true));
		assertEquals(SelectedConnector.MCS, SelectedConnector.fromRawValue(6, true));
		assertEquals(SelectedConnector.NACS, SelectedConnector.fromRawValue(7, true));
	}
}