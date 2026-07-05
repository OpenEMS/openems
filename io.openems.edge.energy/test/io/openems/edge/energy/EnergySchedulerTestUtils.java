package io.openems.edge.energy;

import static io.openems.common.test.TestUtils.createDummyClock;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.ess.fixactivepower.EnergyScheduler;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.heat.askoma.HeatAskomaImpl;

public class EnergySchedulerTestUtils {

	private EnergySchedulerTestUtils() {
	}

	private static final Clock CLOCK = createDummyClock();

	public static enum Controller {
		ESS_EMERGENCY_CAPACITY_RESERVE("Controller.Ess.EmergencyCapacityReserve", //
				new Factory<>(//
						io.openems.edge.controller.ess.emergencycapacityreserve.EnergyScheduler::buildEnergyScheduleHandler,
						io.openems.edge.controller.ess.emergencycapacityreserve.EnergyScheduler.Config.serializer())),
		ESS_LIMIT_TOTAL_DISCHARGE("Controller.Ess.LimitTotalDischarge", //
				new Factory<>(//
						io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler::buildEnergyScheduleHandler,
						io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler.Config.serializer())),
		ESS_FIX_ACTIVE_POWER("Controller.Ess.FixActivePower", //
				new Factory<>(//
						EnergyScheduler::buildEnergyScheduleHandler,
						io.openems.edge.controller.ess.fixactivepower.EnergyScheduler.Config.serializer())),
		ESS_TIME_OF_USE_TARIFF("Controller.Ess.Time-Of-Use-Tariff", //
				new Factory<>(//
						io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler::buildEnergyScheduleHandler,
						io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.Config.serializer())),
		EVSE_CLUSTER("Evse.Controller.Cluster", //
				new Factory<>(//
						(comp, conf) -> io.openems.edge.controller.evse.cluster.EnergyScheduler
								.buildEnergyScheduleHandler(comp, () -> CLOCK, conf),
						io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterEshConfig.serializer(CLOCK))),
		HEAT_ASKOMA(HeatAskomaImpl.FACTORY_ID, //
				new Factory<>(//
						(comp, conf) -> io.openems.edge.heat.askoma.EnergyScheduler.buildEnergyScheduleHandler(comp,
								() -> CLOCK, conf),
						io.openems.edge.heat.askoma.EnergyScheduler.Config.serializer(CLOCK))),
		BRAIINS_SINGLE("Controller.BraiinsOS.Single", //
				new Factory<>(//
						(comp, conf) -> io.openems.edge.braiinsos.EnergyScheduler.buildEnergyScheduleHandler(comp,
								() -> CLOCK, conf),
						io.openems.edge.braiinsos.EnergyScheduler.Config.serializer(CLOCK)));

		public final String factoryPid;
		public final Factory<?> factory;

		private Controller(String factoryPid, Factory<?> factory) {
			this.factoryPid = factoryPid;
			this.factory = factory;
		}

		/**
		 * Gets the {@link Controller} enum for the given Factory-PID.
		 * 
		 * @param factoryPid the Factory-PID
		 * @return the {@link Controller}
		 */
		public static Controller fromFactoryPid(String factoryPid) {
			return Stream.of(Controller.values()) //
					.filter(c -> c.factoryPid.equals(factoryPid)) //
					.findFirst() //
					.orElseThrow(() -> new IllegalArgumentException(
							"DummyEnergySchedulable for Factory-PID [" + factoryPid + "] is not implemented"));
		}
	}

	private record Factory<CONFIG>(
			BiFunction<OpenemsComponent, Supplier<CONFIG>, ? extends EnergyScheduleHandler> factory,
			JsonSerializer<CONFIG> serializer) {

		@SuppressWarnings("unchecked")
		public <ESH> Function<OpenemsComponent, ESH> getEshFactory(JsonElement source) {
			return parent -> (ESH) this.factory.apply(parent, //
					() -> source == null || source.isJsonNull() //
							? null //
							: this.serializer.deserialize(source));
		}
	}

