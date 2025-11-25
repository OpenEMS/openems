package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Utils.parseSmartConfig;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

import com.google.gson.JsonNull;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.SmartOptimizationConfig;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.EshWithOnlyOneMode;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public class EnergyScheduler {

	public static record EshEvseSingle(//
			EshWithDifferentModes<Actual, SmartOptimizationConfig, ScheduleContext> smartEnergyScheduleHandler,
			EshWithOnlyOneMode<ManualOptimizationContext, ScheduleContext> manualEnergyScheduleHandler) {

		protected static EshEvseSingle fromSmart(
				EshWithDifferentModes<Actual, SmartOptimizationConfig, ScheduleContext> smartEnergyScheduleHandler) {
			return new EshEvseSingle(smartEnergyScheduleHandler, null);
		}

		protected static EshEvseSingle fromManual(
				EshWithOnlyOneMode<ManualOptimizationContext, ScheduleContext> manualEnergyScheduleHandler) {
			return new EshEvseSingle(null, manualEnergyScheduleHandler);
		}

		protected synchronized void onChargePointIsReadyForChargingChange(Value<Boolean> before, Value<Boolean> after) {
			if (this.smartEnergyScheduleHandler != null) {
				// Trigger Reschedule on change of IS_READY_FOR_CHARGING
				this.smartEnergyScheduleHandler
						.triggerReschedule("ControllerEvseSingle::onChargePointIsReadyForChargingChange from [" + before
								+ "] to [" + after + "]");
			}
		}

		protected Mode.Actual getSmartModeActual(Mode.Actual orElse) {
			var esh = this.smartEnergyScheduleHandler;
			if (esh == null) {
				return orElse;
			}
			var period = esh.getCurrentPeriod();
			if (period == null) {
				return orElse;
			}
			return period.mode();
		}

		/**
		 * Initializes the {@link EnergyScheduleHandler}.
		 * 
		 * @param goc the {@link GlobalOptimizationContext}
		 * @return Config after initiialization
		 */
		public Config initialize(GlobalOptimizationContext goc) {
			if (this.smartEnergyScheduleHandler != null) {
				return (SmartOptimizationConfig) // /* this is safe */
				((AbstractEnergyScheduleHandler<?, ?>) this.smartEnergyScheduleHandler) //
						.initialize(goc);

			} else if (this.manualEnergyScheduleHandler != null) {
				return (ManualOptimizationContext) // /* this is safe */
				((AbstractEnergyScheduleHandler<?, ?>) this.manualEnergyScheduleHandler) //
						.initialize(goc);
			}
			return null;
		}

		/**
		 * Creates the {@link ScheduleContext}.
		 * 
		 * @return csc
		 */
		public ScheduleContext createScheduleContext() {
			if (this.smartEnergyScheduleHandler != null) {
				return this.smartEnergyScheduleHandler.createScheduleContext();

			} else if (this.manualEnergyScheduleHandler != null) {
				return this.manualEnergyScheduleHandler.createScheduleContext();
			}
			return null;
		}
	}

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
	public static EshWithDifferentModes<Mode.Actual, SmartOptimizationConfig, ScheduleContext> buildSmartEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<SmartOptimizationConfig> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode.Actual, SmartOptimizationConfig, ScheduleContext>create(
				parent) //
				.setSerializer(Config.serializer(), () -> configSupplier.get()) //

				.setAvailableModes((goc,
						coc) -> coc != null && coc.combinedAbilities.isReadyForCharging()
								&& !coc.appearsToBeFullyCharged //
										// TODO MINIMUM instead of ZERO if interrupt is not allowed
										? new Mode.Actual[] { Mode.Actual.SURPLUS, Mode.Actual.ZERO, Mode.Actual.FORCE } //
										: new Mode.Actual[] { Mode.Actual.ZERO } // No choice
				)

				.setOptimizationContext(gsc -> configSupplier.get())

				.setScheduleContext(() -> new ScheduleContext(0 /* TODO */))

				.build();
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
		var cons = ef.getManagedConsumption(id);
		if (cons == 0) {
			return Mode.Actual.ZERO;
		}
		return mode;
	}

	public static sealed interface Config {

		/**
		 * Gets the {@link CombinedAbilities}.
		 * 
		 * @return object
		 */
		public CombinedAbilities combinedAbilities();

		public static record ManualOptimizationContext(Mode.Actual mode, CombinedAbilities combinedAbilities,
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
							json.getObject("combinedAbilities", CombinedAbilities.serializer()), //
							json.getBoolean("appearsToBeFullyCharged"), //
							json.getInt("sessionEnergy"), //
							json.getInt("sessionEnergyLimit") //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("mode", obj.mode) //
							.add("combinedAbilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
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
				boolean appearsToBeFullyCharged, JSCalendar.Tasks<Payload> smartConfig) implements Config {

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
							json.getObject("smartConfig", JSCalendar.Tasks.serializer(Payload.serializer())) //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.add("combinedAbilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
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

	public static record SmartOptimizationContext(CombinedAbilities combinedAbilities, //
			ApplySetPoint.Ability.Watt applySetPointAbility, ZonedDateTime targetTime, Payload targetPayload) {

		protected static SmartOptimizationContext from(SmartOptimizationConfig config, OneTask<Payload> target) {
			var abilities = config.combinedAbilities();

			return new SmartOptimizationContext(abilities, abilities.applySetPoint(),
					target != null ? target.start() : null, //
					target != null ? target.payload() : null);
		}
	}

	public static class ScheduleContext {
		private int sessionEnergy;

		public ScheduleContext(int initialSessionEnergy) {
			this.sessionEnergy = initialSessionEnergy;
		}

		/**
		 * Applies the charge energy per period.
		 * 
		 * @param chargeEnergy the energy
		 */
		public void applyCharge(int chargeEnergy) {
			this.sessionEnergy += chargeEnergy;
		}

		public int getSessionEnergy() {
			return this.sessionEnergy;
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