package io.openems.edge.controller.evse.cluster;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.cluster.Utils.calculate;
import static io.openems.edge.controller.evse.single.PhaseSwitching.DISABLE;
import static io.openems.edge.controller.evse.single.Types.Hysteresis.INACTIVE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.FORCE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.MINIMUM;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.SURPLUS;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.ZERO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.cluster.Utils.PowerDistribution;
import io.openems.edge.controller.evse.single.CombinedAbilities;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.test.DummyControllerEvseSingle;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public class UtilsTest {

	protected static class CalculateTester {

		public static CalculateTester generateControllers(int count) {
			return new CalculateTester(IntStream.range(0, count) //
					.<CtrlBuilder>mapToObj(i -> CtrlBuilder.create() //
							.setId("evse" + i) //
							.setActualMode(ZERO) //
							.setActivePower(0) //
							.setHysteresis(INACTIVE) //
							.setPhaseSwitching(DISABLE) //
							.setAppearsToBeFullyCharged(false) //
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

		private final List<CtrlBuilder> ctrls;
		private final DummySum sum = new DummySum();

		private CalculateTester(List<CtrlBuilder> ctrls) {
			this.ctrls = ctrls;
		}

		protected CalculateTester set(int a, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a }, callback);
		}

		protected CalculateTester set(int a, int b, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a, b }, callback);
		}

		protected CalculateTester set(int a, int b, int c, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a, b, c }, callback);
		}

		protected CalculateTester set(int[] indexes, Consumer<CtrlBuilder> callback) {
			for (var i : indexes) {
				callback.accept(this.ctrls.get(i));
			}
			return this;
		}

		protected CalculateTester setAll(Consumer<CtrlBuilder> callback) {
			for (var ctrl : this.ctrls) {
				callback.accept(ctrl);
			}
			return this;
		}

		protected CalculateTester sum(Consumer<DummySum> sum) {
			sum.accept(this.sum);
			return this;
		}

		protected PowerDistributionTester execute(DistributionStrategy distributionStrategy) {
			return new PowerDistributionTester(calculate(//
					distributionStrategy, //
					this.sum, //
					this.ctrls.stream() //
							.<ControllerEvseSingle>map(CtrlBuilder::build) //
							.toList(), //
					log -> doNothing()));
		}

		protected static record PowerDistributionTester(PowerDistribution powerDistribution) {
			protected static record EntryTester(PowerDistribution.Entry entry) {
				protected int getApplySetPointInMilliAmpere() {
					return ((ApplySetPoint.Action.MilliAmpere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInAmpere() {
					return ((ApplySetPoint.Action.Ampere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInWatt() {
					return ((ApplySetPoint.Action.Watt) this.entry.actions.build().applySetPoint()).value();
				}

				protected PhaseSwitch getPhaseSwitch() {
					return this.entry.actions.build().phaseSwitch();
				}
			}

			protected EntryTester get(int i) {
				return new EntryTester(this.powerDistribution.entries.get(i));
			}

			protected int[] getApplySetPoints() {
				return this.powerDistribution.entries.stream() //
						.mapToInt(e -> e.actions.build().applySetPoint().value()) //
						.toArray();
			}
		}
	}

	private static final class CtrlBuilder {
		private final ChargePointAbilities.Builder chargePointAbilities = ChargePointAbilities.create();
		private final ElectricVehicleAbilities.Builder electricVehicleAbilities = ElectricVehicleAbilities.create();

		private String id;
		private Mode.Actual actualMode;
		private Integer activePower;
		private Hysteresis hysteresis;
		private PhaseSwitching phaseSwitching;
		private boolean appearsToBeFullyCharged;
		private Consumer<CombinedAbilities.Builder> combinedAbilitiesCallback;

		public CtrlBuilder setId(String id) {
			this.id = id;
			return this;
		}

		public CtrlBuilder setActualMode(Mode.Actual actualMode) {
			this.actualMode = actualMode;
			return this;
		}

		public CtrlBuilder setActivePower(Integer activePower) {
			this.activePower = activePower;
			return this;
		}

		public CtrlBuilder setHysteresis(Hysteresis hysteresis) {
			this.hysteresis = hysteresis;
			return this;
		}

		public CtrlBuilder setPhaseSwitching(PhaseSwitching phaseSwitching) {
			this.phaseSwitching = phaseSwitching;
			return this;
		}

		public CtrlBuilder setAppearsToBeFullyCharged(boolean appearsToBeFullyCharged) {
			this.appearsToBeFullyCharged = appearsToBeFullyCharged;
			return this;
		}

		public CtrlBuilder setChargePointAbilities(Consumer<ChargePointAbilities.Builder> chargePointAbilities) {
			chargePointAbilities.accept(this.chargePointAbilities);
			return this;
		}

		public CtrlBuilder setElectricVehicleAbilities(
				Consumer<ElectricVehicleAbilities.Builder> electricVehicleAbilities) {
			electricVehicleAbilities.accept(this.electricVehicleAbilities);
			return this;
		}

		public CtrlBuilder setCombinedAbilities(Consumer<CombinedAbilities.Builder> combinedAbilitiesCallback) {
			this.combinedAbilitiesCallback = combinedAbilitiesCallback;
			return this;
		}

		public DummyControllerEvseSingle build() {
			var combinedAbilities = CombinedAbilities.createFrom(this.chargePointAbilities.build(),
					this.electricVehicleAbilities.build());
			if (this.combinedAbilitiesCallback != null) {
				this.combinedAbilitiesCallback.accept(combinedAbilities);
			}
			var params = new Params(this.actualMode, this.activePower, this.hysteresis, this.phaseSwitching,
					this.appearsToBeFullyCharged, combinedAbilities.build());
			return new DummyControllerEvseSingle(this.id) //
					.withParams(params);
		}

		public static CtrlBuilder create() {
			return new CtrlBuilder();
		}
	}

	@Test
	public void test1() {
		var sut = CalculateTester.generateControllers(5) //
				.set(1, 2, 3, c -> c //
						.setActualMode(SURPLUS)) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.sum(s -> s //
						.withGridActivePower(-32000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(0, sut.get(0).getApplySetPointInMilliAmpere());
		assertEquals(15, sut.get(1).getApplySetPointInAmpere());
		assertEquals(15916, sut.get(2).getApplySetPointInMilliAmpere());
		assertEquals(15458, sut.get(3).getApplySetPointInMilliAmpere());
		assertEquals(0, sut.get(4).getApplySetPointInMilliAmpere());

		assertArrayEquals(new int[] { 0, 15, 15916, 15458, 0 }, sut.getApplySetPoints());
	}

	@Test
	public void test2() {
		var sut = CalculateTester.generateControllers(5) //
				.set(0, 4, c -> c //
						.setActualMode(FORCE)) //
				.set(1, 2, 3, c -> c //
						.setActualMode(SURPLUS)) //
				.sum(s -> s //
						.withGridActivePower(-27000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 7130, 0, 0, 16000 }, sut.getApplySetPoints());
	}

	@Test
	public void test3() {
		var sut = CalculateTester.generateControllers(5) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.set(2, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)))) //
				.set(3, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1000, 5000)))) //
				.setAll(c -> c //
						.setActualMode(MINIMUM)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 6000, 6, 6000, 1380, 6000 }, sut.getApplySetPoints());
	}

	@Test
	public void test4() {
		var sut = CalculateTester.generateControllers(5) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.set(2, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)))) //
				.set(3, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1000, 5000)))) //
				.setAll(c -> c //
						.setActualMode(FORCE)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 16, 16000, 5000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	public void test5() {
		var sut = CalculateTester.generateControllers(5) //
				.set(0, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
								.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true))) //
				.setAll(c -> c //
						.setActualMode(FORCE) //
						.setPhaseSwitching(PhaseSwitching.FORCE_THREE_PHASE)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(PhaseSwitch.TO_THREE_PHASE, sut.get(0).getPhaseSwitch());
		assertNull(sut.get(1).getPhaseSwitch());
	}

	@Test
	public void test6() {
		var combinedAbilities = CombinedAbilities.createFrom(null, null).build();
		var params = new Params(Mode.Actual.FORCE, null, null, null, false, combinedAbilities);
		var ctrl = new DummyControllerEvseSingle("ctrl0") //
				.withParams(params);
		var sum = new DummySum();
		var powerDistribution = calculate(DistributionStrategy.EQUAL_POWER, sum, List.of(ctrl), log -> doNothing());
		assertEquals("PowerDistribution{totalActivePower=0, entries=\n" //
				+ "Entry{ctrl0, Params[actualMode=FORCE, activePower=null, hysteresis=null, phaseSwitching=null, appearsToBeFullyCharged=false, combinedAbilities=CombinedAbilities[" //
				+ "chargePointAbilities=null, electricVehicleAbilities=null, isReadyForCharging=false, applySetPoint=Watt" //
				+ "[phase=THREE_PHASE, min=0, max=0, step=1], phaseSwitch=null]], activePower=null, setPointInWatt=0, actions=UNDEFINED}}",
				powerDistribution.toString());
	}
}
