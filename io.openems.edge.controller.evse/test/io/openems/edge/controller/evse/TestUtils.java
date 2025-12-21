package io.openems.edge.controller.evse;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static java.util.Arrays.stream;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.evse.cluster.ControllerEvseClusterImpl;
import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.single.CombinedAbilities;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImpl;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.controller.evse.test.DummyControllerEvseSingle;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;
import io.openems.edge.evse.api.chargepoint.test.DummyElectricVehicle;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public class TestUtils {

	private TestUtils() {
	}

	public record ClusterSut(TimeLeapClock clock, ControllerTest test, ControllerEvseClusterImpl cluster,
			SingleSut... singles) {
	}

	/**
	 * Generates a {@link ClusterSut}.
	 * 
	 * @param clusterConfig a callback for Cluster-Config
	 * @param singleConfigs a callback for Single-Config
	 * @return the {@link ClusterSut}
	 * @throws Exception on error
	 */
	@SafeVarargs
	public static ClusterSut generateClusterSut(
			Consumer<io.openems.edge.controller.evse.cluster.MyConfig.Builder> clusterConfig,
			Consumer<io.openems.edge.controller.evse.single.MyConfig.Builder>... singleConfigs) throws Exception {

		final var clock = createDummyClock();
		final var singleConfigCounter = new AtomicInteger(0);
		final var singleSuts = stream(singleConfigs) //
				.map(config -> generateSingleSut(clock, singleConfigCounter.getAndIncrement(), config)) //
				.toArray(SingleSut[]::new);
		final var myConfig = io.openems.edge.controller.evse.cluster.MyConfig.create() //
				.setId("ctrlEvseCluster0") //
				.setDistributionStrategy(DistributionStrategy.EQUAL_POWER) //
				.setCtrlIds("ctrlEvseSingle0") //
				.setLogVerbosity(io.openems.edge.controller.evse.cluster.LogVerbosity.NONE);
		clusterConfig.accept(myConfig);

		final var ctrlCluster = new ControllerEvseClusterImpl();
		final var test = new ControllerTest(ctrlCluster) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ctrls", stream(singleSuts) //
						.map(SingleSut::ctrlSingle) //
						.toList()); //
		stream(singleSuts).map(SingleSut::chargePoint) //
				.forEach(cp -> test.addComponent(cp));
		stream(singleSuts).map(SingleSut::electricVehicle) //
				.forEach(ev -> test.addComponent(ev));
		test.activate(myConfig.build());

		return new ClusterSut(clock, test, ctrlCluster, singleSuts);
	}

	public record SingleSut(ControllerTest test, ControllerEvseSingleImpl ctrlSingle, DummyEvseChargePoint chargePoint,
			DummyElectricVehicle electricVehicle) {
	}

	/**
	 * Generates a {@link SingleSut}.
	 * 
	 * @param config a MyConfig callback
	 * @return {@link SingleSut}
	 * @throws OpenemsRuntimeException on error
	 */
	public static SingleSut generateSingleSut(Consumer<io.openems.edge.controller.evse.single.MyConfig.Builder> config)
			throws OpenemsRuntimeException {
		return generateSingleSut(createDummyClock(), 0, config);
	}

	/**
	 * Generates a {@link SingleSut}.
	 * 
	 * @param clock  the {@link Clock}
	 * @param count  sets the Component-ID
	 * @param config a MyConfig callback
	 * @return {@link SingleSut}
	 * @throws OpenemsRuntimeException on error
	 */
	public static SingleSut generateSingleSut(Clock clock, int count,
			Consumer<io.openems.edge.controller.evse.single.MyConfig.Builder> config) throws OpenemsRuntimeException {
		final var ctrlSingle = new ControllerEvseSingleImpl();
		final var chargePoint = new DummyEvseChargePoint("chargePoint0");
		final var electricVehicle = new DummyElectricVehicle("electricVehicle0");
		final var myConfig = io.openems.edge.controller.evse.single.MyConfig.create() //
				.setId("ctrlEvseSingle" + count) //
				.setMode(Mode.MINIMUM) //
				.setChargePointId("chargePoint0") //
				.setElectricVehicleId("electricVehicle0") //
				.setPhaseSwitching(PhaseSwitching.DISABLE) //
				.setOneShot("") //
				.setJsCalendar("[]") //
				.setManualEnergySessionLimit(10_000) //
				.setLogVerbosity(LogVerbosity.NONE);
		config.accept(myConfig);

		try {
			final var test = new ControllerTest(ctrlSingle) //
					.addReference("componentManager", new DummyComponentManager(clock)) //
					.addReference("cm", new DummyConfigurationAdmin()) //
					.addReference("chargePoint", chargePoint) //
					.addReference("electricVehicle", electricVehicle) //
					.activate(myConfig.build());
			return new SingleSut(test, ctrlSingle, chargePoint, electricVehicle);
		} catch (Exception e) {
			throw new OpenemsRuntimeException(e);
		}
	}

	public static final class CtrlBuilder {
		private final ChargePointAbilities.Builder chargePointAbilities = ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
				.setIsReadyForCharging(true);
		private final ElectricVehicleAbilities.Builder electricVehicleAbilities = ElectricVehicleAbilities.create() //
				.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
				.setThreePhaseLimitInMilliAmpere(6000, 16000); //

		private String id = "ctrlEvseSingle0";
		private Mode mode = Mode.ZERO;
		private Integer activePower = null;
		private int sessionEnergy = 0;
		private int sessionEnergyLimit = 0;
		private History history = new History();
		private PhaseSwitching phaseSwitching = PhaseSwitching.DISABLE;
		private Consumer<CombinedAbilities.Builder> combinedAbilitiesCallback;
		private JSCalendar.Tasks<Payload> smartConfig = JSCalendar.Tasks.empty();

		public CtrlBuilder setId(String id) {
			this.id = id;
			return this;
		}

		public CtrlBuilder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public CtrlBuilder setActivePower(int activePower) {
			this.activePower = activePower;
			return this;
		}

		public CtrlBuilder setSessionEnergy(int sessionEnergy) {
			this.sessionEnergy = sessionEnergy;
			return this;
		}

		public CtrlBuilder setSessionEnergyLimit(int sessionEnergyLimit) {
			this.sessionEnergyLimit = sessionEnergyLimit;
			return this;
		}

		public CtrlBuilder setHistory(History history) {
			this.history = history;
			return this;
		}

		public CtrlBuilder setPhaseSwitching(PhaseSwitching phaseSwitching) {
			this.phaseSwitching = phaseSwitching;
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

		public CtrlBuilder setSmartConfig(JSCalendar.Tasks<Payload> smartConfig) {
			this.smartConfig = smartConfig;
			return this;
		}

		public DummyControllerEvseSingle build() {
			var combinedAbilities = CombinedAbilities.createFrom(this.chargePointAbilities.build(),
					this.electricVehicleAbilities.build());
			if (this.combinedAbilitiesCallback != null) {
				this.combinedAbilitiesCallback.accept(combinedAbilities);
			}
			var params = new Params(this.id, this.mode, this.activePower, this.sessionEnergy, this.sessionEnergyLimit,
					this.history, this.phaseSwitching, combinedAbilities.build(), this.smartConfig);
			return new DummyControllerEvseSingle(this.id) //
					.withParams(params);
		}
	}

	/**
	 * Generates a {@link DummyControllerEvseSingle} via {@link CtrlBuilder}.
	 * 
	 * @return a new {@link CtrlBuilder}
	 */
	public static CtrlBuilder createSingleCtrl() {
		return new CtrlBuilder();
	}
}
