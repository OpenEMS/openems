package io.openems.edge.controller.evcs;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Quantiles;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.evcs.JSCalendar.Task;
import io.openems.edge.controller.evcs.Utils.EshContext.EshManualContext;
import io.openems.edge.controller.evcs.Utils.EshContext.EshSmartContext;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.InitialPopulation;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext.Evcs;
import io.openems.edge.evcs.api.ChargeMode;

public final class Utils {

	protected static final int FORCE_CHARGE_POWER = 11000; // [W]
	protected static final int MIN_CHARGE_POWER = 4600; // [W]

	private static int TARGET_HOUR = 7;

	private Utils() {
	}

	/**
	 * Builds an {@link EnergyScheduleHandler} for SMART mode.
	 * 
	 * @param context a supplier for {@link EshSmartContext}
	 * @return the {@link EnergyScheduleHandler.WithDifferentStates}
	 */
	public static EnergyScheduleHandler.WithDifferentStates<SmartMode, EshSmartContext> buildEshSmart(
			Supplier<EshSmartContext> context) {
		return EnergyScheduleHandler.WithDifferentStates.<SmartMode, EshSmartContext>create() //
				.setDefaultState(SmartMode.ZERO) //
				// TODO if there is no surplus power, SmartMode.SURPLUS_PV should not be an
				// option
				.setAvailableStates(() -> SmartMode.values()) //
				.setContextFunction(simContext -> context.get()) //
				.setInitialPopulationsFunction(gsc -> {
					// Sets initial population to FORCE charge during cheapest periods
					var targetDateTime = getTargetDateTime(gsc.startTime(), TARGET_HOUR);
					var threshold = Quantiles.percentiles() //
							.index(5) //
							.compute(gsc.periods().stream() //
									.takeWhile(p -> !p.time().isAfter(targetDateTime)) //
									.mapToDouble(p -> p.price()) //
									.toArray());
					var times = gsc.periods().stream() //
							.filter(p -> p.price() < threshold) //
							.map(p -> p.time()) //
							.toList();
					return List.of(//
							InitialPopulation.of(times, SmartMode.FORCE));
				}) //
				.setSimulator((simContext, period, energyFlow, ctrlContext, mode) -> {
					final var evcsOne = simContext.evcss.get(ctrlContext.evcsId);
					switch (mode) {
					case FORCE -> applyChargeEnergy(energyFlow, ctrlContext, evcsOne, mode.chargeMode, mode.priority,
							MIN_CHARGE_POWER, FORCE_CHARGE_POWER);
					case ZERO -> doNothing();
					}

					if (period.time().isAfter(getTargetDateTime(simContext.global.startTime(), TARGET_HOUR))
							&& evcsOne.getInitialEnergySession() < ctrlContext.energySessionLimit) {
						// TODO apply JSCalendar SmartConfig
						// Charged less than EnergySessionLimit till next 7am.
						return 1_000_000; // add high cost
					}
					return 0.;
				}) //
				.setPostProcessor(Utils::postprocessSimulatorState) //
				.build();
	}

	/**
	 * Builds an {@link EnergyScheduleHandler} for MANUL mode.
	 * 
	 * @param context a supplier for {@link EshManualContext}
	 * @return the {@link EnergyScheduleHandler.WithOnlyOneState}
	 */
	public static EnergyScheduleHandler.WithOnlyOneState<EshManualContext> buildEshManual(
			Supplier<EshManualContext> context) {
		return EnergyScheduleHandler.WithOnlyOneState.<EshManualContext>create() //
				.setContextFunction(simContext -> context.get()) //
				.setSimulator((simContext, period, energyFlow, ctrlContext) -> {
					final var evcsGlobal = simContext.global.evcss().get(ctrlContext.evcsId);
					if (!ctrlContext.enabledCharging || evcsGlobal == null) {
						return;
					}
					switch (evcsGlobal.status()) {
					case CHARGING:
					case READY_FOR_CHARGING:
						break;
					case CHARGING_REJECTED:
					case ENERGY_LIMIT_REACHED:
					case ERROR:
					case NOT_READY_FOR_CHARGING:
					case STARTING:
					case UNDEFINED:
						return;
					}

					final var evcsOne = simContext.evcss.get(ctrlContext.evcsId);
					applyChargeEnergy(energyFlow, ctrlContext, evcsOne, ctrlContext.chargeMode, ctrlContext.priority,
							ctrlContext.defaultChargeMinPower, ctrlContext.forceChargeMinPower);
				}) //
				.build();
	}

	public static sealed interface EshContext {

		/**
		 * Gets the configured Energy-Session-Limit.
		 * 
		 * @return the value
		 */
		public int energySessionLimit();

