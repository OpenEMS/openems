package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

/**
 * Tests setActivePower() through public wrappers.
 */
public class ManagedSymmetricEssSetActivePowerTest {

	private DummyManagedSymmetricEss ess;

	@Before
	public void before() {
		this.ess = new DummyManagedSymmetricEss("ess0") //
				.withMaxApparentPower(10_000) //
				.withAllowedChargePower(-10_000) //
				.withAllowedDischargePower(10_000);
	}

	@Test
	public void testWithoutFilter_ValueNull() throws OpenemsNamedException {
		this.ess.setPower(new DummyPower(10_000));

		this.ess.setActivePowerEqualsWithoutFilter(null);

		assertFalse(this.ess.getSetActivePowerEqualsChannel().getNextWriteValue().isPresent());
	}

	@Test
	public void testWithFilter_ValueNull() throws OpenemsNamedException {
		this.ess.setPower(new DummyPower(10_000, 0.3, 0.3, 0.1));

		this.ess.setActivePowerEqualsWithFilter(null);

		assertFalse(this.ess.getSetActivePowerEqualsChannel().getNextWriteValue().isPresent());
	}

	@Test
	public void testWithFilter_MaxPower_Less_MinPower() throws OpenemsNamedException {
		var filter = new SpyPidFilter(0.3, 0.3, 0.1);
		var power = new DummyPower(10_000, filter) {
			@Override
			public int getMinPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
				return 1_005;
			}

			@Override
			public int getMaxPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
				return 1_000;
			}
		};
		this.ess.setPower(power);
		this.ess._setActivePower(1_000);

		this.ess.setActivePowerEqualsWithFilter(1_003);

		// Early return branch: no filter configuration, no PID call, no write.
		assertFalse(filter.wasSetLimitsCalled());
		assertFalse(filter.wasApplyCalled());
		assertFalse(this.ess.getSetActivePowerEqualsChannel().getNextWriteValue().isPresent());
	}

	@Test
	public void testWithoutFilter() throws OpenemsNamedException {
		var filter = new SpyPidFilter(0.3, 0.3, 0.1);
		this.ess.setPower(new DummyPower(10_000, filter));

		this.ess.setActivePowerEqualsWithoutFilter(2_500);

		assertTrue(filter.wasResetCalled());
		assertEquals(Integer.valueOf(2_500),
				this.ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset().orElseThrow());
	}

	@Test
	public void testWithFilter_FilterIsNull() throws OpenemsNamedException {
		this.ess.setPower(new DummyPower(10_000)); // no PID filter

		this.ess.setActivePowerEqualsWithFilter(1_700);

		assertEquals(Integer.valueOf(1_700),
				this.ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset().orElseThrow());
	}

	@Test
	public void testWithFilter_Filter_Overflow_Proof() throws OpenemsNamedException {
		var filter = new SpyPidFilter(0.3, 0.3, 0.1);
		var power = new DummyPower(10_000, filter) {
			@Override
			public int getMinPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
				return 1_000;
			}

			@Override
			public int getMaxPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
				return 1_005; // difference is 5 < 10 -> must hit early return branch
			}
		};
		this.ess.setPower(power);
		this.ess._setActivePower(1_000);

		this.ess.setActivePowerEqualsWithFilter(1_003);

		// Early return branch: no filter configuration, no PID call, no write.
		assertFalse(filter.wasSetLimitsCalled());
		assertFalse(filter.wasApplyCalled());
		assertFalse(this.ess.getSetActivePowerEqualsChannel().getNextWriteValue().isPresent());
	}

	@Test
	public void testWithFilter_PidCheck() throws OpenemsNamedException {
		var filter = new SpyPidFilter(0.3, 0.3, 0.1);
		this.ess.setPower(new DummyPower(10_000, filter));

		// cycle 1
		this.ess._setActivePower(0);
		this.ess.setActivePowerEqualsWithFilter(5_000);
		var first = this.ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset().orElseThrow();

		// cycle 2 (simulate ESS moved to previous setpoint)
		this.ess._setActivePower(first);
		this.ess.setActivePowerEqualsWithFilter(5_000);
		var second = this.ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset().orElseThrow();

		assertTrue(filter.wasApplyCalled());
		assertTrue(first > 0);
		assertTrue(second > first);
		assertTrue(second <= 10_000);
	}

	private static class SpyPidFilter extends PidFilter {
		private boolean resetCalled = false;
		private boolean applyCalled = false;
		private boolean setLimitsCalled = false;

		private SpyPidFilter(double p, double i, double d) {
			super(p, i, d);
		}

		@Override
		public void reset() {
			this.resetCalled = true;
			super.reset();
		}

		@Override
		public int applyPidFilter(int input, int target) {
			this.applyCalled = true;
			return super.applyPidFilter(input, target);
		}

		@Override
		public void setLimits(Integer lowLimit, Integer highLimit) {
			this.setLimitsCalled = true;
			super.setLimits(lowLimit, highLimit);
		}

		private boolean wasResetCalled() {
			return this.resetCalled;
		}

		private boolean wasApplyCalled() {
			return this.applyCalled;
		}

		private boolean wasSetLimitsCalled() {
			return this.setLimitsCalled;
		}
	}
}
