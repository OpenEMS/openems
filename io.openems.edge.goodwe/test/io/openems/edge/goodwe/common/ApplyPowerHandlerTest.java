package io.openems.edge.goodwe.common;

import static io.openems.edge.goodwe.common.ApplyPowerHandler.checkControlModeRequiresSmartMeter;
import static io.openems.edge.goodwe.common.ApplyPowerHandler.checkControlModeWithActiveFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

public class ApplyPowerHandlerTest {

	@Test
	public void testApply() throws OpenemsNamedException {
		final var applyPowerHandler = new ApplyPowerHandler(null);
		final var smartModeNotWorkingWithPidFilter = new AtomicBoolean();
		final var noSmartMeterDetected = new AtomicBoolean();
		final var emsPowerSet = new AtomicLong();
		final var emsPowerMode = new AtomicReference<EmsPowerMode>();
		for (int i = 0; i < 20; i++) {
			applyPowerHandler.apply(//
					/* setActivePower */ 1000, //
					ControlMode.SMART, //
					/* gridActivePower */ new Value<>(null, 250), //
					/* essActivePower */ new Value<>(null, (int) emsPowerSet.get()), //
					/* maxAcImport */ new Value<>(null, 5000), //
					/* maxAcExport */ new Value<>(null, 5000), //
					/* isPidEnabled */ true, //
					MeterCommunicateStatus.NG, //
					/* pvProduction */ 300, //
					/* surplusPower */ 105, //
					smartModeNotWorkingWithPidFilter::set, //
					noSmartMeterDetected::set, //
					emsPowerSet::set, //
					emsPowerMode::set, //
					/* maxBatChargeP */ null, //
					/* maxBatDischargeP */ null //
			);
		}
		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(700, emsPowerSet.get());
		assertEquals(EmsPowerMode.DISCHARGE_BAT, emsPowerMode.get());
	}