		public static record EshManualContext(boolean enabledCharging, ChargeMode chargeMode, int forceChargeMinPower,
				int defaultChargeMinPower, Priority priority, String evcsId, int energySessionLimit)
				implements EshContext {

			/**
			 * Factory for {@link EshManualContext} from {@link Config}.
			 * 
			 * @param config the {@link Config}
			 * @return the {@link EshManualContext}
			 */
			public static EshManualContext fromConfig(Config config) {
				return new EshManualContext(config.enabledCharging(), config.chargeMode(), config.forceChargeMinPower(),
						config.defaultChargeMinPower(), config.priority(), config.evcs_id(),
						config.energySessionLimit());
			}
		}

		public static record EshSmartContext(String evcsId, int energySessionLimit /* TODO required */,
				ImmutableList<JSCalendar.Task<Payload>> tasks) implements EshContext {

			/**
			 * Factory for {@link EshSmartContext} from {@link Config}.
			 * 
			 * @param config the {@link Config}
			 * @return the {@link EshSmartContext}
			 */
			public static EshSmartContext fromConfig(Config config) {
				return new EshSmartContext(config.evcs_id(), config.energySessionLimit(),
						Payload.fromJson(config.smartConfig()));
			}

			public static record Payload(int energySessionLimit) {
				/**
				 * Parses the String configuration to {@link Payload} objects.
				 * 
				 * @param smartConfig the configuration
				 * @return the result
				 */
				public static ImmutableList<Task<Payload>> fromJson(String smartConfig) {
					if (smartConfig == null || smartConfig.isBlank()) {
						return ImmutableList.of();
					}

					try {
						return JSCalendar.Task.<Payload>fromJson(parseToJsonArray(smartConfig), Payload::fromJson);
					} catch (OpenemsNamedException e) {
						e.printStackTrace();
						return ImmutableList.of();
					}
				}

				/**
				 * Parses one {@link JsonObject} to a {@link Payload} object.
				 * 
				 * @param j the {@link JsonObject}
				 * @return the {@link Payload} object
				 * @throws OpenemsNamedException on error
				 */
				public static Payload fromJson(JsonObject j) throws OpenemsNamedException {
					var sessionEnergy = getAsInt(j, "sessionEnergy");
					return new Payload(sessionEnergy);
				}
			}
		}
	}

	protected static ZonedDateTime getTargetDateTime(ZonedDateTime startTime, int hour) {
		var localTime = startTime.withZoneSameInstant(Clock.systemDefaultZone().getZone());
		var targetDate = localTime.getHour() > hour //
				? startTime.plusDays(1) //
				: startTime;
		return targetDate.truncatedTo(ChronoUnit.DAYS).withHour(hour);
	}

	private static void applyChargeEnergy(EnergyFlow.Model energyFlow, EshContext ctrlContext, Evcs evcsOne,
			ChargeMode chargeMode, Priority priority, int chargeMinPower, int forceChargePower) {
		if (evcsOne == null) {
			return;
		}

		// Evaluate Charge-Energy per mode
		final var chargeEnergy = switch (chargeMode) {
		case EXCESS_POWER //
			-> switch (priority) {
			case CAR //
				-> toEnergy(//
						max(chargeMinPower, energyFlow.production - energyFlow.unmanagedConsumption));
			case STORAGE -> 0; // TODO not implemented
			};
		case FORCE_CHARGE //
			-> toEnergy(forceChargePower);
		};

		if (chargeEnergy <= 0) {
			return; // stop early
		}

		// Apply Session Limit
		final int limitedChargeEnergy;
		if (ctrlContext.energySessionLimit() > 0) {
			limitedChargeEnergy = min(chargeEnergy,
					max(0, ctrlContext.energySessionLimit() - evcsOne.getInitialEnergySession()));
		} else {
			limitedChargeEnergy = chargeEnergy;
		}

		if (limitedChargeEnergy > 0) {
			energyFlow.addConsumption(limitedChargeEnergy);
			evcsOne.calculateInitialEnergySession(limitedChargeEnergy);
		}
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param osc     the {@link OneSimulationContext}
	 * @param ef      the {@link EnergyFlow} for the state
	 * @param context the {@link EshContext}
	 * @param mode    the initial {@link SmartMode}
	 * @return the new state
	 */
	protected static SmartMode postprocessSimulatorState(OneSimulationContext osc, EnergyFlow ef, EshContext context,
			SmartMode mode) {
		if (mode == SmartMode.ZERO) {
			return mode;
		}
		if (ef.getManagedCons() == 0) { // TODO this works only reliably with one ControllerEvcs
			return SmartMode.ZERO;
		}
		return mode;
	}
}
