package io.openems.edge.evcs.alpitronic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.types.SemanticVersion;

class VersionCompatibilityTest {

	@Test
	void testFirmwareVersion18Detection() {
		// Test v1.8
		var v18 = new SemanticVersion(1, 8, 0);
		assertTrue(AlpitronicVersionUtils.isVersion18(v18));
		assertFalse(AlpitronicVersionUtils.isVersion23(v18));
		assertFalse(AlpitronicVersionUtils.isVersion24(v18));
		assertFalse(AlpitronicVersionUtils.isVersion25OrLater(v18));
		assertFalse(AlpitronicVersionUtils.supportsTotalChargedEnergy(v18));
		assertFalse(AlpitronicVersionUtils.supportsMaxChargingPowerAC(v18));
		assertFalse(AlpitronicVersionUtils.usesApparentPowerForStation(v18));
	}

	@Test
	void testFirmwareVersion23Detection() {
		// Test v2.3
		var v23 = new SemanticVersion(2, 3, 0);
		assertFalse(AlpitronicVersionUtils.isVersion18(v23));
		assertTrue(AlpitronicVersionUtils.isVersion23(v23));
		assertFalse(AlpitronicVersionUtils.isVersion24(v23));
		assertFalse(AlpitronicVersionUtils.isVersion25OrLater(v23));
		assertTrue(AlpitronicVersionUtils.supportsTotalChargedEnergy(v23));
		assertFalse(AlpitronicVersionUtils.supportsMaxChargingPowerAC(v23));
		assertFalse(AlpitronicVersionUtils.usesApparentPowerForStation(v23));
	}

	@Test
	void testFirmwareVersion24Detection() {
		// Test v2.4
		var v24 = new SemanticVersion(2, 4, 0);
		assertFalse(AlpitronicVersionUtils.isVersion18(v24));
		assertFalse(AlpitronicVersionUtils.isVersion23(v24));
		assertTrue(AlpitronicVersionUtils.isVersion24(v24));
		assertFalse(AlpitronicVersionUtils.isVersion25OrLater(v24));
		assertTrue(AlpitronicVersionUtils.supportsTotalChargedEnergy(v24));
		assertTrue(AlpitronicVersionUtils.supportsMaxChargingPowerAC(v24));
		assertFalse(AlpitronicVersionUtils.usesApparentPowerForStation(v24));
	}

	@Test
	void testFirmwareVersion25Detection() {
		// Test v2.5
		var v25 = new SemanticVersion(2, 5, 0);
		assertFalse(AlpitronicVersionUtils.isVersion18(v25));
		assertFalse(AlpitronicVersionUtils.isVersion23(v25));
		assertFalse(AlpitronicVersionUtils.isVersion24(v25));
		assertTrue(AlpitronicVersionUtils.isVersion25OrLater(v25));
		assertTrue(AlpitronicVersionUtils.supportsTotalChargedEnergy(v25));
		assertTrue(AlpitronicVersionUtils.supportsMaxChargingPowerAC(v25));
		assertTrue(AlpitronicVersionUtils.usesApparentPowerForStation(v25));
	}

	@Test
	void testFirmwareVersion253Detection() {
		// Test v2.5.3
		var v253 = new SemanticVersion(2, 5, 3);
		assertTrue(AlpitronicVersionUtils.isVersion25OrLater(v253));
	}

	@Test
	void testFirmwareVersion30Detection() {
		// Test v3.0 (future version)
		var v30 = new SemanticVersion(3, 0, 0);
		assertTrue(AlpitronicVersionUtils.isVersion25OrLater(v30));
		assertTrue(AlpitronicVersionUtils.supportsTotalChargedEnergy(v30));
		assertTrue(AlpitronicVersionUtils.supportsMaxChargingPowerAC(v30));
		assertTrue(AlpitronicVersionUtils.usesApparentPowerForStation(v30));
	}

	@Test
	void testConnectorTypeMapping() {
		var v24 = new SemanticVersion(2, 4, 0);
		assertFalse(AlpitronicVersionUtils.supportsExtendedConnectorTypes(v24));

		var v25 = new SemanticVersion(2, 5, 0);
		assertTrue(AlpitronicVersionUtils.supportsExtendedConnectorTypes(v25));
	}
}
