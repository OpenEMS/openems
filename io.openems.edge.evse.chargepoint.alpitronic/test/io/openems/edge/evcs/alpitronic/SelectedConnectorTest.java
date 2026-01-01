package io.openems.edge.evcs.alpitronic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

/**
 * Comprehensive tests for SelectedConnector enum.
 *
 * <p>
 * Tests all connector types, their values, names, and version-specific
 * mappings.
 */
public class SelectedConnectorTest {

	@Test
	public void testAllConnectorTypes() {
		System.out.println("\n=== Testing All Connector Types ===");

		System.out.println("Testing UNDEFINED");
		assertEquals(-1, SelectedConnector.UNDEFINED.getValue());
		assertEquals("Undefined", SelectedConnector.UNDEFINED.getName());

		System.out.println("Testing CHARGE_POINT");
		assertEquals(0, SelectedConnector.CHARGE_POINT.getValue());
		assertEquals("ChargePoint", SelectedConnector.CHARGE_POINT.getName());

		System.out.println("Testing CCS_DC");
		assertEquals(10, SelectedConnector.CCS_DC.getValue());
		assertEquals("CCS DC Connector", SelectedConnector.CCS_DC.getName());

		System.out.println("Testing CCS1");
		assertEquals(11, SelectedConnector.CCS1.getValue());
		assertEquals("CCS1 Connector", SelectedConnector.CCS1.getName());

		System.out.println("Testing CCS2");
		assertEquals(12, SelectedConnector.CCS2.getValue());
		assertEquals("CCS2 Connector", SelectedConnector.CCS2.getName());

		System.out.println("Testing CHA_DEMO");
		assertEquals(20, SelectedConnector.CHA_DEMO.getValue());
		assertEquals("CHAdeMO Connector", SelectedConnector.CHA_DEMO.getName());

		System.out.println("Testing CCS_AC");
		assertEquals(30, SelectedConnector.CCS_AC.getValue());
		assertEquals("CCS AC Connector", SelectedConnector.CCS_AC.getName());

		System.out.println("Testing GBT");
		assertEquals(40, SelectedConnector.GBT.getValue());
		assertEquals("GBT Connector", SelectedConnector.GBT.getName());

		System.out.println("Testing MCS");
		assertEquals(60, SelectedConnector.MCS.getValue());
		assertEquals("MCS Connector", SelectedConnector.MCS.getName());

		System.out.println("Testing NACS");
		assertEquals(70, SelectedConnector.NACS.getValue());
		assertEquals("NACS Connector", SelectedConnector.NACS.getName());

		System.out.println("All connector types test passed");
		System.out.println("=== All Connector Types Test Complete ===\n");
	}

	@Test
	public void testGetUndefined() {
		System.out.println("\n=== Testing getUndefined ===");

		SelectedConnector undefined = (SelectedConnector) SelectedConnector.CHARGE_POINT.getUndefined();
		assertEquals("getUndefined should return UNDEFINED", SelectedConnector.UNDEFINED, undefined);

		System.out.println("getUndefined test passed");
		System.out.println("=== getUndefined Test Complete ===\n");
	}

	@Test
	public void testFromRawValueV18to24() {
		System.out.println("\n=== Testing fromRawValue for v1.8-2.4 ===");

		System.out.println("Testing raw value 0 -> CHARGE_POINT");
		assertEquals(SelectedConnector.CHARGE_POINT, SelectedConnector.fromRawValue(0, false));

		System.out.println("Testing raw value 1 -> CCS_DC");
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.fromRawValue(1, false));

