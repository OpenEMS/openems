package io.openems.edge.controller.evse.cluster.powerdistribute;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.single.Mode.ZERO;
import static io.openems.edge.controller.evse.single.PhaseSwitching.DISABLE;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.TestUtils;
import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.cluster.LogVerbosity;
import io.openems.edge.controller.evse.cluster.powerdistribute.ramp.PowerDistributionRamp;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Types;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;

class CalculateTester {

	public static CalculateTester generateControllers(int count) {
		final var clock = createDummyClock();

		// Add History with high value to tick Utils::applyChangeLimit.
		final var history = new Types.History();
		history.addEntry(Instant.now(clock), null, 22000 /* [W] */, 22000, false);
		clock.leap(500, ChronoUnit.MILLIS);

		return new CalculateTester(clock, IntStream.range(0, count) //
		.<TestUtils.CtrlBuilder>mapToObj(i -> TestUtils.createSingleCtrl() //
				.setCtrlSingleId("evse" + i) //
				.setChargePointId("evseChargePoint" + i) //
				.setMode(ZERO) //
				.setActivePower(0) //
				.setHistory(history) //
				.setPhaseSwitching(DISABLE) //
				.setChargePointAbilities(a -> a //
						.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
						.setIsReadyForCharging(true)) //
				.setElectricVehicleAbilities(a -> a //
						.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
						.setThreePhaseLimitInMilliAmpere(6000, 16000)) //
				.setCombinedAbilities(a -> a //
						.setIsReadyForCharging(true)))
				.toList());
	}

	public final TimeLeapClock clock;

	private final List<TestUtils.CtrlBuilder> ctrls;
	private final DummySum sum = new DummySum();

	protected PowerDistributionRamp ramp;

	private CalculateTester(TimeLeapClock clock, List<TestUtils.CtrlBuilder> ctrls) {
		this.clock = clock;
		this.ctrls = ctrls;
	}

	protected CalculateTester set(int a, Consumer<TestUtils.CtrlBuilder> callback) {
		return this.set(new int[] { a }, callback);
	}

	protected CalculateTester set(int a, int b, Consumer<TestUtils.CtrlBuilder> callback) {
		return this.set(new int[] { a, b }, callback);
	}

	protected CalculateTester set(int a, int b, int c, Consumer<TestUtils.CtrlBuilder> callback) {
		return this.set(new int[] { a, b, c }, callback);
	}

	protected CalculateTester set(int[] indexes, Consumer<TestUtils.CtrlBuilder> callback) {
		for (var i : indexes) {
			callback.accept(this.ctrls.get(i));
		}
		return this;
	}

	protected CalculateTester setAll(Consumer<TestUtils.CtrlBuilder> callback) {
		for (var ctrl : this.ctrls) {
			callback.accept(ctrl);
		}
		return this;
	}

	protected CalculateTester sum(Consumer<DummySum> sum) {
		sum.accept(this.sum);
		return this;
	}

	protected CalculateTester setRamp(PowerDistributionRamp ramp) {
		this.ramp = ramp;
		return this;
	}

	protected PowerDistributionTester execute(DistributionStrategy distributionStrategy) {
		var ctrls = this.ctrls.stream().map(x -> (ControllerEvseSingle) x.build()).toList();
		final var params = ctrls.stream().map(x -> ((ControllerEvseSingle) x).getParams()).toList();

		var distributor = new EvsePowerDistributionManager(params, distributionStrategy, null, this.ramp,
				LogVerbosity.TRACE, this.sum, this.clock);
		var result = distributor.run(ctrls);

		return new PowerDistributionTester(result);
	}

	protected static record PowerDistributionTester(
			List<EvsePowerDistributionManager.CalculationResult> calculationResults) {
		protected static record EntryTester(EvsePowerDistributionManager.CalculationResult entry) {
			protected int getApplySetPointInMilliAmpere() {
				return ((ApplySetPoint.Action.MilliAmpere) this.entry.actions().applySetPoint()).value();
			}

			protected int getApplySetPointInAmpere() {
				return ((ApplySetPoint.Action.Ampere) this.entry.actions().applySetPoint()).value();
			}

			protected int getApplySetPointInWatt() {
				return ((ApplySetPoint.Action.Watt) this.entry.actions().applySetPoint()).value();
			}

			protected ApplyPhaseSwitch.PhaseSwitchDirection getPhaseSwitchDirection() {
				final var phaseSwitch = this.entry.actions().phaseSwitch();
				if (phaseSwitch == null) {
					return null;
				}

				return phaseSwitch.direction();
			}
		}

		protected EntryTester get(int i) {
			return new EntryTester(this.calculationResults.get(i));
		}

		protected int[] getApplySetPoints() {
			return this.calculationResults.stream() //
					.mapToInt(r -> r.actions().applySetPoint().value()) //
					.toArray();
		}
	}
}