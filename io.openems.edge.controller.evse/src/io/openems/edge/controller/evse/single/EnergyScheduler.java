package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Utils.mergeLimits;
import static io.openems.edge.controller.evse.single.Utils.parseSmartConfig;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonNull;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.SmartOptimizationConfig;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.EshWithOnlyOneMode;
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
	public static EshWithOnlyOneMode<ManualOptimizationContext, ScheduleContext> buildManualEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<ManualOptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<ManualOptimizationContext, ScheduleContext>create(parent) //
				.setSerializer(Config.serializer(), () -> cocSupplier.get()) //

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
				.setSerializer(Config.serializer(), () -> configSupplier.get()) //

				.setDefaultMode(Mode.Actual.SURPLUS) //
				.setAvailableModes((goc, coc) -> coc != null && coc.isReadyForCharging //
						// TODO MINIMUM instead of ZERO if interrupt is not allowed
						? new Mode.Actual[] { Mode.Actual.ZERO, Mode.Actual.SURPLUS, Mode.Actual.FORCE } //
						: new Mode.Actual[] { Mode.Actual.ZERO } // No choice
				)

				.setInitialPopulationsProvider(EnergyScheduler::initialPopulationsProvider)

				.setOptimizationContext(gsc -> {
					var config = configSupplier.get();
					final OneTask<Payload> ot;
					if (config.smartConfig.isEmpty()) {
						ot = null;
					} else {
						var firstTime = gsc.periods().getFirst().time();
						var lastTime = gsc.periods().getLast().time();
						System.out.println("OPTIMIZER cocFunction smartConfig=" + config.smartConfig + "; firstTime="
								+ firstTime + "; lastTime=" + lastTime);
						var ots = JSCalendar.Tasks.getOccurencesBetween(config.smartConfig, firstTime, lastTime);
						ot = ots.isEmpty() //
								? null //
								: ots.getFirst();
					}
					System.out.println("OPTIMIZER cocFunction ot=" + ot);
					return SmartOptimizationContext.from(config, ot);
				})

				.setScheduleContext(() -> new ScheduleContext(0 /* TODO */))

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness) -> {
					final var periodInitialSessionEnergy = csc.sessionEnergy;
					final int chargeEnergy;

					var durationLeft = coc.targetTime != null //
							? Duration.between(period.time(), coc.targetTime) //
							: null;
					if (durationLeft != null && durationLeft.isNegative()) {
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
					applyChargeEnergy(id, csc, ef, chargeEnergy);
				})

				// Disabled for better traceability
				// .setPostProcessor(EnergyScheduler::postprocessSimulatorState)

				.build();
	}

	// TODO Energy Period length?
	private static int calculateChargeEnergy(ScheduleContext csc, EnergyFlow.Model ef, Mode.Actual mode, Limit limit,
			Integer sessionEnergyLimit) {
		// Evaluate Charge-Energy per mode
		int chargeEnergy = switch (mode) {
		case FORCE -> toEnergy(limit.getMaxPower());
		case MINIMUM -> toEnergy(limit.getMinPower());
		case SURPLUS -> calculateSurplusEnergy(limit, ef.production, ef.unmanagedConsumption);
		case ZERO -> 0;
		};

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

	private static int calculateSurplusEnergy(Limit limit, int production, int unmanagedConsumption) {
		// TODO consider Non-Interruptable SURPLUS
		// fitWithin(limit.getMinPower(), limit.getMaxPower(), //
		// ef.production - ef.unmanagedConsumption);
		var surplus = production - unmanagedConsumption;
		if (surplus < toEnergy(limit.getMinPower())) {
			return 0; // Not sufficient surplus power
		} else if (surplus > toEnergy(limit.getMaxPower())) {
			return toEnergy(limit.getMaxPower());
		} else {
			return surplus;
		}
	}

	private static ImmutableList<InitialPopulation<Mode.Actual>> initialPopulationsProvider(
			GlobalOptimizationContext goc, SmartOptimizationContext coc, Mode.Actual[] availableModes) {
		if (coc.targetTime == null || coc.targetPayload == null) {
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
			remainingEnergy -= calculateSurplusEnergy(coc.limit, p.production(), p.consumption());
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
						calculateSurplusEnergy(coc.limit, p.production(), p.consumption())
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

			/**
			 * Returns a {@link JsonSerializer} for a {@link ManualOptimizationContext}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ManualOptimizationContext> serializer() {
				return jsonObjectSerializer(json -> {
					return new ManualOptimizationContext(//
							json.getEnum("mode", Mode.Actual.class), //
							json.getBoolean("isReadyForCharging"), //
							json.getObject("limit", Limit.serializer()), //
							json.getInt("sessionEnergy"), //
							json.getInt("sessionEnergyLimit") //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("isReadyForCharging", obj.isReadyForCharging()) //
							.add("limit", Limit.serializer().serialize(obj.limit())) //
							.build();
				});
			}

			protected static ManualOptimizationContext from(Mode.Actual mode, EvseChargePoint.ChargeParams chargePoint,
					EvseElectricVehicle.ChargeParams electricVehicle, int sessionEnergy, int sessionEnergyLimit) {
				final boolean isReadyForCharging;
				final Limit limit;
				if (chargePoint == null || electricVehicle == null) {
					isReadyForCharging = false;
					limit = null;
				} else {
					isReadyForCharging = chargePoint.isReadyForCharging();
					limit = mergeLimits(chargePoint, electricVehicle);
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

			/**
			 * Returns a {@link JsonSerializer} for a {@link SmartOptimizationConfig}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<SmartOptimizationConfig> serializer() {
				return jsonObjectSerializer(json -> {
					return new SmartOptimizationConfig(//
							json.getBoolean("isReadyForCharging"), //
							json.getObject("limit", Limit.serializer()), //
							json.getImmutableList("smartConfig", JSCalendar.Task.serializer(Payload.serializer())) //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("isReadyForCharging", obj.isReadyForCharging()) //
							.add("limit", Limit.serializer().serialize(obj.limit())) //
							.add("smartConfig", JSCalendar.Tasks.serializer(Payload.serializer()) //
									.serialize(obj.smartConfig()))
							.build();
				});
			}

			protected static SmartOptimizationConfig from(EvseChargePoint.ChargeParams chargePoint,
					EvseElectricVehicle.ChargeParams electricVehicle, String smartConfigString) {
				final boolean isReadyForCharging;
				final Limit limit;
				if (chargePoint == null || electricVehicle == null) {
					isReadyForCharging = false;
					limit = null;
				} else {
					isReadyForCharging = chargePoint.isReadyForCharging();
					limit = mergeLimits(chargePoint, electricVehicle);
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
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<Config>create() //
					.add(ManualOptimizationContext.class, ManualOptimizationContext.serializer(),
							ManualOptimizationContext.class.getSimpleName()) //
					.add(SmartOptimizationConfig.class, SmartOptimizationConfig.serializer(),
							SmartOptimizationConfig.class.getSimpleName()) //
					.build();

			return jsonSerializer(Config.class, json -> {
				return json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("class"));
			}, obj -> {
				if (obj == null) {
					return JsonNull.INSTANCE;
				}

				return polymorphicSerializer.serialize(obj);
			});
		}
	}

	public static record Payload(int sessionEnergyMinimum) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Payload}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Payload> serializer() {
			return jsonObjectSerializer(Payload.class, json -> {
				return new Payload(//
						json.getInt("sessionEnergyMinimum") //
				);
			}, obj -> {
				return obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.addProperty("sessionEnergyMinimum", obj.sessionEnergyMinimum) //
								.build();
			});
		}
	}

	public static record SmartOptimizationContext(boolean isReadyForCharging, Limit limit, ZonedDateTime targetTime,
			Payload targetPayload) {

		protected static SmartOptimizationContext from(SmartOptimizationConfig config, OneTask<Payload> target) {
			return new SmartOptimizationContext(config.isReadyForCharging, config.limit, //
					target != null ? target.start() : null, //
					target != null ? target.payload() : null);
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