		System.out.println("Testing raw value 2 -> CHA_DEMO");
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(2, false));

		System.out.println("Testing raw value 3 -> CCS_AC");
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(3, false));

		System.out.println("Testing raw value 4 -> GBT");
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(4, false));

		System.out.println("Testing unknown raw value 5 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(5, false));

		System.out.println("Testing unknown raw value 99 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(99, false));

		System.out.println("Testing negative raw value -1 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-1, false));

		System.out.println("v1.8-2.4 mapping tests passed");
		System.out.println("=== fromRawValue v1.8-2.4 Test Complete ===\n");
	}

	@Test
	public void testFromRawValueV25Plus() {
		System.out.println("\n=== Testing fromRawValue for v2.5+ ===");

		System.out.println("Testing raw value 0 -> CHARGE_POINT");
		assertEquals(SelectedConnector.CHARGE_POINT, SelectedConnector.fromRawValue(0, true));

		System.out.println("Testing raw value 1 -> CCS2");
		assertEquals(SelectedConnector.CCS2, SelectedConnector.fromRawValue(1, true));

		System.out.println("Testing raw value 2 -> CCS1");
		assertEquals(SelectedConnector.CCS1, SelectedConnector.fromRawValue(2, true));

		System.out.println("Testing raw value 3 -> CHA_DEMO");
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(3, true));

		System.out.println("Testing raw value 4 -> CCS_AC");
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(4, true));

		System.out.println("Testing raw value 5 -> GBT");
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(5, true));

		System.out.println("Testing raw value 6 -> MCS");
		assertEquals(SelectedConnector.MCS, SelectedConnector.fromRawValue(6, true));

		System.out.println("Testing raw value 7 -> NACS");
		assertEquals(SelectedConnector.NACS, SelectedConnector.fromRawValue(7, true));

		System.out.println("Testing unknown raw value 8 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(8, true));

		System.out.println("Testing unknown raw value 99 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(99, true));

		System.out.println("Testing negative raw value -1 -> UNDEFINED");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-1, true));

		System.out.println("v2.5+ mapping tests passed");
		System.out.println("=== fromRawValue v2.5+ Test Complete ===\n");
	}

	@Test
	public void testVersionDifferences() {
		System.out.println("\n=== Testing Version-Specific Mapping Differences ===");

		// Raw value 1 maps differently between versions
		System.out.println("Raw value 1: v1.8-2.4 -> CCS_DC, v2.5+ -> CCS2");
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.fromRawValue(1, false));
		assertEquals(SelectedConnector.CCS2, SelectedConnector.fromRawValue(1, true));

		// Raw value 2 maps differently between versions
		System.out.println("Raw value 2: v1.8-2.4 -> CHA_DEMO, v2.5+ -> CCS1");
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(2, false));
		assertEquals(SelectedConnector.CCS1, SelectedConnector.fromRawValue(2, true));

		// Raw value 3 maps differently between versions
		System.out.println("Raw value 3: v1.8-2.4 -> CCS_AC, v2.5+ -> CHA_DEMO");
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(3, false));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(3, true));

		// Raw value 4 maps differently between versions
		System.out.println("Raw value 4: v1.8-2.4 -> GBT, v2.5+ -> CCS_AC");
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(4, false));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(4, true));

		// Raw value 5 only exists in v2.5+
		System.out.println("Raw value 5: v1.8-2.4 -> UNDEFINED, v2.5+ -> GBT");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(5, false));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(5, true));

		// Raw value 6 only exists in v2.5+ (MCS)
		System.out.println("Raw value 6: v1.8-2.4 -> UNDEFINED, v2.5+ -> MCS");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(6, false));
		assertEquals(SelectedConnector.MCS, SelectedConnector.fromRawValue(6, true));

		// Raw value 7 only exists in v2.5+ (NACS)
		System.out.println("Raw value 7: v1.8-2.4 -> UNDEFINED, v2.5+ -> NACS");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(7, false));
		assertEquals(SelectedConnector.NACS, SelectedConnector.fromRawValue(7, true));

		System.out.println("Version differences test passed");
		System.out.println("=== Version Differences Test Complete ===\n");
	}

	@Test
	public void testAllEnumValues() {
		System.out.println("\n=== Testing All Enum Values ===");

		// Verify all enum values are accessible
		SelectedConnector[] allValues = SelectedConnector.values();
		System.out.println("Total enum values: " + allValues.length);

		assertNotNull("Enum values should not be null", allValues);
		assertEquals("Should have 10 connector types", 10, allValues.length);

		// Test valueOf
		System.out.println("\nTesting valueOf()");
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.valueOf("CCS_DC"));
		assertEquals(SelectedConnector.CCS1, SelectedConnector.valueOf("CCS1"));
		assertEquals(SelectedConnector.CCS2, SelectedConnector.valueOf("CCS2"));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.valueOf("CHA_DEMO"));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.valueOf("CCS_AC"));
		assertEquals(SelectedConnector.GBT, SelectedConnector.valueOf("GBT"));
		assertEquals(SelectedConnector.MCS, SelectedConnector.valueOf("MCS"));
		assertEquals(SelectedConnector.NACS, SelectedConnector.valueOf("NACS"));
		assertEquals(SelectedConnector.CHARGE_POINT, SelectedConnector.valueOf("CHARGE_POINT"));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.valueOf("UNDEFINED"));

		System.out.println("All enum values test passed");
		System.out.println("=== All Enum Values Test Complete ===\n");
	}

	@Test
	public void testEdgeCases() {
		System.out.println("\n=== Testing Edge Cases ===");

		// Test with extreme values
		System.out.println("Testing very large raw value 1000");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(1000, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(1000, true));

		System.out.println("Testing very negative raw value -999");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-999, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-999, true));

		System.out.println("Testing boundary value Integer.MAX_VALUE");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(Integer.MAX_VALUE, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(Integer.MAX_VALUE, true));

		System.out.println("Testing boundary value Integer.MIN_VALUE");
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(Integer.MIN_VALUE, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(Integer.MIN_VALUE, true));

		System.out.println("Edge cases test passed");
		System.out.println("=== Edge Cases Test Complete ===\n");
	}
}
