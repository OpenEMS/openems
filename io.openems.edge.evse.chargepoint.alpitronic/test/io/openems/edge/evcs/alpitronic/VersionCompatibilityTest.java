package io.openems.edge.evcs.alpitronic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import io.openems.common.types.SemanticVersion;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

/**
 * Tests for firmware version compatibility.
 */
public class VersionCompatibilityTest {

	@Test
	public void testFirmwareVersionDetection() {
		System.out.println("\n=== Testing Firmware Version Detection ===");

		// Test v1.8
		System.out.println("\n--- Testing v1.8.14 ---");
		SemanticVersion v18 = new SemanticVersion(1, 8, 14);
		System.out.println("Created version: " + v18);
		System.out.println("isVersion18: " + AlpitronicVersionUtils.isVersion18(v18));
		System.out.println("isVersion23: " + AlpitronicVersionUtils.isVersion23(v18));
		System.out.println("isVersion24: " + AlpitronicVersionUtils.isVersion24(v18));
		System.out.println("isVersion25OrLater: " + AlpitronicVersionUtils.isVersion25OrLater(v18));
		System.out.println("supportsTotalChargedEnergy: " + AlpitronicVersionUtils.supportsTotalChargedEnergy(v18));
		System.out.println("supportsMaxChargingPowerAC: " + AlpitronicVersionUtils.supportsMaxChargingPowerAC(v18));
		System.out.println("usesApparentPowerForStation: " + AlpitronicVersionUtils.usesApparentPowerForStation(v18));

		assertTrue("Should detect v1.8", AlpitronicVersionUtils.isVersion18(v18));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v18));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v18));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v18));
		assertFalse("v1.8 should not support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v18));
		assertFalse("v1.8 should not support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v18));
		assertFalse("v1.8 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v18));

		// Test v2.3
		System.out.println("\n--- Testing v2.3.5 ---");
		SemanticVersion v23 = new SemanticVersion(2, 3, 5);
		System.out.println("Created version: " + v23);
		System.out.println("isVersion18: " + AlpitronicVersionUtils.isVersion18(v23));
		System.out.println("isVersion23: " + AlpitronicVersionUtils.isVersion23(v23));
		System.out.println("isVersion24: " + AlpitronicVersionUtils.isVersion24(v23));
		System.out.println("isVersion25OrLater: " + AlpitronicVersionUtils.isVersion25OrLater(v23));
		System.out.println("supportsTotalChargedEnergy: " + AlpitronicVersionUtils.supportsTotalChargedEnergy(v23));
		System.out.println("supportsMaxChargingPowerAC: " + AlpitronicVersionUtils.supportsMaxChargingPowerAC(v23));
		System.out.println("usesApparentPowerForStation: " + AlpitronicVersionUtils.usesApparentPowerForStation(v23));

		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v23));
		assertTrue("Should detect v2.3", AlpitronicVersionUtils.isVersion23(v23));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v23));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v23));
		assertTrue("v2.3 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v23));
		assertFalse("v2.3 should not support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v23));
		assertFalse("v2.3 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v23));

		// Test v2.4
		System.out.println("\n--- Testing v2.4.10 ---");
		SemanticVersion v24 = new SemanticVersion(2, 4, 10);
		System.out.println("Created version: " + v24);
		System.out.println("isVersion18: " + AlpitronicVersionUtils.isVersion18(v24));
		System.out.println("isVersion23: " + AlpitronicVersionUtils.isVersion23(v24));
		System.out.println("isVersion24: " + AlpitronicVersionUtils.isVersion24(v24));
		System.out.println("isVersion25OrLater: " + AlpitronicVersionUtils.isVersion25OrLater(v24));
		System.out.println("supportsTotalChargedEnergy: " + AlpitronicVersionUtils.supportsTotalChargedEnergy(v24));
		System.out.println("supportsMaxChargingPowerAC: " + AlpitronicVersionUtils.supportsMaxChargingPowerAC(v24));
		System.out.println("usesApparentPowerForStation: " + AlpitronicVersionUtils.usesApparentPowerForStation(v24));

		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v24));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v24));
		assertTrue("Should detect v2.4", AlpitronicVersionUtils.isVersion24(v24));
		assertFalse("Should not detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v24));
		assertTrue("v2.4 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v24));
		assertTrue("v2.4 should support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v24));
		assertFalse("v2.4 should not use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v24));

		// Test v2.5
		System.out.println("\n--- Testing v2.5.0 ---");
		SemanticVersion v25 = new SemanticVersion(2, 5, 0);
		System.out.println("Created version: " + v25);
		System.out.println("isVersion18: " + AlpitronicVersionUtils.isVersion18(v25));
		System.out.println("isVersion23: " + AlpitronicVersionUtils.isVersion23(v25));
		System.out.println("isVersion24: " + AlpitronicVersionUtils.isVersion24(v25));
		System.out.println("isVersion25OrLater: " + AlpitronicVersionUtils.isVersion25OrLater(v25));
		System.out.println("supportsTotalChargedEnergy: " + AlpitronicVersionUtils.supportsTotalChargedEnergy(v25));
		System.out.println("supportsMaxChargingPowerAC: " + AlpitronicVersionUtils.supportsMaxChargingPowerAC(v25));
		System.out.println("usesApparentPowerForStation: " + AlpitronicVersionUtils.usesApparentPowerForStation(v25));
		System.out.println("supportsExtendedConnectorTypes: " + AlpitronicVersionUtils.supportsExtendedConnectorTypes(v25));

		assertFalse("Should not detect v1.8", AlpitronicVersionUtils.isVersion18(v25));
		assertFalse("Should not detect v2.3", AlpitronicVersionUtils.isVersion23(v25));
		assertFalse("Should not detect v2.4", AlpitronicVersionUtils.isVersion24(v25));
		assertTrue("Should detect v2.5+", AlpitronicVersionUtils.isVersion25OrLater(v25));
		assertTrue("v2.5 should support total charged energy", AlpitronicVersionUtils.supportsTotalChargedEnergy(v25));
		assertTrue("v2.5 should support max charging power AC", AlpitronicVersionUtils.supportsMaxChargingPowerAC(v25));
		assertTrue("v2.5 should use apparent power", AlpitronicVersionUtils.usesApparentPowerForStation(v25));
		assertTrue("v2.5 should support extended connector types", AlpitronicVersionUtils.supportsExtendedConnectorTypes(v25));

		System.out.println("\n=== Firmware Version Detection Tests Complete ===\n");
	}

	@Test
	public void testVersionComparison() {
		System.out.println("\n=== Testing Version Comparison ===");

		SemanticVersion v18 = new SemanticVersion(1, 8, 0);
		SemanticVersion v25 = new SemanticVersion(2, 5, 3);
		SemanticVersion v30 = new SemanticVersion(3, 0, 0);

		System.out.println("\nTesting v1.8.0:");
		System.out.println("  v1.8 >= 1.8: " + v18.isAtLeast(new SemanticVersion(1, 8, 0)));
		System.out.println("  v1.8 >= 2.0: " + v18.isAtLeast(new SemanticVersion(2, 0, 0)));

		// Test isAtLeast
		assertTrue("v1.8 should be at least 1.8", v18.isAtLeast(new SemanticVersion(1, 8, 0)));
		assertFalse("v1.8 should not be at least 2.0", v18.isAtLeast(new SemanticVersion(2, 0, 0)));

		System.out.println("\nTesting v2.5.3:");
		System.out.println("  v2.5 >= 2.0: " + v25.isAtLeast(new SemanticVersion(2, 0, 0)));
		System.out.println("  v2.5 >= 2.5: " + v25.isAtLeast(new SemanticVersion(2, 5, 0)));
		System.out.println("  v2.5 >= 3.0: " + v25.isAtLeast(new SemanticVersion(3, 0, 0)));

		assertTrue("v2.5 should be at least 2.0", v25.isAtLeast(new SemanticVersion(2, 0, 0)));
		assertTrue("v2.5 should be at least 2.5", v25.isAtLeast(new SemanticVersion(2, 5, 0)));
		assertFalse("v2.5 should not be at least 3.0", v25.isAtLeast(new SemanticVersion(3, 0, 0)));

		System.out.println("\nTesting v3.0.0:");
		System.out.println("  v3.0 >= 2.5: " + v30.isAtLeast(new SemanticVersion(2, 5, 0)));

		assertTrue("v3.0 should be at least 2.5", v30.isAtLeast(new SemanticVersion(2, 5, 0)));

		System.out.println("\n=== Version Comparison Tests Complete ===\n");
	}
	
	@Test
	public void testConnectorTypeMapping() {
		System.out.println("\n=== Testing Connector Type Mapping ===");

		// Test that connector type enums exist with unique values
		System.out.println("\nTesting enum values:");
		System.out.println("  CCS_DC value: " + SelectedConnector.CCS_DC.getValue());
		System.out.println("  CHA_DEMO value: " + SelectedConnector.CHA_DEMO.getValue());
		System.out.println("  CCS_AC value: " + SelectedConnector.CCS_AC.getValue());
		System.out.println("  GBT value: " + SelectedConnector.GBT.getValue());
		System.out.println("  CCS2 value: " + SelectedConnector.CCS2.getValue());
		System.out.println("  CCS1 value: " + SelectedConnector.CCS1.getValue());
		System.out.println("  MCS value: " + SelectedConnector.MCS.getValue());
		System.out.println("  NACS value: " + SelectedConnector.NACS.getValue());

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
		System.out.println("\nTesting v1.8-2.4 raw value mapping:");
		System.out.println("  Raw 1 -> " + SelectedConnector.fromRawValue(1, false));
		System.out.println("  Raw 2 -> " + SelectedConnector.fromRawValue(2, false));
		System.out.println("  Raw 3 -> " + SelectedConnector.fromRawValue(3, false));
		System.out.println("  Raw 4 -> " + SelectedConnector.fromRawValue(4, false));

		// v1.8-2.4 mapping
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.fromRawValue(1, false));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(2, false));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(3, false));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(4, false));

		System.out.println("\nTesting v2.5+ raw value mapping:");
		System.out.println("  Raw 1 -> " + SelectedConnector.fromRawValue(1, true));
		System.out.println("  Raw 2 -> " + SelectedConnector.fromRawValue(2, true));
		System.out.println("  Raw 3 -> " + SelectedConnector.fromRawValue(3, true));
		System.out.println("  Raw 4 -> " + SelectedConnector.fromRawValue(4, true));
		System.out.println("  Raw 5 -> " + SelectedConnector.fromRawValue(5, true));
		System.out.println("  Raw 6 -> " + SelectedConnector.fromRawValue(6, true));
		System.out.println("  Raw 7 -> " + SelectedConnector.fromRawValue(7, true));

		// v2.5+ mapping
		assertEquals(SelectedConnector.CCS2, SelectedConnector.fromRawValue(1, true));
		assertEquals(SelectedConnector.CCS1, SelectedConnector.fromRawValue(2, true));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(3, true));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(4, true));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(5, true));
		assertEquals(SelectedConnector.MCS, SelectedConnector.fromRawValue(6, true));
		assertEquals(SelectedConnector.NACS, SelectedConnector.fromRawValue(7, true));

		System.out.println("\n=== Connector Type Mapping Tests Complete ===\n");
	}
}