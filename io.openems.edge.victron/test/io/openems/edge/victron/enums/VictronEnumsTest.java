package io.openems.edge.victron.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Tests for Victron Enums.
 */
public class VictronEnumsTest {

	@Test
	public void testAlarmValues() {
		var values = Alarm.values();
		assertNotNull(values);
		assertEquals(4, values.length);

		assertEquals(0, Alarm.NO_ALARM.getValue());
		assertEquals(1, Alarm.WARNING.getValue());
		assertEquals(2, Alarm.ALARM.getValue());
	}

	@Test
	public void testAllowDisallowValues() {
		var values = AllowDisallow.values();
		assertNotNull(values);
		assertEquals(3, values.length);

		assertEquals(0, AllowDisallow.DISALLOWED.getValue());
		assertEquals(1, AllowDisallow.ALLOWED.getValue());
	}

	@Test
	public void testEnableDisableValues() {
		var values = EnableDisable.values();
		assertNotNull(values);
		assertEquals(3, values.length);

		assertEquals(0, EnableDisable.DISABLE.getValue());
		assertEquals(1, EnableDisable.ENABLE.getValue());
	}

	@Test
	public void testVeBusStateValues() {
		var values = VeBusState.values();
		assertNotNull(values);

		// Check some known states
		assertEquals(0, VeBusState.OFF.getValue());
		assertEquals(3, VeBusState.BULK.getValue());
		assertEquals(4, VeBusState.Absorption.getValue());
		assertEquals(5, VeBusState.FLOAT.getValue());
		assertEquals(9, VeBusState.INVERTING.getValue());
	}

	@Test
	public void testVeBusErrorValues() {
		var values = VeBusError.values();
		assertNotNull(values);

		assertEquals(0, VeBusError.NO_ERROR.getValue());
	}

	@Test
	public void testSwitchPositionValues() {
		var values = SwitchPosition.values();
		assertNotNull(values);

		assertEquals(1, SwitchPosition.CHARGER_ONLY.getValue());
		assertEquals(2, SwitchPosition.INVERTER_ONLY.getValue());
		assertEquals(3, SwitchPosition.ON.getValue());
		assertEquals(4, SwitchPosition.OFF.getValue());
	}

	@Test
	public void testActiveInputSourceValues() {
		var values = ActiveInputSource.values();
		assertNotNull(values);
	}

	@Test
	public void testActiveInactiveValues() {
		var values = ActiveInactive.values();
		assertNotNull(values);

		assertEquals(0, ActiveInactive.INACTIVE.getValue());
		assertEquals(1, ActiveInactive.ACTIVE.getValue());
	}

	@Test
	public void testChargeStateEssValues() {
		var values = ChargeStateEss.values();
		assertNotNull(values);
	}

	@Test
	public void testBatteryStateValues() {
		var values = BatteryState.values();
		assertNotNull(values);
	}

	@Test
	public void testChargeStateValues() {
		var values = ChargeState.values();
		assertNotNull(values);
	}

	@Test
	public void testErrorYesNoValues() {
		var values = ErrorYesNo.values();
		assertNotNull(values);

		assertEquals(0, ErrorYesNo.NO_ERROR.getValue());
		assertEquals(1, ErrorYesNo.FAULT.getValue());
	}

	@Test
	public void testDeviceTypeValues() {
		var values = DeviceType.values();
		assertNotNull(values);
	}

	@Test
	public void testMppOperationModeValues() {
		var values = MppOperationMode.values();
		assertNotNull(values);
	}

	@Test
	public void testOnOffValues() {
		var values = OnOff.values();
		assertNotNull(values);

		assertEquals(4, OnOff.OFF.getValue());
		assertEquals(1, OnOff.ON.getValue());
	}

	@Test
	public void testOpenClosedValues() {
		var values = OpenClosed.values();
		assertNotNull(values);
	}

	@Test
	public void testPositionValues() {
		var values = Position.values();
		assertNotNull(values);
	}

	@Test
	public void testSystemSwitchValues() {
		var values = SystemSwitch.values();
		assertNotNull(values);
	}

	@Test
	public void testVictronStateValues() {
		var values = VictronState.values();
		assertNotNull(values);
	}

	@Test
	public void testVeBusBmsErrorValues() {
		var values = VeBusBmsError.values();
		assertNotNull(values);
	}

}
