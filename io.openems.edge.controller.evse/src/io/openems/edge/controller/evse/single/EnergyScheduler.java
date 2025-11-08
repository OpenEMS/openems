package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Utils.parseSmartConfig;

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
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

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
					var chargeEnergy = coc == null || !coc.abilities.isReadyForCharging() || coc.appearsToBeFullyCharged //
							? 0 //
							: calculateChargeEnergy(period, csc, ef, coc.mode, coc.abilities.applySetPoint(),
									coc.sessionEnergyLimit);
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
				.setAvailableModes((goc, coc) -> coc != null && coc.isReadyForCharging && !coc.appearsToBeFullyCharged //
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
						chargeEnergy = calculateChargeEnergy(period, csc, ef, mode, coc.applySetPointAbility, null);
					}
					applyChargeEnergy(id, csc, ef, chargeEnergy);
				})

				// Disabled for better traceability
				// .setPostProcessor(EnergyScheduler::postprocessSimulatorState)

				.build();
	}

	private static int calculateChargeEnergy(Period period, ScheduleContext csc, EnergyFlow.Model ef, Mode.Actual mode,
			ApplySetPoint.Ability.Watt applySetPointAbility, Integer sessionEnergyLimit) {
		// Evaluate Charge-Energy per mode
		int chargeEnergy = switch (mode) {
		case FORCE -> period.duration().convertPowerToEnergy(applySetPointAbility.max());
		case MINIMUM -> period.duration().convertPowerToEnergy(applySetPointAbility.min());
		case SURPLUS -> calculateSurplusEnergy(period, applySetPointAbility, ef.production, ef.unmanagedConsumption);
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

	private static void applyChargeEnergy(String id, ScheduleContext csc, EnergyFlow.Model ef, int targetChargeEnergy) {
		var actualChargeEnergy = ef.addConsumption(id, targetChargeEnergy);
		if (csc != null) {
			csc.applyCharge(actualChargeEnergy);
		}
	}

	private static int calculateSurplusEnergy(Period period, ApplySetPoint.Ability.Watt applySetPointAbility,
			int production, int consumption) {
		// TODO consider Non-Interruptable SURPLUS
		// TODO this would have to be calculated by EVSE-Cluster to handle distribution
		// correctly; current calculation causes more consumption per period than will
		// be applied in reality
		var surplus = production - consumption;
		if (surplus < period.duration().convertPowerToEnergy(applySetPointAbility.min())) {
			return 0; // Not sufficient surplus power
		}

		var maxEnergy = period.duration().convertPowerToEnergy(applySetPointAbility.max());
		if (surplus > maxEnergy) {
			return maxEnergy;
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
		for (var period : periodsBeforeTargetTime) {
			remainingEnergy = calculateSurplusEnergy(period, coc.applySetPointAbility, period.production(),
					period.consumption());
			modes[period.index()] = Mode.Actual.SURPLUS;
			if (remainingEnergy < 0) {
				break;
			}
		}

		// Remaining FORCE
		if (remainingEnergy > 0) {
			var sortedPeriods = periodsBeforeTargetTime.stream() //
					.sorted((p0, p1) -> Double.compare(p0.price(), p1.price())) //
					.toList();
			for (var period : sortedPeriods) {
				remainingEnergy += /* Remove SURPLUS from before */
						calculateSurplusEnergy(period, coc.applySetPointAbility, period.production(),
								period.consumption())
								/* Calculate FORCE energy */
								- period.duration().convertPowerToEnergy(coc.applySetPointAbility.max());
				modes[period.index()] = Mode.Actual.FORCE;
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
		return mode;
	}

	public static sealed interface Config {

		public static record ManualOptimizationContext(Mode.Actual mode, CombinedAbilities abilities,
				boolean appearsToBeFullyCharged, int sessionEnergy, int sessionEnergyLimit) implements Config {

			/**
			 * Returns a {@link JsonSerializer} for a {@link ManualOptimizationContext}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ManualOptimizationContext> serializer() {
				return jsonObjectSerializer(json -> {
					return new ManualOptimizationContext(//
							json.getEnum("mode", Mode.Actual.class), //
							json.getObject("abilities", CombinedAbilities.serializer()), //
							json.getBoolean("appearsToBeFullyCharged"), //
							json.getInt("sessionEnergy"), //
							json.getInt("sessionEnergyLimit") //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("mode", obj.mode) //
							.add("abilities", CombinedAbilities.serializer().serialize(obj.abilities)) //
							.addProperty("appearsToBeFullyCharged", obj.appearsToBeFullyCharged) //
							.addProperty("sessionEnergy", obj.sessionEnergy) //
							.addProperty("sessionEnergyLimit", obj.sessionEnergyLimit) //
							.build();
				});
			}

			protected static ManualOptimizationContext from(Mode.Actual mode, ChargePointAbilities chargePointAbilities,
					ElectricVehicleAbilities electricVehicleAbilities, boolean appearsToBeFullyCharged,
					int sessionEnergy, int sessionEnergyLimit) {
				final var combinedAbilities = CombinedAbilities
						.createFrom(chargePointAbilities, electricVehicleAbilities) //
						.build();
				return new ManualOptimizationContext(mode, combinedAbilities, appearsToBeFullyCharged, sessionEnergy,
						sessionEnergyLimit);
			}
		}

		public static record SmartOptimizationConfig(CombinedAbilities combinedAbilities,
				boolean appearsToBeFullyCharged, ImmutableList<Task<Payload>> smartConfig) implements Config {

			/**
			 * Returns a {@link JsonSerializer} for a {@link SmartOptimizationConfig}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<SmartOptimizationConfig> serializer() {
				return jsonObjectSerializer(json -> {
					return new SmartOptimizationConfig(//
							json.getObject("abilities", CombinedAbilities.serializer()), //
							json.getBoolean("appearsToBeFullyCharged"), //
							json.getImmutableList("smartConfig", JSCalendar.Task.serializer(Payload.serializer())) //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.add("abilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
							.addProperty("appearsToBeFullyCharged", obj.appearsToBeFullyCharged) //
							.add("smartConfig", JSCalendar.Tasks.serializer(Payload.serializer()) //
									.serialize(obj.smartConfig()))
							.build();
				});
			}

			protected static SmartOptimizationConfig from(ChargePointAbilities chargePointAbilities,
					ElectricVehicleAbilities electricVehicleAbilities, boolean appearsToBeFullyCharged,
					String smartConfigString) {
				final var combinedAbilities = CombinedAbilities
						.createFrom(chargePointAbilities, electricVehicleAbilities) //
						.build();
				final var smartConfig = parseSmartConfig(smartConfigString);
				return new SmartOptimizationConfig(combinedAbilities, appearsToBeFullyCharged, smartConfig);
			}
		}

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

	public static record SmartOptimizationContext(boolean isReadyForCharging,
			ApplySetPoint.Ability.Watt applySetPointAbility, boolean appearsToBeFullyCharged, ZonedDateTime targetTime,
			Payload targetPayload) {

		protected static SmartOptimizationContext from(SmartOptimizationConfig config, OneTask<Payload> target) {
			var abilities = config.combinedAbilities();

			return new SmartOptimizationContext(abilities.isReadyForCharging(), abilities.applySetPoint(),
					config.appearsToBeFullyCharged, //
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

	/**
	 * Factory for {@link EnergyScheduleHandler}.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier a {@link Supplier} for {@link Config}
	 * @return EnergyScheduleHandler
	 */
	public static EnergyScheduleHandler buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<? extends Config> configSupplier) {
		var config = configSupplier.get();
		return switch (config) {
		case ManualOptimizationContext moc -> buildManualEnergyScheduleHandler(parent, () -> moc);
		case SmartOptimizationConfig soc -> buildSmartEnergyScheduleHandler(parent, () -> soc);
		};
	}
}