	/**
	 * Creates a {@link DummyEnergySchedulable} from a source {@link JsonObject}.
	 * 
	 * @param parentFactoryPid  the Factory-PID
	 * @param parentComponentId the Component-ID
	 * @param source            the source {@link JsonElement}
	 * @return a new {@link DummyEnergySchedulable}
	 * @throws IllegalArgumentException on error
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> createFromJson(String parentFactoryPid,
			String parentComponentId, JsonElement source) throws IllegalArgumentException {
		try {
			return createFromJson(Controller.fromFactoryPid(parentFactoryPid), parentComponentId, source);
		} catch (OpenemsNamedException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a {@link DummyEnergySchedulable} from a log message.
	 * 
	 * @param controller        the {@link Controller}
	 * @param parentComponentId the Component-ID
	 * @param source            the source {@link JsonElement}
	 * @return a new {@link DummyEnergySchedulable}
	 * @throws OpenemsNamedException on error
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> createFromJson(Controller controller,
			String parentComponentId, JsonElement source) throws OpenemsNamedException {
		return new DummyEnergySchedulable<>(controller.factoryPid, parentComponentId,
				controller.factory.getEshFactory(source));
	}

	/**
	 * Creates a {@link DummyEnergySchedulable} for a given {@link Controller}.
	 * 
	 * @param controller  the {@link Controller}
	 * @param componentId the Component-ID
	 * @param eshFactory  factory for a {@link EnergyScheduleHandler}
	 * @return a new {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> create(Controller controller,
			String componentId, Function<OpenemsComponent, ? extends EnergyScheduleHandler> eshFactory) {
		return new DummyEnergySchedulable<>(controller.factoryPid, componentId, eshFactory);
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of
	 * Controller.Ess.EmergencyCapacityReserve.
	 * 
	 * @param componentId the Component-ID
	 * @param reserveSoc  the configured Reserve-Soc
	 * @return the {@link DummyEnergySchedulable}
	 */
	protected static DummyEnergySchedulable<? extends EnergyScheduleHandler> dummyEssEmergencyCapacityReserve(
			String componentId, int reserveSoc) {
		return create(Controller.ESS_EMERGENCY_CAPACITY_RESERVE, componentId,
				cmp -> io.openems.edge.controller.ess.emergencycapacityreserve. //
						EnergyScheduler.buildEnergyScheduleHandler(cmp,
								() -> new io.openems.edge.controller.ess.emergencycapacityreserve. //
										EnergyScheduler.Config(reserveSoc)));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of
	 * Controller.Ess.LimitTotalDischarge.
	 * 
	 * @param componentId the Component-ID
	 * @param minSoc      the configured Min-Soc
	 * @return the {@link DummyEnergySchedulable}
	 */
	protected static DummyEnergySchedulable<? extends EnergyScheduleHandler> dummyEssLimitTotalDischarge(
			String componentId, int minSoc) {
		return create(Controller.ESS_LIMIT_TOTAL_DISCHARGE, componentId,
				cmp -> io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler
						.buildEnergyScheduleHandler(cmp, () -> new io.openems.edge.controller.ess.limittotaldischarge. //
								EnergyScheduler.Config(minSoc)));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.FixActivePower.
	 * 
	 * @param componentId the Component-ID
	 * @param mode        the
	 *                    {@link io.openems.edge.controller.ess.fixactivepower.enums.Mode}
	 * @param power       the configured power [W]
	 * @param targetSoc   the target state of charge (SoC) for the modes
	 *                    {@link io.openems.edge.controller.ess.fixactivepower.enums.Mode#CHARGE_ONCE}
	 *                    and
	 *                    {@link io.openems.edge.controller.ess.fixactivepower.enums.Mode#DISCHARGE_ONCE}
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> dummyEssFixActivePower(//
			String componentId, io.openems.edge.controller.ess.fixactivepower.enums.Mode mode, int power,
			Integer targetSoc) {
		return create(Controller.ESS_FIX_ACTIVE_POWER, componentId,
				cmp -> io.openems.edge.controller.ess.fixactivepower.EnergyScheduler //
						.buildEnergyScheduleHandler(cmp, () -> new io.openems.edge.controller.ess.fixactivepower. //
								EnergyScheduler.Config(mode, power, targetSoc)));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Controller.Ess.Time-Of-Use-Tariff.
	 *
	 * @param componentId      the Component-ID
	 * @param activeModes      the active {@link StateMachine} modes
	 * @param targetSocBuffer  the target SoC buffer for
	 *                         {@link io.openems.edge.controller.ess.gridoptimizedcharge.Mode#AUTOMATIC}
	 *                         mode (used by GridOptimizedCharge)
	 * @param manualTargetTime the manual target time for
	 *                         {@link io.openems.edge.controller.ess.gridoptimizedcharge.Mode#MANUAL}
	 *                         mode (used by GridOptimizedCharge), or null for
	 *                         automatic calculation
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> dummyEssTimeOfUseTariff(String componentId,
			List<StateMachine> activeModes, Double targetSocBuffer, LocalTime manualTargetTime) {
		return create(Controller.ESS_TIME_OF_USE_TARIFF, componentId,
				cmp -> io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler //
						.buildEnergyScheduleHandler(cmp, () -> new io.openems.edge.controller.ess.timeofusetariff. //
								EnergyScheduler.Config(activeModes, targetSocBuffer, manualTargetTime)));
	}

	/**
	 * Builds a {@link DummyEnergySchedulable} of Evse.Controller.Cluster.
	 *
	 * @param componentId          the Component-ID
	 * @param distributionStrategy the {@link DistributionStrategy}
	 * @param singleParams         the {@link Params} of Evse.Controller.Single
	 * @return the {@link DummyEnergySchedulable}
	 */
	public static DummyEnergySchedulable<? extends EnergyScheduleHandler> dummyEvseCluster(String componentId,
			DistributionStrategy distributionStrategy, ImmutableMap<String, Params> singleParams) {
		return create(Controller.EVSE_CLUSTER, componentId,
				cmp -> io.openems.edge.controller.evse.cluster.EnergyScheduler //
						.buildEnergyScheduleHandler(cmp, () -> CLOCK, () -> new io.openems.edge.controller.evse.cluster. //
								EnergyScheduler.ClusterEshConfig(distributionStrategy, singleParams)));
	}
}
