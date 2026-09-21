package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl.getAcPower;
import static io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl.targetForModeOnce;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.SystemLimits.fromMeta;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TestUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.ess.fixactivepower.enums.HybridEssMode;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

class ControllerEssFixActivePowerImplTest {

	@Test
	void testOn() throws Exception {
		AtomicReference<DummyMeta> meta = new AtomicReference<>(new DummyMeta() //
				.withGridSellHardLimit(10_000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssChargeFromGridAllowed(false));
		final var ess = new DummyManagedAsymmetricEss("ess0");
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum() //
						.withGridActivePower(1000)) //
				.addReference("meta", meta.get()) //
				.addReference("ess", ess) //
				.activate(this.baseConfig(Mode.MANUAL_ON) //
						.setPower(-4000) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, true) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null) //
				) //
				.next(new TestCase("§14a EnWG limit with grid buy of 1000W -> only 3200W left") //
						.onBeforeProcessImage(() -> meta.set(meta.get() //
								.withIsEssDischargeToGridAllowed(true) //
								.withIsEssChargeFromGridAllowed(true) //
								.withGridBuyHardLimit(4200))) //
						.input("ess0", SOC, 80) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS,
								withDefaultBuffer(-4200) + 1000) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
				// PowerConstraint.apply currently not testable with DummyPower because no real
				// constraint is added.
				) //
				.deactivate();
	}

	@Test
	void testOff() throws Exception {
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum() //
						.withGridActivePower(1000)) //
				.addReference("meta",
						new DummyMeta().withGridSellHardLimit(10_000).withGridBuyHardLimit(10_000)
								.withIsEssChargeFromGridAllowed(true)) //
				.addReference("ess", new DummyManagedAsymmetricEss("ess0")) //
				.activate(this.baseConfig(Mode.MANUAL_OFF) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null) //
				) //
				.deactivate();
	}

	@Test
	void testChargeOnce() throws Exception {
		AtomicReference<DummyMeta> meta = new AtomicReference<>(new DummyMeta() //
				.withGridSellHardLimit(10_000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssChargeFromGridAllowed(false));
		final var ess = new DummyManagedAsymmetricEss("ess0");
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("meta", meta.get()) //
				.addReference("ess", ess) //
				.activate(this.baseConfig(Mode.CHARGE_ONCE) //
						.setChargeOncePower(-5000) //
						.setChargeOnceTargetSocEnable(true) //
						.setChargeOnceTargetSoc(80) //
						.build()) //
				.next(new TestCase("ChargeFromGrid not allowed") //
						.input("ess0", SOC, 50) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, true) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null) //
				) //
				.next(new TestCase("Charge (limited by 14a) once with permission") //
						.onBeforeProcessImage(() -> meta.set(meta.get() //
								.withIsEssDischargeToGridAllowed(true) //
								.withIsEssChargeFromGridAllowed(true) //
								.withGridBuyHardLimit(4200)))
						.input("ess0", SOC, 50) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS,
								withDefaultBuffer(-4200)) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false)) //
				.next(new TestCase("Charging finished not allowed") //
						.input("ess0", SOC, 81) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null) //
				).deactivate();
	}

	@Test
	void testChargeOnceWithServicePermission() throws Exception {
		AtomicReference<DummyMeta> meta = new AtomicReference<>(new DummyMeta() //
				.withGridSellHardLimit(10_000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssDischargeToGridAllowed(false) //
				.withIsEssChargeFromGridAllowed(false));
		final var ess = new DummyManagedAsymmetricEss("ess0");
		final var cm = new DummyConfigurationAdmin();
		final var id = "ctrl0";
		final var sut = new ControllerEssFixActivePowerImpl();
		new ControllerTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("meta", meta.get()) //
				.addReference("ess", ess) //
				.activate(this.baseConfig(Mode.CHARGE_ONCE) //
						.setChargeOncePower(-5000) //
						.setId(id) //
						.setChargeOnceTargetSocEnable(true) //
						.setChargeOnceTargetSoc(80) //
						.setIgnoreSystemLimitsPermissionsOnce(true) //
						.build()) //
				.next(new TestCase("ChargeFromGrid normally not allowed -> Service Permission for on event") //
						.input("ess0", SOC, 50) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, -5000) //
				) //
				.next(new TestCase("Charge (limited by 14a) once with permission") //
						.onBeforeProcessImage(() -> meta.set(meta.get() //
								.withGridBuyHardLimit(4200))) //
						.input("ess0", SOC, 50) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS,
								withDefaultBuffer(-4200)) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false)) //

				.next(new TestCase("Permission still set once") //
						.input("ess0", SOC, 50) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, true) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS,
								withDefaultBuffer(-4200)) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.onAfterControllersCallbacks(() -> {
							var config = cm.getConfiguration("ctrl0", "?");
							var permission = config.getProperties().get("ignoreSystemLimitsPermissionsOnce");
							assertTrue((boolean) permission);
						})) //

				.next(new TestCase("Charging finished not allowed") //
						.input("ess0", SOC, 81) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null)) //

				.next(new TestCase("Charging finished - permission gone") //

						.input("ess0", SOC, 81) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, null)) //

				.next(new TestCase("Mode changed to OFF") //
						.onAfterProcessImage(() -> {
							var config = cm.getConfiguration("ctrl0", "?");
							var mode = config.getProperties().get("mode");
							assertEquals(Mode.MANUAL_OFF.name(), mode);
						})) //
				.modified(this.baseConfig(Mode.MANUAL_OFF).build()).next(new TestCase("Permission changed to ") //
						.onAfterProcessImage(() -> {
							var config = cm.getConfiguration("ctrl0", "?");
							var permission = config.getProperties().get("ignoreSystemLimitsPermissionsOnce");
							assertFalse((boolean) permission);
						})) //
		;
	}

	@Test
	void testIgnoreSystemLimits() throws Exception {
		// E.g. for existing systems that have no valid grid limits configured.
		AtomicReference<DummyMeta> meta = new AtomicReference<>(new DummyMeta() //
				.withGridSellHardLimit(10_000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssChargeFromGridAllowed(false));
		final var ess = new DummyManagedAsymmetricEss("ess0");
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("meta", meta.get()) //
				.addReference("ess", ess) //
				.activate(this.baseConfig(Mode.MANUAL_ON) //
						.setConsiderSystemLimits(false) //
						.setPower(-100_000) //
						.build()) //
				.next(new TestCase("ChargeFromGrid not allowed") //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, -100_000) //
				) //
				.next(new TestCase("Charge (limited by 14a) once with permission") //
						.onBeforeProcessImage(() -> meta.set(meta.get() //
								.withGridBuyHardLimit(22000))) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, false) //
						.output(ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, false) //
						.output(ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, false) //
						.output(ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, -100_000)) //
				.deactivate();
	}

	@Test
	void testFallbackHandlerTriggersAfterTenMinutes() {
		var clock = TestUtils.createDummyClock();
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(0);
		var fallbackHandler = new FallbackHandler();

		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.CHARGE_ONCE, clock));

		clock.leap(9, ChronoUnit.MINUTES);
		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.CHARGE_ONCE, clock));

		clock.leap(2, ChronoUnit.MINUTES);
		assertTrue(fallbackHandler.isFallbackTimeoutReached(ess, Mode.CHARGE_ONCE, clock));
	}

	@Test
	void testFallbackHandlerResetsWhenAllowedPowerRecovers() {
		var clock = TestUtils.createDummyClock();
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedDischargePower(0);
		var fallbackHandler = new FallbackHandler();

		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.DISCHARGE_ONCE, clock));

		clock.leap(6, ChronoUnit.MINUTES);
		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.DISCHARGE_ONCE, clock));

		ess.withAllowedDischargePower(4000);
		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.DISCHARGE_ONCE, clock));

		ess.withAllowedDischargePower(0);
		assertFalse(fallbackHandler.isFallbackTimeoutReached(ess, Mode.DISCHARGE_ONCE, clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertTrue(fallbackHandler.isFallbackTimeoutReached(ess, Mode.DISCHARGE_ONCE, clock));
	}

	@Test
	void testGetAcPower() {
		var hybridEss = new DummyHybridEss("ess0") //
				.withActivePower(7000) //
				.withMaxApparentPower(10000) //
				.withAllowedChargePower(-5000) //
				.withAllowedDischargePower(5000) //
				.withDcDischargePower(3000); //

		assertEquals(Integer.valueOf(5000), //
				getAcPower(hybridEss, HybridEssMode.TARGET_AC, 5000));

		assertEquals(Integer.valueOf(9000), //
				getAcPower(hybridEss, HybridEssMode.TARGET_DC, 5000));
	}

	@Test
	void testTargetForModeOnceCharge() {
		var config = this.baseConfig(Mode.CHARGE_ONCE) //
				.setChargeOncePower(4000) //
				.setChargeOnceTargetSocEnable(true) //
				.setChargeOnceTargetSoc(90) //
				.build();

		var activeTarget = targetForModeOnce(config, 50);
		assertTrue(activeTarget.isPresent());
		assertEquals(Integer.valueOf(-4000), Integer.valueOf(activeTarget.get().power()));

		var stopTarget = targetForModeOnce(config, 90);
		assertFalse(stopTarget.isPresent());

		var fullBatteryStopTarget = targetForModeOnce(config, 100);
		assertFalse(fullBatteryStopTarget.isPresent());
	}

	@Test
	void testTargetForModeOnceDischarge() {
		var config = this.baseConfig(Mode.DISCHARGE_ONCE) //
				.setDischargeOncePower(1500) //
				.setDischargeOnceTargetSocEnable(true) //
				.setDischargeOnceTargetSoc(30) //
				.build();

		var activeTarget = targetForModeOnce(config, 50);
		assertTrue(activeTarget.isPresent());
		assertEquals(Integer.valueOf(1500), Integer.valueOf(activeTarget.get().power()));

		var stopTarget = targetForModeOnce(config, 30);
		assertFalse(stopTarget.isPresent());

		var emptyBatteryStopTarget = targetForModeOnce(config, 0);
		assertFalse(emptyBatteryStopTarget.isPresent());
	}

	@Test
	void testTargetForModeOnceDischargePowerIsForwarded() {
		var config = this.baseConfig(Mode.DISCHARGE_ONCE) //
				.setDischargeOncePower(5000) //
				.build();

		var activeTarget = targetForModeOnce(config, 50);
		assertTrue(activeTarget.isPresent());
		assertEquals(5000, activeTarget.get().power());
	}

	@Test
	void testLimitBySystemConstraintsDischarge() {
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedDischargePower(7000);
		var meta = new DummyMeta() //
				.withGridSellHardLimit(5000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssChargeFromGridAllowed(true) //
				.withIsEssDischargeToGridAllowed(true);
		var systemLimits = fromMeta(meta);

		var input = new ControllerEssFixActivePowerImpl.PowerTarget(ALL, Pwr.ACTIVE, Relationship.EQUALS,
				HybridEssMode.TARGET_AC, 8000);
		var output = SystemLimitHelper.clampToSystemLimits("ctrl0", input, ess, systemLimits, null);

		assertNotNull(output.powerTarget());
		assertEquals(withDefaultBuffer(5000), output.powerTarget().power());
		assertFalse(output.limitedByEssHardware());
		assertTrue(output.limitedByMetaLimit());
	}

	@Test
	void testLimitBySystemConstraintsCharge() {
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(-3000);
		var meta = new DummyMeta() //
				.withGridSellHardLimit(5000) //
				.withGridBuyHardLimit(10_000).withIsEssChargeFromGridAllowed(true);
		var systemLimits = fromMeta(meta);

		var input = new ControllerEssFixActivePowerImpl.PowerTarget(ALL, Pwr.ACTIVE, Relationship.EQUALS,
				HybridEssMode.TARGET_AC, -5000);
		var output = SystemLimitHelper.clampToSystemLimits("ctrl0", input, ess, systemLimits, null, false, true);

		assertNotNull(output.powerTarget());
		assertEquals(-5000, output.powerTarget().power());
		assertFalse(output.limitedByEssHardware());
		assertFalse(output.limitedByMetaLimit());
	}

	@Test
	void testLimitBySystemConstraintsChargeByGridBuyHardLimit() {
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(-8000);
		var meta = new DummyMeta() //
				.withGridBuyHardLimit(3464) //
				.withGridSellHardLimit(5000) //
				.withIsEssChargeFromGridAllowed(true);
		var systemLimits = fromMeta(meta);

		var input = new ControllerEssFixActivePowerImpl.PowerTarget(ALL, Pwr.ACTIVE, Relationship.EQUALS,
				HybridEssMode.TARGET_AC, -7000);
		var output = SystemLimitHelper.clampToSystemLimits("ctrl0", input, ess, systemLimits, null);

		assertNotNull(output.powerTarget());
		assertEquals(withDefaultBuffer(-3464), output.powerTarget().power());
		assertFalse(output.limitedByEssHardware());
		assertTrue(output.limitedByMetaLimit());
	}

	@Test
	void testLimitBySystemConstraintsDischargeTargetDcWithMetaLimit() {
		var ess = new DummyManagedSymmetricEss("ess0") //
				.withAllowedDischargePower(7000);
		var meta = new DummyMeta() //
				.withGridSellHardLimit(5000) //
				.withGridBuyHardLimit(10_000) //
				.withIsEssChargeFromGridAllowed(true) //
				.withIsEssDischargeToGridAllowed(true);
		var systemLimits = fromMeta(meta);

		var input = new ControllerEssFixActivePowerImpl.PowerTarget(ALL, Pwr.ACTIVE, Relationship.EQUALS,
				HybridEssMode.TARGET_DC, 8000);
		var output = SystemLimitHelper.clampToSystemLimits("ctrl0", input, ess, systemLimits, null);

		assertNotNull(output.powerTarget());
		assertEquals(withDefaultBuffer(5000), output.powerTarget().power());
		assertFalse(output.limitedByEssHardware()); // ESS hardware not checked in DC path yet
		assertTrue(output.limitedByMetaLimit());
	}

	private MyConfig.Builder baseConfig(Mode mode) {
		return MyConfig.create() //
				.setId("ctrl0") //
				.setEssId("ess0") //
				.setMode(mode) //
				.setHybridEssMode(HybridEssMode.TARGET_DC) //
				.setPower(1234) //
				.setPhase(ALL) //
				.setRelationship(EQUALS) //
				.setChargeOncePower(2000) //
				.setChargeOnceTargetSocEnable(false) //
				.setChargeOnceTargetSoc(90) //
				.setDischargeOncePower(2000) //
				.setDischargeOnceTargetSocEnable(false) //
				.setDischargeOnceTargetSoc(30) //
				.setConsiderSystemLimits(true) //
				.setIgnoreSystemLimitsPermissionsOnce(false); //
	}

	private static int withDefaultBuffer(int power) {
		return (int) (power * (1 - SystemLimitHelper.DEFAULT_GRID_BUFFER_FACTOR));
	}
}
