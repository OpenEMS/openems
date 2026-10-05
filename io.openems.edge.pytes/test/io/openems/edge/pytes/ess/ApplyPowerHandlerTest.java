package io.openems.edge.pytes.ess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.RemoteDispatchSystemLimitSwitch;
import io.openems.edge.pytes.enums.WorkState;

/**
 * Cycle-by-cycle tests of {@link ApplyPowerHandler} against fakes. The expected
 * register values follow the constants in the handler: bias 190 W, losses
 * 30 W + 3 % of (|battery| + PV), register 44106 in 10 W steps, negative =
 * discharge in battery control, positive = export in AC output control.
 */
public class ApplyPowerHandlerTest {

	private static final int MAX_APPARENT_POWER = 10_000;

	private DummyApplyPowerEss ess;
	private DummyPytesBattery battery;
	private DummyPytesDcCharger charger;
	private ApplyPowerHandler handler;

	@Before
	public void setup() {
		this.ess = new DummyApplyPowerEss("ess0") //
				.withMaxApparentPower(MAX_APPARENT_POWER) //
				.withAllowedChargePower(-2100) //
				.withAllowedDischargePower(2300) //
				.withBatteryLimits(-2100, 2100) //
				.withActivePower(0) //
				.withDcDischargePower(0);
		this.battery = new DummyPytesBattery("battery0") //
				.withDcDischargePower(0) //
				.withBackupLoadPower(0);
		this.charger = new DummyPytesDcCharger("dccharger0") //
				.withActualPower(0);
		this.handler = new ApplyPowerHandler(this.ess, this.battery, this.charger);
	}

	private Optional<?> written(PytesJs3.ChannelId channelId) {
		WriteChannel<?> channel = this.ess.channel(channelId);
		return channel.getNextWriteValueAndReset();
	}

