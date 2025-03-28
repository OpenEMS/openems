package io.openems.edge.controller.evse.single;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.edge.controller.evse.single.Utils.parseSmartConfig;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.SmartOptimizationConfig;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

public class EnergyScheduler {

	/**
	 * Builds the {@link EnergyScheduleHandler} for manual mode.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent      the parent {@link OpenemsComponent}
	 * @param cocSupplier supplier for {@link ManualOptimizationContext}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildManualEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<ManualOptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<ManualOptimizationContext, ScheduleContext>create(parent) //
				.setSerializer(() -> Config.toJson(cocSupplier.get())) //

				.setOptimizationContext(gsc -> cocSupplier.get())
				.setScheduleContext(coc -> coc == null ? null : new ScheduleContext(coc.sessionEnergy)) //

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					var chargeEnergy = coc == null || !coc.isReadyForCharging //
							? 0 //
							: calculateChargeEnergy(csc, ef, coc.mode, coc.limit, coc.sessionEnergyLimit);
					applyChargeEnergy(id, csc, ef, chargeEnergy);
				}) //

				.build();
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} for {@link Mode#SMART} mode.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link SmartOptimizationConfig}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<Mode.Actual, SmartOptimizationContext, ScheduleContext> buildSmartEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<SmartOptimizationConfig> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode.Actual, SmartOptimizationContext, ScheduleContext>create(
				parent) //
				.setSerializer(() -> Config.toJson(configSupplier.get())) //

				.setDefaultMode(Mode.Actual.SURPLUS) //
				.setAvailableModes((goc, coc) -> coc != null && coc.isReadyForCharging //
						// TODO MINIMUM instead of ZERO if interrupt is not allowed
						? new Mode.Actual[] { Mode.Actual.ZERO, Mode.Actual.SURPLUS, Mode.Actual.FORCE } //
						: new Mode.Actual[] { Mode.Actual.ZERO } // No choice
				)

				.setInitialPopulationsProvider(EnergyScheduler::initialPopulationsProvider)

				.setOptimizationContext(gsc -> {
					var config = configSupplier.get();
					if (config.smartConfig.isEmpty()) {
						return null;
					}
					var firstTime = gsc.periods().getFirst().time();
					var lastTime = gsc.periods().getLast().time();
					System.out.println("OPTIMIZER cocFunction smartConfig=" + config.smartConfig + "; firstTime="
							+ firstTime + "; lastTime=" + lastTime);
					var ots = JSCalendar.Tasks.getOccurencesBetween(config.smartConfig, firstTime, lastTime);
					System.out.println("OPTIMIZER cocFunction ots=" + ots);
					if (ots.isEmpty()) {
						return null;
					}
					return SmartOptimizationContext.from(config, ots.getFirst());
				})

				.setScheduleContext(() -> new ScheduleContext(0 /* TODO */))

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness) -> {
					final var periodInitialSessionEnergy = csc.sessionEnergy;
					final int chargeEnergy;
					if (coc == null) {
						chargeEnergy = 0;

					} else {
						var durationLeft = Duration.between(period.time(), coc.targetTime);
						if (durationLeft.isNegative()) {
							// TODO move this logic to EshCodec
							chargeEnergy = 0; // Do not plan Charging after TargetTime
							if (periodInitialSessionEnergy < coc.targetPayload.sessionEnergyMinimum) {
								fitness.addHardConstraintViolation((int) durationLeft.toMinutes() / -15);
							}

						} else if (!coc.isReadyForCharging) {
							chargeEnergy = 0;

						} else {
							chargeEnergy = calculateChargeEnergy(csc, ef, mode, coc.limit, null);
						}
					}
					applyChargeEnergy(id, csc, ef, chargeEnergy);
				})

				// Disabled for better traceability
				// .setPostProcessor(EnergyScheduler::postprocessSimulatorState)

				.build();
	}

	private static int calculateChargeEnergy(ScheduleContext csc, EnergyFlow.Model ef, Mode.Actual mode, Limit limit,
			Integer sessionEnergyLimit) {
		// Evaluate Charge-Energy per mode
		var chargeEnergy = toEnergy(switch (mode) {
		case FORCE -> limit.getMaxPower();
		case MINIMUM -> limit.getMinPower();
		case SURPLUS -> calculateSurplusPower(limit, ef.production, ef.unmanagedConsumption);
		case ZERO -> 0;
		});

		if (chargeEnergy <= 0) {
			return 0;
		}

		// Apply Session Energy Limit
		if (sessionEnergyLimit != null) {
			chargeEnergy = Math.min(sessionEnergyLimit - csc.sessionEnergy, chargeEnergy);
		}

		return chargeEnergy;
	}

	private static void applyChargeEnergy(String id, ScheduleContext csc, EnergyFlow.Model ef, int chargeEnergy) {
		ef.addConsumption(id, chargeEnergy);
		if (csc != null) {
			csc.applyCharge(chargeEnergy);
		}
	}

	private static int calculateSurplusPower(Limit limit, int production, int unmanagedConsumption) {
		// TODO consider Non-Interruptable SURPLUS
		// fitWithin(limit.getMinPower(), limit.getMaxPower(), //
		// ef.production - ef.unmanagedConsumption);
		var surplus = production - unmanagedConsumption;
		if (surplus < limit.getMinPower()) {
			return 0; // Not sufficient surplus power
		} else if (surplus > limit.getMaxPower()) {
			return limit.getMaxPower();
		} else {
			return surplus;
		}
	}

	private static ImmutableList<InitialPopulation<Mode.Actual>> initialPopulationsProvider(
			GlobalOptimizationContext goc, SmartOptimizationContext coc, Mode.Actual[] availableModes) {
		if (coc == null) {
			return ImmutableList.of();
		}

		var result = ImmutableList.<InitialPopulation<Mode.Actual>>builder();
		var periodsBeforeTargetTime = goc.periods().stream() //
				.filter(p -> !p.time().isAfter(coc.targetTime)) //
				.toList();

		// Try all SURPLUS
		var remainingEnergy = coc.targetPayload.sessionEnergyMinimum;
		var modes = goc.periods().stream() //
				.map(i -> Mode.Actual.ZERO) //
				.toArray(Mode.Actual[]::new);
		for (var p : periodsBeforeTargetTime) {
			remainingEnergy -= toEnergy(calculateSurplusPower(coc.limit, p.production(), p.consumption()));
			modes[p.index()] = Mode.Actual.SURPLUS;
			if (remainingEnergy < 0) {
				break;
			}
		}

		// Remaining FORCE
		if (remainingEnergy > 0) {
			var sortedPeriods = periodsBeforeTargetTime.stream() //
					.sorted((p0, p1) -> Double.compare(p0.price(), p1.price())) //
					.toList();
			for (var p : sortedPeriods) {
				remainingEnergy += /* Remove SURPLUS from before */
						toEnergy(calculateSurplusPower(coc.limit, p.production(), p.consumption()))
								/* Calculate FORCE energy */
								- toEnergy(coc.limit().getMaxPower());
				// TODO toEnergy consider quarter/hour minutes
				modes[p.index()] = Mode.Actual.FORCE;
				if (remainingEnergy < 0) {
					break;
				}
			}
		}
		result.add(new InitialPopulation<>(modes));

		return result.build();
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param id     an identifier, e.g. the Component-ID
	 * @param period the {@link GlobalOptimizationContext.Period}
	 * @param gsc    the {@link GlobalScheduleContext}
	 * @param ef     the {@link EnergyFlow}
	 * @param coc    the {@link SmartOptimizationContext}
	 * @param mode   the initial {@link Mode.Actual}
	 * @return the new Mode
	 */
	protected static Mode.Actual postprocessSimulatorState(String id, GlobalOptimizationContext.Period period,
			GlobalScheduleContext gsc, EnergyFlow ef, SmartOptimizationContext coc, Mode.Actual mode) {
		if (mode == Mode.Actual.ZERO) {
			return mode;
		}
		var cons = ef.getManagedCons(id);
		if (cons == 0) {
			return Mode.Actual.ZERO;
		}
		// TODO consider the other way around. Postprocess MINIMUM to SURPLUS
		if (mode == Mode.Actual.SURPLUS //
				&& cons == toEnergy(coc.limit().getMinPower())) {
			return Mode.Actual.MINIMUM;
		}
		return mode;
	}

	public static sealed interface Config {

		public static record ManualOptimizationContext(Mode.Actual mode, boolean isReadyForCharging, Limit limit,
				int sessionEnergy, int sessionEnergyLimit) implements Config {

			protected static ManualOptimizationContext from(Mode.Actual mode, EvseChargePoint.ChargeParams chargePoint,
					EvseElectricVehicle.ChargeParams electricVehicle, int sessionEnergy, int sessionEnergyLimit) {
				final boolean isReadyForCharging;
				final Limit limit;
				if (chargePoint == null || electricVehicle == null) {
					isReadyForCharging = false;
					limit = null;
				} else {
					isReadyForCharging = chargePoint.isReadyForCharging();
					limit = Utils.mergeLimits(chargePoint, electricVehicle);
				}
				return new ManualOptimizationContext(mode, isReadyForCharging, limit, sessionEnergy,
						sessionEnergyLimit);
			}

			@Override
			public final EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent) {
				return buildManualEnergyScheduleHandler(parent, () -> this);
			}
		}

		public static record SmartOptimizationConfig(boolean isReadyForCharging, Limit limit,
				ImmutableList<Task<Payload>> smartConfig) implements Config {

			protected static SmartOptimizationConfig from(EvseChargePoint.ChargeParams chargePoint,
					EvseElectricVehicle.ChargeParams electricVehicle, String smartConfigString) {
				final boolean isReadyForCharging;
				final Limit limit;
				if (chargePoint == null || electricVehicle == null) {
					isReadyForCharging = false;
					limit = null;
				} else {
					isReadyForCharging = chargePoint.isReadyForCharging();
					limit = Utils.mergeLimits(chargePoint, electricVehicle);
				}
				var smartConfig = parseSmartConfig(smartConfigString);
				return new SmartOptimizationConfig(isReadyForCharging, limit, smartConfig);
			}

			@Override
			public final EshWithDifferentModes<Mode.Actual, SmartOptimizationContext, ScheduleContext> buildEnergyScheduleHandler(
					OpenemsComponent parent) {
				return buildSmartEnergyScheduleHandler(parent, () -> this);
			}
		}

		/**
		 * EVSE is ready for charging.
		 * 
		 * @return boolean
		 */
		public boolean isReadyForCharging();

		/**
		 * EVSE {@link Limit}.
		 * 
		 * @return {@link Limit}
		 */
		public Limit limit();

		/**
		 * Factory for {@link EnergyScheduleHandler}.
		 * 
		 * @param parent the parent {@link OpenemsComponent}
		 * @return EnergyScheduleHandler.WithOnlyOneMode
		 */
		public EnergyScheduleHandler buildEnergyScheduleHandler(OpenemsComponent parent);

		/**
		 * Serialize.
		 * 
		 * @param config the {@link Config}, possibly null
		 * @return the {@link JsonElement}
		 */
		private static JsonElement toJson(Config config) {
			if (config == null) {
				return JsonNull.INSTANCE;
			}
			var b = buildJsonObject() //
					.addProperty("class", config.getClass().getSimpleName()) //
					.addProperty("isReadyForCharging", config.isReadyForCharging()) //
					.add("limit", Limit.toJson(config.limit()));
			switch (config) {
			case ManualOptimizationContext coc -> b //
					.addProperty("mode", coc.mode) //
					.addProperty("sessionEnergy", coc.sessionEnergy) //
					.addProperty("sessionEnergyLimit", coc.sessionEnergyLimit);
			case SmartOptimizationConfig coc -> b //
					.add("smartConfig", Tasks.toJsonArray(coc.smartConfig, Payload::toJson));
			}
			return b.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonElement}
		 * @return the {@link Config}
		 * @throws OpenemsNamedException on error
		 */
		public static Config fromJson(JsonElement j) {
			if (j.isJsonNull()) {
				return null;
			}
			try {
				var clazz = getAsString(j, "class");
				var isReadyForCharging = getAsBoolean(j, "isReadyForCharging");
				var limit = Limit.fromJson(getAsJsonObject(j, "limit"));

				if (clazz.equals(ManualOptimizationContext.class.getSimpleName())) {
					return new ManualOptimizationContext(//
							getAsEnum(Mode.Actual.class, j, "mode"), //
							isReadyForCharging, limit, //
							getAsInt(j, "sessionEnergy"), //
							getAsInt(j, "sessionEnergyLimit"));

				} else if (clazz.equals(SmartOptimizationConfig.class.getSimpleName())) {
					return new SmartOptimizationConfig(//
							isReadyForCharging, limit, //
							JSCalendar.Tasks.fromJson(getAsJsonArray(j, "smartConfig"), Payload::fromJson));

				} else {
					throw new IllegalArgumentException("Unsupported class [" + clazz + "]");
				}
			} catch (

			OpenemsNamedException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	public static record Payload(int sessionEnergyMinimum) {
		/**
		 * Parses one {@link JsonObject} to a {@link Payload} object.
		 * 
		 * @param j the {@link JsonObject}
		 * @return the {@link Payload} object
		 * @throws OpenemsNamedException on error
		 */
		public static Payload fromJson(JsonObject j) throws OpenemsNamedException {
			return new Payload(//
					getAsInt(j, "sessionEnergyMinimum"));
		}

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJson() {
			return buildJsonObject() //
					.addProperty("sessionEnergyMinimum", this.sessionEnergyMinimum) //
					.build();
		}
	}

	public static record SmartOptimizationContext(boolean isReadyForCharging, Limit limit, ZonedDateTime targetTime,
			Payload targetPayload) {

		protected static SmartOptimizationContext from(SmartOptimizationConfig config, OneTask<Payload> target) {
			return new SmartOptimizationContext(config.isReadyForCharging, config.limit, target.start(),
					target.payload());
		}
	}

	public static class ScheduleContext {
		private int sessionEnergy;

		public ScheduleContext(int initialSessionEnergy) {
			this.sessionEnergy = initialSessionEnergy;
		}

		protected void applyCharge(int chargeEnergy) {
			this.sessionEnergy += chargeEnergy;
		}
	}
}