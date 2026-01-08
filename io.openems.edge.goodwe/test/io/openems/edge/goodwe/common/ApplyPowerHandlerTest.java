package io.openems.edge.goodwe.common;

import static io.openems.edge.goodwe.common.ApplyPowerHandler.checkControlModeRequiresSmartMeter;
import static io.openems.edge.goodwe.common.ApplyPowerHandler.checkControlModeWithActivePid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

public class ApplyPowerHandlerTest {

	@Test
	public void testApply() throws OpenemsNamedException {
		final var smartModeNotWorkingWithPidFilter = new AtomicBoolean();
		final var noSmartMeterDetected = new AtomicBoolean();
		final var emsPowerSet = new AtomicInteger();
		final var emsPowerMode = new AtomicReference<EmsPowerMode>();

		ApplyPowerHandler.apply(//
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* maxAcImport */ new Value<>(null, 5000), //
				/* maxAcExport */ new Value<>(null, 5000), //
				/* isPidEnabled */ true, //
				MeterCommunicateStatus.NG, //
				/* pvProduction */ 300, //
				/* surplusPower */ 105, //
				smartModeNotWorkingWithPidFilter::set, //
				noSmartMeterDetected::set, //
				emsPowerSet::set, //
				emsPowerMode::set //
		);

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

		// SMART - Surplus: activePowerSetPoint == surplusPower → AUTO (balanced)
		assertResult(EmsPowerMode.AUTO, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 105,
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ 250, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 105));

		// SMART - CHARGE_BAT: Battery charges with PV surplus
		assertResult(EmsPowerMode.CHARGE_BAT, 2000, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 1000,
				/* pvProduction */ 3000,
				ControlMode.SMART, //
				/* gridActivePower */ 500,
				/* essActivePower */ 300, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 0));

		// SMART - Constraint: activePowerSetPoint == 0 → battery neutral
		assertResult(EmsPowerMode.DISCHARGE_BAT, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 0,
				/* pvProduction */ 300, //
				ControlMode.SMART, //
				/* gridActivePower */ 350, //
				/* essActivePower */ 420, //
				/* maxAcImport */ 5000, //
				/* maxAcExport */ 5000, //
				/* surplusPower */ 0));

		// SMART - Missing data falls back to AUTO
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
	public void testCheckControlModeWithActivePid() throws OpenemsNamedException {
		assertFalse(checkControlModeWithActivePid(ControlMode.SMART, false));
		assertFalse(checkControlModeWithActivePid(ControlMode.REMOTE, true));
		assertTrue(checkControlModeWithActivePid(ControlMode.SMART, true));
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
		final var smartModeNotWorkingWithPidFilter = new AtomicBoolean();
		final var noSmartMeterDetected = new AtomicBoolean();
		final var emsPowerSet = new AtomicInteger();
		final var emsPowerMode = new AtomicReference<EmsPowerMode>();

		ApplyPowerHandler.apply(//
				/* setActivePower */ 5000, // Previous test with 1000 is obsolete due to maxAcImport/Export limits
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* maxAcImport */ new Value<>(null, 5000), // Not used in apply
				/* maxAcExport */ new Value<>(null, 5000), // Not used in apply
				/* isPidEnabled */ true, //
				MeterCommunicateStatus.NG, //
				/* pvProduction */ 300, //
				/* surplusPower */ 105, //
				smartModeNotWorkingWithPidFilter::set, //
				noSmartMeterDetected::set, //
				emsPowerSet::set, //
				emsPowerMode::set //
		);

		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(4700, emsPowerSet.get());
		assertEquals(EmsPowerMode.DISCHARGE_BAT, emsPowerMode.get());

		// --- AC set-point of -5kW (import) ---
		ApplyPowerHandler.apply(//
				/* setActivePower */ -5_000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* maxAcImport */ new Value<>(null, 20000), // Not used in apply
				/* maxAcExport */ new Value<>(null, 20000), // Not used in apply
				/* isPidEnabled */ true, //
				MeterCommunicateStatus.NG, //
				/* pvProduction */ 300, //
				/* surplusPower */ 105, //
				smartModeNotWorkingWithPidFilter::set, //
				noSmartMeterDetected::set, //
				emsPowerSet::set, //
				emsPowerMode::set //
		);

		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(5300, emsPowerSet.get());
		assertEquals(EmsPowerMode.CHARGE_BAT, emsPowerMode.get());

		// --- AC set-point of -5kW (import) ---
		ApplyPowerHandler.apply(//
				/* setActivePower */ -5_000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* maxAcImport */ new Value<>(null, 20000), // Not used in apply
				/* maxAcExport */ new Value<>(null, 20000), // Not used in apply
				/* isPidEnabled */ true, //
				MeterCommunicateStatus.NG, //
				/* pvProduction */ 300, //
				/* surplusPower */ 105, //
				smartModeNotWorkingWithPidFilter::set, //
				noSmartMeterDetected::set, //
				emsPowerSet::set, //
				emsPowerMode::set //
		);

		assertTrue(smartModeNotWorkingWithPidFilter.get());
		assertTrue(noSmartMeterDetected.get());
		assertEquals(5300, emsPowerSet.get());
		assertEquals(EmsPowerMode.CHARGE_BAT, emsPowerMode.get());
	}

	@Test
	public void testConstraintedRealWorldScenario() throws OpenemsNamedException {
		// Real-world scenario: evcc storage lock with EV charging (GitHub evcc-io/evcc #22827)
		// PV: 5000W, Consumption: 12200W, Loadpoint-Consumption: 7200, Constraint: battery not discharging

		// REMOTE mode: battery stays neutral
		assertResult(EmsPowerMode.DISCHARGE_BAT, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 0,
				/* pvProduction */ 5000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ 12200, //
				/* essActivePower */ -5000, //
				/* maxAcImport */ 20000, //
				/* maxAcExport */ 20000, //
				/* surplusPower */ 0));

		// SMART mode: falls through to REMOTE, same result
		assertResult(EmsPowerMode.DISCHARGE_BAT, 0, ApplyPowerHandler.calculate(//
				/* activePowerSetPoint */ 0,
				/* pvProduction */ 5000, //
				ControlMode.SMART, //
				/* gridActivePower */ 12200, //
				/* essActivePower */ -5000, //
				/* maxAcImport */ 20000, //
				/* maxAcExport */ 20000, //
				/* surplusPower */ 0));
	}
}
