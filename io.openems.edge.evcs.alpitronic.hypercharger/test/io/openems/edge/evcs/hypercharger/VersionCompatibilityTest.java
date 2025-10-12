package io.openems.edge.evcs.hypercharger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import io.openems.common.types.SemanticVersion;

/**
 * Tests for firmware version compatibility.
 */
public class VersionCompatibilityTest {

	@Test
	public void testFirmwareVersionDetection() {
		// Test v1.8
		SemanticVersion v18 = new SemanticVersion(1, 8, 14);
		assertTrue("Should detect v1.8", AlpitronicVersionUtils.isVersion18(v18));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v18));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v18));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v18));
		assertFalse("v1.8 should not support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v18));
		assertFalse("v1.8 should not support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v18));
		assertFalse("v1.8 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v18));

		// Test v2.3
		SemanticVersion v23 = new SemanticVersion(2, 3, 5);
		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v23));
		assertTrue("Should detect v2.3", AlpitronicVersionUtils.isVersion23(v23));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v23));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v23));
		assertTrue("v2.3 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v23));
		assertFalse("v2.3 should not support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v23));
		assertFalse("v2.3 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v23));

		// Test v2.4
		SemanticVersion v24 = new SemanticVersion(2, 4, 10);
		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v24));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v24));
		assertTrue("Should detect v2.4", AlpitronicVersionUtils.isVersion24(v24));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v24));
		assertTrue("v2.4 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v24));
		assertTrue("v2.4 should support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v24));
		assertFalse("v2.4 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v24));

		// Test v2.5
		SemanticVersion v25 = new SemanticVersion(2, 5, 0);
		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v25));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v25));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v25));
		assertTrue("Should detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v25));
		assertTrue("v2.5 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v25));
		assertTrue("v2.5 should support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v25));
		assertTrue("v2.5 should use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v25));
		assertTrue("v2.5 should support extended connector types", AlpitronicVersionUtils.supportsExtendedConnectorTypes(v25));
	}

	@Test
	public void testVersionComparison() {
		SemanticVersion v18 = new SemanticVersion(1, 8, 0);
		SemanticVersion v25 = new SemanticVersion(2, 5, 3);
		SemanticVersion v30 = new SemanticVersion(3, 0, 0);

		// Test isAtLeast
		assertTrue("v1.8 should be at least 1.8", v18.isAtLeast(new SemanticVersion(1, 8, 0)));
		assertFalse("v1.8 should not be at least 2.0", v18.isAtLeast(new SemanticVersion(2, 0, 0)));

		assertTrue("v2.5 should be at least 2.0", v25.isAtLeast(new SemanticVersion(2, 0, 0)));
		assertTrue("v2.5 should be at least 2.5", v25.isAtLeast(new SemanticVersion(2, 5, 0)));
		assertFalse("v2.5 should not be at least 3.0", v25.isAtLeast(new SemanticVersion(3, 0, 0)));

		assertTrue("v3.0 should be at least 2.5", v30.isAtLeast(new SemanticVersion(2, 5, 0)));
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