	@Test
	public void testCalculate() throws OpenemsNamedException {
		// INTERNAL
		assertResult(EmsPowerMode.AUTO, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000, //
				/* pvProduction */ 300, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// REMOTE positive
		assertResult(EmsPowerMode.DISCHARGE_BAT, 700, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000, //
				/* pvProduction */ 300, //
				ControlMode.REMOTE, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// REMOTE negative
		assertResult(EmsPowerMode.CHARGE_BAT, 1300, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ -1000, //
				/* pvProduction */ 300, //
				ControlMode.REMOTE, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// SMART - DISCHARGE_BAT
		assertResult(EmsPowerMode.DISCHARGE_BAT, 700, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000, //
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// SMART - CHARGE_BAT
		assertResult(EmsPowerMode.CHARGE_BAT, 300, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 0, //
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// SMART - null
		assertResult(EmsPowerMode.AUTO, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000, //
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ null, //
				/* essActivePower */ null, //
				/* maxAcImport */ null, //
				/* maxAcExport */ null, //
				/* surplusPower */ 105));

		// SMART - DISCHARGE_BAT (without Grid-Meter)
		assertResult(EmsPowerMode.DISCHARGE_BAT, 700, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000, //
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ null, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// SMART - AUTO (Balancing)
		assertResult(EmsPowerMode.AUTO, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 670, //
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));
	}

	@Test
	public void testCheckControlModeWithActiveFilter() throws OpenemsNamedException {
		assertFalse(checkControlModeWithActiveFilter(ControlMode.SMART, false));
		assertFalse(checkControlModeWithActiveFilter(ControlMode.REMOTE, true));
		assertTrue(checkControlModeWithActiveFilter(ControlMode.SMART, true));
	}

	@Test
	public void testCheckControlModeRequiresSmartMeter() throws OpenemsNamedException {
		checkControlModeRequiresSmartMeter(ControlMode.INTERNAL, MeterCommunicateStatus.UNDEFINED);
		checkControlModeRequiresSmartMeter(ControlMode.INTERNAL, MeterCommunicateStatus.OK);
		checkControlModeRequiresSmartMeter(ControlMode.INTERNAL, MeterCommunicateStatus.NG);
		checkControlModeRequiresSmartMeter(ControlMode.SMART, MeterCommunicateStatus.NG);
		checkControlModeRequiresSmartMeter(ControlMode.REMOTE, MeterCommunicateStatus.NG);
	}

	private static void assertResult(EmsPowerMode emsPowerMode, int emsPowerSet, ApplyPowerHandler.Result result) {
		assertEquals(emsPowerMode, result.emsPowerMode());
		assertEquals(emsPowerSet, result.emsPowerSet());
	}

	@Test
	public void testApplyWithEmsPowerSet() throws OpenemsNamedException {
		final var applyPowerHandler = new ApplyPowerHandler(null);
		final var smartModeNotWorkingWithPidFilter = new AtomicBoolean();
		final var noSmartMeterDetected = new AtomicBoolean();
		final var emsPowerSet = new AtomicLong();
		final var emsPowerMode = new AtomicReference<EmsPowerMode>();
		for (int i = 0; i < 24; i++) {
			applyPowerHandler.apply(//
					/* setActivePower */ 5000, // Previous test with 1000 is obsolete due to maxAcImport/Export limits
					ControlMode.SMART, //
					/* gridActivePower */ new Value<Integer>(null, 250), //
					/* essActivePower */ new Value<>(null, (int) emsPowerSet.get()), //
					/* maxAcImport */ new Value<>(null, -5000), // Not used in apply
					/* maxAcExport */ new Value<>(null, 5000), // Not used in apply
					/* isPidEnabled */ true, //
					MeterCommunicateStatus.NG, //
					/* pvProduction */ 300, //
					/* surplusPower */ 105, //
					smartModeNotWorkingWithPidFilter::set, //
					noSmartMeterDetected::set, //
					emsPowerSet::set, //
					emsPowerMode::set, //
					/* maxBatChargeP */ null, //
					/* maxBatDischargeP */ null //
			);
		}

		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(4700, emsPowerSet.get());
		assertEquals(EmsPowerMode.DISCHARGE_BAT, emsPowerMode.get());

		for (int i = 0; i < 19; i++) {
			// --- AC set-point of -5kW (import) ---
			applyPowerHandler.apply(//
					/* setActivePower */ -5_000, //
					ControlMode.SMART, //
					/* gridActivePower */ new Value<Integer>(null, 250), //
					/* essActivePower */ new Value<>(null, (int) emsPowerSet.get()), //
					/* maxAcImport */ new Value<>(null, -20000), // Not used in apply
					/* maxAcExport */ new Value<>(null, 20000), // Not used in apply
					/* isPidEnabled */ true, //
					MeterCommunicateStatus.NG, //
					/* pvProduction */ 300, //
					/* surplusPower */ 105, //
					smartModeNotWorkingWithPidFilter::set, //
					noSmartMeterDetected::set, //
					emsPowerSet::set, //
					emsPowerMode::set, //
					/* maxBatChargeP */ null, //
					/* maxBatDischargeP */ null //
			);
		}
		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(5300, emsPowerSet.get());
		assertEquals(EmsPowerMode.CHARGE_BAT, emsPowerMode.get());
	}

	@Test
	public void testApplyInternalFilter_NullLimits() {
		// Test DISCHARGE_BAT with null maxBatDischargeP → no upper clamp, passes
		// through
		var resultNullImport = new ApplyPowerHandler.Result(EmsPowerMode.DISCHARGE_BAT, 3000L);
		var filteredNullImport = new ApplyPowerHandler(null).applyInternalFilter(//
				false, //
				resultNullImport, //
				/* maxBatChargeP */ null, //
				/* maxBatDischargeP */ 5000);
		assertEquals(3000L, filteredNullImport);

		// Test CHARGE_BAT with null maxBatChargeP → no upper clamp, passes through
		var resultNullExport = new ApplyPowerHandler.Result(EmsPowerMode.CHARGE_BAT, 2500L);
		var filteredNullExport = new ApplyPowerHandler(null).applyInternalFilter(//
				false, //
				resultNullExport, //
				/* maxBatChargeP */ 5000, //
				/* maxBatDischargeP */ null);
		assertEquals(2500L, filteredNullExport);

		// Test DISCHARGE_BAT with both null → no clamping, passes through
		var resultBothNull = new ApplyPowerHandler.Result(EmsPowerMode.DISCHARGE_BAT, 4000L);
		var filteredBothNull = new ApplyPowerHandler(null).applyInternalFilter(//
				false, //
				resultBothNull, //
				/* maxBatChargeP */ null, //
				/* maxBatDischargeP */ null);
		assertEquals(4000L, filteredBothNull);
	}

	@Disabled
	@Test
	public void testApplyInternalFilter_WithValidLimits() {
		final var applyPowerHandler = new ApplyPowerHandler(null);

		// Test DISCHARGE_BAT mode with valid limits - PT1 filter should be applied
		var resultDischarge = new ApplyPowerHandler.Result(EmsPowerMode.DISCHARGE_BAT, 1000);
		var filteredDischarge = applyPowerHandler.applyInternalFilter(//
				false, //
				resultDischarge, //
				/* maxBatChargeP */ null, //
				/* maxBatDischargeP */ 5000);
		// PT1 filter should smooth the transition, so result should be < target
		assertTrue(filteredDischarge > 0);
		assertTrue(filteredDischarge <= 1000);

		// Test CHARGE_BAT mode with valid limits
		var resultCharge = new ApplyPowerHandler.Result(EmsPowerMode.CHARGE_BAT, 2000);
		var filteredCharge = applyPowerHandler.applyInternalFilter(//
				false, //
				resultCharge, //
				/* maxBatChargeP */ 10000, //
				/* maxBatDischargeP */ null);
		// PT1 should smooth the transition
		assertTrue(filteredCharge > 0);
		assertTrue(filteredCharge <= 2000);
	}

	@Disabled
	@Test
	public void testApplyInternalFilter_PidConvergence() {
		final var applyPowerHandler = new ApplyPowerHandler(null);

		// Test that PT1 filter converges over multiple iterations
		long lastFiltered = 0;
		for (int i = 0; i < 10; i++) {
			var result = new ApplyPowerHandler.Result(EmsPowerMode.DISCHARGE_BAT, 1000);
			lastFiltered = applyPowerHandler.applyInternalFilter(//
					false, //
					result, //
					/* maxBatChargeP */ null, //
					/* maxBatDischargeP */ 5000);
		}
		// After 10 iterations, should be close to target (within reasonable tolerance)
		assertTrue(lastFiltered >= 900); // Should converge close to 1000
		assertTrue(lastFiltered <= 1000); // Should not exceed target
	}

	@Disabled
	@Test
	public void testApplyInternalFilter_RespectLimits() {
		final var applyPowerHandler = new ApplyPowerHandler(null);

		// Test that PT1 filter respects discharge limit
		var resultHighExport = new ApplyPowerHandler.Result(EmsPowerMode.DISCHARGE_BAT, 10000);
		var filteredHighExport = applyPowerHandler.applyInternalFilter(//
				false, //
				resultHighExport, //
				/* maxBatChargeP */ null, //
				/* maxBatDischargeP */ 3000);
		// Should be limited to maxBatDischargeP
		assertTrue(filteredHighExport <= 3000);

		// Test that PT1 filter respects charge limit
		var resultHighImport = new ApplyPowerHandler.Result(EmsPowerMode.CHARGE_BAT, 10000);
		var filteredHighImport = applyPowerHandler.applyInternalFilter(//
				false, //
				resultHighImport, //
				/* maxBatChargeP */ 2500, //
				/* maxBatDischargeP */ null);
		// Should be limited to maxBatChargeP
		assertTrue(filteredHighImport <= 2500);
	}

	@Disabled
	@Test
	public void testApplyInternalFilter_AllEmsPowerModes() {
		// Test various EmsPowerMode values to ensure they all work with PID filter
		EmsPowerMode[] nonAutoModes = { //
				EmsPowerMode.BATTERY_STANDBY, //
				EmsPowerMode.BUY_POWER, //
				EmsPowerMode.CHARGE_BAT, //
				EmsPowerMode.CHARGE_PV, //
				EmsPowerMode.CONSERVE, //
				EmsPowerMode.DISCHARGE_BAT, //
				EmsPowerMode.DISCHARGE_PV, //
				EmsPowerMode.EXPORT_AC, //
				EmsPowerMode.IMPORT_AC, //
				EmsPowerMode.OFF_GRID, //
				EmsPowerMode.SELL_POWER, //
				EmsPowerMode.STOPPED, //
				EmsPowerMode.UNDEFINED //
		};

		for (EmsPowerMode mode : nonAutoModes) {
			var handler = new ApplyPowerHandler(null);
			var result = new ApplyPowerHandler.Result(mode, 1000);
			var filtered = handler.applyInternalFilter(//
					false, //
					result, //
					/* maxBatChargeP */ 5000, //
					/* maxBatDischargeP */ 5000);
			// All non-AUTO modes should apply PT1 filter
			assertTrue(filtered > 0, "Mode " + mode + " should apply PT1 filter");
			assertTrue(filtered <= 1000, "Mode " + mode + " should not exceed target");
		}
	}
}
