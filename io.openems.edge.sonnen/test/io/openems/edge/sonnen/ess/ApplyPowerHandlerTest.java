package io.openems.edge.sonnen.ess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.sonnen.core.OperatingMode;

public class ApplyPowerHandlerTest {

	private static final OperatingMode DEFAULT_MODE = OperatingMode.SELF_CONSUMPTION;
	private static final int MAX_CHARGE_POWER = 3300;
	private static final int MAX_DISCHARGE_POWER = 3300;

	@Test
	public void testInternalMode() {
		var result = ApplyPowerHandler.calculate(//
				-1000, ControlMode.INTERNAL, 100, 500, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.SELF_CONSUMPTION, result.operatingMode());
		assertEquals(null, result.chargeSetpointW());
		assertEquals(null, result.dischargeSetpointW());
	}

	@Test
	public void testRemoteCharge() {
		var result = ApplyPowerHandler.calculate(//
				-1000, ControlMode.REMOTE, 1000, -500, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(1000, result.chargeSetpointW());
		assertEquals(0, result.dischargeSetpointW());
	}

	@Test
	public void testRemoteChargeClampedToMax() {
		var result = ApplyPowerHandler.calculate(//
				-5000, ControlMode.REMOTE, 5000, -1000, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(MAX_CHARGE_POWER, result.chargeSetpointW());
	}

	@Test
	public void testRemoteDischarge() {
		var result = ApplyPowerHandler.calculate(//
				1000, ControlMode.REMOTE, -1000, 500, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(0, result.chargeSetpointW());
		assertEquals(1000, result.dischargeSetpointW());
	}

	@Test
	public void testRemoteDischargeClampedToMax() {
		var result = ApplyPowerHandler.calculate(//
				5000, ControlMode.REMOTE, -5000, 1000, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(MAX_DISCHARGE_POWER, result.dischargeSetpointW());
	}

	@Test
	public void testRemoteStandby() {
		var result = ApplyPowerHandler.calculate(//
				0, ControlMode.REMOTE, 0, 0, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(0, result.chargeSetpointW());
		assertEquals(0, result.dischargeSetpointW());
	}

	@Test
	public void testSmartModeStaysInternalIfSetpointIsMet() {
		// The sonnenBatterie in its internal operating mode already keeps the grid
		// exchange at the set-point (balancing to zero)
		var result = ApplyPowerHandler.calculate(//
				0, ControlMode.SMART, 30, -30, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.SELF_CONSUMPTION, result.operatingMode());
		assertEquals(null, result.chargeSetpointW());
		assertEquals(null, result.dischargeSetpointW());
	}

	@Test
	public void testSmartModeFallsBackToRemoteIfSetpointIsNotMet() {
		var result = ApplyPowerHandler.calculate(//
				-2000, ControlMode.SMART, 1000, -500, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(2000, result.chargeSetpointW());
	}

	@Test
	public void testSmartModeWithoutGridMeterAppliesRemote() {
		var result = ApplyPowerHandler.calculate(//
				1000, ControlMode.SMART, null, 500, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(1000, result.dischargeSetpointW());
	}

	@Test
	public void testSmartModeWithoutEssValueAppliesRemote() {
		var result = ApplyPowerHandler.calculate(//
				1000, ControlMode.SMART, 1000, null, DEFAULT_MODE, //
				MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
		assertEquals(OperatingMode.MANUAL, result.operatingMode());
		assertEquals(1000, result.dischargeSetpointW());
	}
}
