package io.openems.edge.evcs.alpitronic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

class SelectedConnectorTest {

	@Test
	void testAllConnectorTypes() {
		// Verify all enum values are properly defined
		assertEquals(-1, SelectedConnector.UNDEFINED.getValue());
		assertEquals(0, SelectedConnector.CHARGE_POINT.getValue());
		assertEquals(10, SelectedConnector.CCS_DC.getValue());
		assertEquals(11, SelectedConnector.CCS1.getValue());
		assertEquals(12, SelectedConnector.CCS2.getValue());
		assertEquals(20, SelectedConnector.CHA_DEMO.getValue());
		assertEquals(30, SelectedConnector.CCS_AC.getValue());
		assertEquals(40, SelectedConnector.GBT.getValue());
		assertEquals(60, SelectedConnector.MCS.getValue());
		assertEquals(70, SelectedConnector.NACS.getValue());
	}

	@Test
	void testFromRawValueV18to24() {
		// v1.8-2.4 mapping
		assertEquals(SelectedConnector.CHARGE_POINT, SelectedConnector.fromRawValue(0, false));
		assertEquals(SelectedConnector.CCS_DC, SelectedConnector.fromRawValue(1, false));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(2, false));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(3, false));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(4, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(5, false));
	}

	@Test
	void testFromRawValueV25Plus() {
		// v2.5+ mapping
		assertEquals(SelectedConnector.CHARGE_POINT, SelectedConnector.fromRawValue(0, true));
		assertEquals(SelectedConnector.CCS2, SelectedConnector.fromRawValue(1, true));
		assertEquals(SelectedConnector.CCS1, SelectedConnector.fromRawValue(2, true));
		assertEquals(SelectedConnector.CHA_DEMO, SelectedConnector.fromRawValue(3, true));
		assertEquals(SelectedConnector.CCS_AC, SelectedConnector.fromRawValue(4, true));
		assertEquals(SelectedConnector.GBT, SelectedConnector.fromRawValue(5, true));
		assertEquals(SelectedConnector.MCS, SelectedConnector.fromRawValue(6, true));
		assertEquals(SelectedConnector.NACS, SelectedConnector.fromRawValue(7, true));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(8, true));
	}

	@Test
	void testVersionDifferences() {
		// Verify that CCS_DC (v1.8-2.4) is different from CCS2 (v2.5+)
		var ccs2V18 = SelectedConnector.fromRawValue(1, false);
		var ccs2V25 = SelectedConnector.fromRawValue(1, true);
		assertEquals(SelectedConnector.CCS_DC, ccs2V18);
		assertEquals(SelectedConnector.CCS2, ccs2V25);

		// Verify that CHA_DEMO has same mapping in both versions
		var chademoV18 = SelectedConnector.fromRawValue(2, false);
		var chademoV25 = SelectedConnector.fromRawValue(3, true);
		assertEquals(SelectedConnector.CHA_DEMO, chademoV18);
		assertEquals(SelectedConnector.CHA_DEMO, chademoV25);
	}

	@Test
	void testEdgeCases() {
		// Out-of-range values should return UNDEFINED
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-1, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(100, false));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(-1, true));
		assertEquals(SelectedConnector.UNDEFINED, SelectedConnector.fromRawValue(100, true));
	}
}
