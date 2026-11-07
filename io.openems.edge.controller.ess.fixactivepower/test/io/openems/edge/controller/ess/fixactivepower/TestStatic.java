package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.calculateAcMaximum;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.calculateAcMinimum;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.clampToSystemLimits;
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

	private static final String DEFAULT_CONTROLLER_ID = "ctrlFixActivePower0";

	private DummyMeta meta;
	private DummyManagedSymmetricEss ess;

	@BeforeEach
	void setup() {
		this.ess = new DummyManagedSymmetricEss("ess0");
		this.meta = new DummyMeta()//
				.withGridLimit(l -> l //
						.setGridBuyHardLimit(30000) //
						.setGridSellHardLimit(22000) //
						.setFuseCurrent(110) //
						.setEssChargeFromGridAllowed(true) //
						.setEssDischargeToGridAllowed(true));
	}

	@Test
	void testLimitBySystemConstraints_NotAllowed() {
		this.meta.withGridLimit(l -> l //
				.setEssChargeFromGridAllowed(false) //
				.setEssDischargeToGridAllowed(false));
		Integer gridActivePower = 10000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-5000);

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		assertNull(result.powerTarget());
		assertTrue(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_LimitedByEss() {
		Integer gridActivePower = 10000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-10000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		assertEquals(-8000, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertTrue(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_LimitedByMeta() {
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(4200));
		Integer gridActivePower = 1000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-10000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		// Grid limit of 4200 minus grid
		assertEquals(-4200 + 1000, result.powerTarget().power());
		assertTrue(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());

		this.ess.setPower(new DummyPower(2500));
		result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		assertEquals(-2500, result.powerTarget().power()); // Grid limit but ess limit more strict
		assertFalse(result.limitedByMetaLimit());
		assertTrue(result.limitedByEssHardware());
	}

	@Test
	void testLimitBySystemConstraints_NotLimited() {
		Integer gridActivePower = 1000;
		var powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(-4000);

		this.ess.setPower(new DummyPower(8000));

		var result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		assertEquals(-4000, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());

		powerTarget = ControllerEssFixActivePowerImpl.PowerTarget.fromDcPowerWithDefaults(0);
		result = clampToSystemLimits(DEFAULT_CONTROLLER_ID, powerTarget, this.ess, this.meta.getGridLimit(),
				gridActivePower);

		assertEquals(0, result.powerTarget().power());
		assertFalse(result.limitedByMetaLimit());
		assertFalse(result.limitedByEssHardware());
	}

	@Test
	void testCalculateAcMinimumWithoutGrid() {
		// Without gridActivePower, minimum is limited only by
		// gridBuyHardLimitWithBuffer
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(5000));
		int essMinPower = calculateAcMinimum(this.meta.getGridLimit(), null, 0);

		// Ess Allowed Charge Power is 5000 -> MinValue: -5000
		assertEquals(-5000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithGridBuy() {
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(3000));
		int essMinPower = calculateAcMinimum(this.meta.getGridLimit(), 1000, 0);

		assertEquals(-2000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithGridSell() {
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(4000));
		int essMinPower = calculateAcMinimum(this.meta.getGridLimit(), -2000, 0);

		assertEquals(-6000, essMinPower);
	}

	@Test
	void testCalculateAcMinimumWithEssDischarging() {
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(10000));
		int acMin = calculateAcMinimum(this.meta.getGridLimit(), 2000, 3000);

		// Without essActivePower the realGridWould be 5000, so only 5000 for ess charge
		// left.
		assertEquals(-5000, acMin);
	}

	@Test
	void testCalculateAcMinimumWithEssCharging() {
		this.meta.withGridLimit(l -> l //
				.setGridBuyHardLimitWithBuffer(10000));
		int acMin = calculateAcMinimum(this.meta.getGridLimit(), 2000, -500);

		assertEquals(-8500, acMin);
	}

	@Test
	void testCalculateAcMaximumWithoutGrid() {
		// Without gridActivePower, maximum is limited only by
		// gridSellHardLimitWithBuffer
		this.meta.withGridLimit(l -> l //
				.setGridSellHardLimitWithBuffer(5000));
		int essMaxPower = calculateAcMaximum(this.meta.getGridLimit(), null, 0);

		assertEquals(5000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithGridBuy() {
		this.meta.withGridLimit(l -> l //
				.setGridSellHardLimitWithBuffer(3000));
		int essMaxPower = calculateAcMaximum(this.meta.getGridLimit(), 1000, 0);

		assertEquals(4000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithGridSell() {
		this.meta.withGridLimit(l -> l //
				.setGridSellHardLimitWithBuffer(4000));
		int essMaxPower = calculateAcMaximum(this.meta.getGridLimit(), -2000, 0);

		assertEquals(2000, essMaxPower);
	}

	@Test
	void testCalculateAcMaximumWithEssDischarging() {
		this.meta.withGridLimit(l -> l //
				.setGridSellHardLimitWithBuffer(10000));
		int acMax = calculateAcMaximum(this.meta.getGridLimit(), 2000, 3000);

		assertEquals(15000, acMax);
	}

	@Test
	void testCalculateAcMaximumWithEssCharging() {
		this.meta.withGridLimit(l -> l //
				.setGridSellHardLimitWithBuffer(10000));
		int acMax = calculateAcMaximum(this.meta.getGridLimit(), 2000, -500);

		assertEquals(11500, acMax);
	}
}
