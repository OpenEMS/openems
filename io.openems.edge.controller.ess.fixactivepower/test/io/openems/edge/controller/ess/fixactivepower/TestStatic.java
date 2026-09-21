package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.calculateAcMaximum;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.calculateAcMinimum;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.clampToSystemLimits;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.SystemLimits.fromMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

class TestStatic {

	private static final String DEFAULT_ESS_ID = "ess0";
	private static final String DEFAULT_CONTROLLER_ID = "ctrlFixActivePower0";
	private static final float TEST_BUFFER = 0f;

	private DummyMeta meta;
	private DummyManagedSymmetricEss ess;

	@BeforeEach
	void setup() {
		this.ess = new DummyManagedSymmetricEss(DEFAULT_ESS_ID);
		this.meta = new DummyMeta()//
				.withGridBuyHardLimit(22000)//
				.withGridSellHardLimit(22000)//
				.withIsEssChargeFromGridAllowed(true)//
				.withIsEssDischargeToGridAllowed(true);
	}

	@Test
	void testLimitBySystemConstraints_NotAllowed() {

		this.meta = this.meta //
				.withIsEssChargeFromGridAllowed(false) //
				.withIsEssDischargeToGridAllowed(false);

		var systemLimits = fromMeta(this.meta);
		Integer gridActivePower = 10000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-5000);

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits, gridActivePower);

		assertNull(result.powerTarget());
		assertTrue(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_LimitedByEss() {

		var systemLimits = fromMeta(this.meta);
		Integer gridActivePower = 10000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-10000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits, gridActivePower);

		assertEquals(-8000, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertTrue(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_LimitedByMeta() {

		this.meta = this.meta //
				.withGridBuyHardLimit(4200);// e.g. §14a EnWG limit in future

		var systemLimits = fromMeta(this.meta);
		Integer gridActivePower = 1000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-10000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits, gridActivePower);

		// Grid limit of 4200 minus grid
		assertEquals(withDefaultBuffer(-4200) + 1000, result.powerTarget().power());
		assertTrue(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());

		this.ess.setPower(new DummyPower(2500));
		result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits, gridActivePower);

		assertEquals(-2500, result.powerTarget().power()); // Grid limit but ess limit more strict
		assertFalse(result.limitedByMetaLimit());
		assertTrue(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_NotLimited() {

		var systemLimits = fromMeta(this.meta);
		Integer gridActivePower = 1000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-4000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits, gridActivePower);

		assertEquals(-4000, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());

		powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(0);
		result = SystemLimitHelper.clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, systemLimits,
				gridActivePower);

		assertEquals(0, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());
	}

	@Test
	void testCalculateAcMinimumWithoutGrid() {
		// Without gridActivePower, minimum is limited only by gridBuyHardLimit
		var systemLimits = fromMeta(new DummyMeta().withGridBuyHardLimit(5000));
		int essMinPower = calculateAcMinimum(systemLimits, null, 0, TEST_BUFFER);

		// Ess Allowed Charge Power is 5000 -> MinValue: -5000
		assertEquals(-5000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithGridBuy() {
		var systemLimits = fromMeta(new DummyMeta().withGridBuyHardLimit(3000));
		int essMinPower = calculateAcMinimum(systemLimits, 1000, 0, TEST_BUFFER);

		assertEquals(-2000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithGridSell() {
		var systemLimits = fromMeta(new DummyMeta().withGridBuyHardLimit(4000));
		int essMinPower = calculateAcMinimum(systemLimits, -2000, 0, TEST_BUFFER);

		assertEquals(-6000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithEssDischarging() {
		var systemLimits = fromMeta(new DummyMeta().withGridBuyHardLimit(10000));
		int acMin = calculateAcMinimum(systemLimits, 2000, 3000, TEST_BUFFER);

		// Without essActivePower the realGridWould be 5000, so only 5000 for ess charge
		// left.
		assertEquals(-5000, acMin);
	}

	@Test
	void testCalculateAcMinimumWithEssCharging() {
		var systemLimits = fromMeta(new DummyMeta().withGridBuyHardLimit(10000));
		int acMin = calculateAcMinimum(systemLimits, 2000, -500, TEST_BUFFER);

		assertEquals(-8500, acMin);
	}

	@Test
	void testCalculateAcMaximumWithoutGrid() {
		// Without gridActivePower, maximum is limited only by gridSellHardLimit
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(5000));
		int essMaxPower = calculateAcMaximum(systemLimits, null, 0, TEST_BUFFER);

		assertEquals(5000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithGridBuy() {
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(3000));
		int essMaxPower = calculateAcMaximum(systemLimits, 1000, 0, TEST_BUFFER);

		assertEquals(4000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithGridSell() {
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(4000));
		int essMaxPower = calculateAcMaximum(systemLimits, -2000, 0, TEST_BUFFER);

		assertEquals(2000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithEssDischarging() {
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(10000));
		int acMax = calculateAcMaximum(systemLimits, 2000, 3000, TEST_BUFFER);

		assertEquals(15000, acMax);
	}

	@Test
	void testCalculateAcMaximumWithEssCharging() {
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(10000));
		int acMax = calculateAcMaximum(systemLimits, 2000, -500, TEST_BUFFER);

		assertEquals(11500, acMax);
	}

	@Test
	void testCalculateAcMaximumWithEssChargingWithBuffer() {
		var systemLimits = fromMeta(new DummyMeta().withGridSellHardLimit(10000).withGridBuyHardLimit(10000));
		int acMax = calculateAcMaximum(systemLimits, 2000, -500, SystemLimitHelper.DEFAULT_GRID_BUFFER_FACTOR);

		assertEquals(withDefaultBuffer(10000) - 500 + 2000, acMax);
	}

	public static int withDefaultBuffer(int power) {
		return (int) (power * (1 - SystemLimitHelper.DEFAULT_GRID_BUFFER_FACTOR));
	}
}