	// Runs one cycle in battery control and returns the value written to reg 44106
	private int applyBatteryControl(int acTarget) throws Exception {
		this.handler.apply(acTarget, 0, MAX_APPARENT_POWER, RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL);
		return (Integer) this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER).orElseThrow();
	}

	// Runs one cycle in AC output control and returns the value written to reg 44106
	private int applyAcOutputControl(int acTarget) throws Exception {
		this.handler.apply(acTarget, 0, MAX_APPARENT_POWER, RemoteDispatchRealtimeControlSwitch.AC_OUTPUT_CONTROL);
		return (Integer) this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER).orElseThrow();
	}

	@Test
	public void batteryControlAppliesFeedForward() throws Exception {
		// 500 W AC with 200 W PV -> 300 W from the battery, plus bias 190 and
		// losses 30 + 3 % * (300 + 200) = 45 -> 535 W -> register -54
		this.charger.withActualPower(200);
		this.ess.withActivePower(500);
		this.battery.withDcDischargePower(300);

		assertEquals(-54, this.applyBatteryControl(500));
		assertEquals(RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL.getValue(),
				this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH).orElseThrow());
		assertEquals(5, this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_FAILSAFE_SETTING).orElseThrow());
		assertEquals(RemoteDispatchSystemLimitSwitch.DISABLE.getValue(),
				this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH).orElseThrow());
	}

	@Test
	public void batteryControlClampsToBmsLimits() throws Exception {
		this.charger.withActualPower(200);
		// far above the discharge limit -> 2100 W -> -210
		assertEquals(-210, this.applyBatteryControl(5000));
		// far below the charge limit -> -2100 W -> +210
		assertEquals(210, this.applyBatteryControl(-5000));
	}

	@Test
	public void batteryControlHasNoFeedForwardWhenIdle() throws Exception {
		// below MIN_TARGET_W the inverter is treated as idle: no bias, no losses
		assertEquals(-2, this.applyBatteryControl(20));
	}

	@Test
	public void acOutputControlSubtractsBackupLoad() throws Exception {
		// 1.2 kW EV on the backup port: the grid-side port only has to deliver
		// the difference
		this.charger.withActualPower(200);
		this.battery.withBackupLoadPower(1190);
		this.ess.withActivePower(1200);

		assertEquals(1, this.applyAcOutputControl(1200));
		assertEquals(RemoteDispatchRealtimeControlSwitch.AC_OUTPUT_CONTROL.getValue(),
				this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH).orElseThrow());
	}

	@Test
	public void acOutputControlExportsPvAboveChargeLimit() throws Exception {
		// PV 5 kW, battery may take 2.1 kW: the inverter must export at least
		// 2.9 kW regardless of the 800 W the controller asks for. The surplus
		// search is off (as while reg 43052 limits) - this tests the clamp only.
		this.charger.withActualPower(5000);
		this.ess.withAllowedChargePower(0).withPvLimitActive(true);
		assertEquals(290, this.applyAcOutputControl(800));
	}

	@Test
	public void acOutputControlSurplusProbeRaisesTheLowerBound() throws Exception {
		// Battery full (charge limit 0), PV curtailed to the 500 W the controller
		// asks for: the probe lifts the lower bound one step above the measured PV
		this.ess.withBatteryLimits(0, 2100).withAllowedChargePower(0).withActivePower(500).withGridPower(-100)
				.withGridFeedInLimit(5000);
		this.charger.withActualPower(500);
		for (int i = 0; i < PvSurplusProbe.AVERAGE_CYCLES - 1; i++) {
			assertEquals(50, this.applyAcOutputControl(500)); // steady window not full yet
		}
		assertEquals(80, this.applyAcOutputControl(500)); // 500 + STEP_W
		assertEquals(800, (int) this.ess.channel(PytesJs3.ChannelId.SURPLUS_FLOOR).getNextValue().get());

		// no search in battery control
		this.handler.apply(500, 0, MAX_APPARENT_POWER, RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL);
		assertEquals(0, (int) this.ess.channel(PytesJs3.ChannelId.SURPLUS_FLOOR).getNextValue().get());
	}

	@Test
	public void acOutputControlClampsToAllowedRange() throws Exception {
		this.charger.withActualPower(200);
		this.ess.withPvLimitActive(true); // clamp only, no surplus search
		assertEquals(230, this.applyAcOutputControl(5000)); // AllowedDischargePower 2300
		assertEquals(-190, this.applyAcOutputControl(-5000)); // PV 200 + charge limit -2100
	}

	@Test
	public void trimStartsAfterWarmUpAndIntegratesSlowly() throws Exception {
		// battery control, no PV: target 500 W, the inverter only delivers 300 W
		this.ess.withActivePower(300);
		this.battery.withDcDischargePower(300);

		int expectedWithoutTrim = -74; // 500 + 190 + (30 + 3 % * 500) = 735 W
		for (int cycle = 1; cycle <= 30; cycle++) {
			assertEquals("cycle " + cycle, expectedWithoutTrim, this.applyBatteryControl(500));
		}
		// from cycle 31 on: +0.04 * 200 W = 8 W per cycle -> after 15 cycles +120 W
		for (int cycle = 31; cycle <= 45; cycle++) {
			this.applyBatteryControl(500);
		}
		assertEquals(-86, this.applyBatteryControl(500)); // 735 + 128 = 863 W
	}

	@Test
	public void trimFreezesAfterSetPointStep() throws Exception {
		this.ess.withActivePower(300);
		this.battery.withDcDischargePower(300);
		for (int cycle = 1; cycle <= 40; cycle++) {
			this.applyBatteryControl(500);
		}
		int before = this.applyBatteryControl(500);
		// a step of 1000 W freezes the trim for 12 s, so the register only
		// changes by the step itself: +1000 W target, +30 W losses -> +103
		this.ess.withActivePower(300); // inverter has not followed yet
		int afterStep = this.applyBatteryControl(1500);
		assertEquals(before - 103, afterStep);
		for (int cycle = 1; cycle <= 10; cycle++) {
			assertEquals("frozen cycle " + cycle, afterStep, this.applyBatteryControl(1500));
		}
	}

	@Test
	public void writesGridFeedInLimit() throws Exception {
		this.ess.withGridFeedInLimit(1000);
		this.applyBatteryControl(500);
		assertEquals(RemoteDispatchSystemLimitSwitch.EXPORT_LIMIT_ENABLE.getValue(),
				this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH).orElseThrow());
		assertEquals(1000, this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT).orElseThrow());

		this.ess.withGridFeedInLimit(null);
		this.applyBatteryControl(500);
		assertEquals(RemoteDispatchSystemLimitSwitch.DISABLE.getValue(),
				this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH).orElseThrow());
		assertEquals(0, this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT).orElseThrow());
	}

	@Test
	public void doesNotWriteInErrorState() throws Exception {
		this.ess.withWorkState(WorkState.ERROR);
		this.handler.apply(500, 0, MAX_APPARENT_POWER, RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL);
		assertFalse(this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER).isPresent());
		assertFalse(this.written(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SWITCH).isPresent());
	}

	@Test
	public void keepsWritingInWarningState() throws Exception {
		// a warning (derating, fan, ...) is informational - the inverter runs on
		this.ess.withWorkState(WorkState.WARNING);
		assertEquals(-2, this.applyBatteryControl(20));
	}
}
