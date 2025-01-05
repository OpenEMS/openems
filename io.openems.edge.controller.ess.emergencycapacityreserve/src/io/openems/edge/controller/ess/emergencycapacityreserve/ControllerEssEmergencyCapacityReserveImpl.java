package io.openems.edge.controller.ess.emergencycapacityreserve;

import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;

import java.util.OptionalInt;
import java.util.function.Supplier;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.Context;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.EmergencyCapacityReserve", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssEmergencyCapacityReserveImpl extends AbstractOpenemsComponent
		implements ControllerEssEmergencyCapacityReserve, EnergySchedulable, Controller, OpenemsComponent {

	/** Minimum reserve SoC value in [%]. */
	private static final int reservSocMinValue = 5;
	/** Maximum reserve SoC value in [%]. */
	private static final int reservSocMaxValue = 100;

	private final Logger log = LoggerFactory.getLogger(ControllerEssEmergencyCapacityReserveImpl.class);
	private final EnergyScheduleHandler energyScheduleHandler;
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final RampFilter rampFilter = new RampFilter();

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference
	private Meta meta;

	@Reference
	private ManagedSymmetricEss ess;

	private Config config;

	public ControllerEssEmergencyCapacityReserveImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssEmergencyCapacityReserve.ChannelId.values() //
		);
		this.energyScheduleHandler = buildEnergyScheduleHandler(//
				() -> this.config.isReserveSocEnabled() //
						? this.config.reserveSoc() //
						: null);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		super.modified(context, id, alias, enabled);
		this.updateConfig(this.config);
		this.energyScheduleHandler.triggerReschedule("ControllerEssEmergencyCapacityReserveImpl::modified()");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var context = this.handleStateMachine();
		Integer actualReserveSoc = null;

		if (this.config.isReserveSocEnabled()) {
			actualReserveSoc = this.config.reserveSoc();
			var activePowerConstraint = this.rampFilter.getFilteredValueAsInteger(context.getTargetPower(),
					context.getRampPower());

			if (activePowerConstraint != null && context.maxApparentPower > activePowerConstraint) {
				// Set constraint did not reach max apparent power
				this.ess.setActivePowerLessOrEquals(activePowerConstraint);
				this._setDebugSetActivePowerLessOrEquals(activePowerConstraint);
			} else {
				// Set no constraint max apparent power reached
				this.ess.setActivePowerLessOrEquals(null);
				this._setDebugSetActivePowerLessOrEquals(null);
			}

			// set debug channels
			this._setDebugTargetPower(context.getTargetPower());
			this._setDebugRampPower(context.getRampPower());
		}

		// Set the actual reserve Soc. This channel is used mainly for visualization in
		// UI.
		this._setActualReserveSoc(actualReserveSoc);
	}

	/**
	 * Update {@link Config} for the controller.
	 *
	 * @param config to update
	 */
	private void updateConfig(Config config) {
		this.config = config;

		var enableWarning = false;
		if (this.config.reserveSoc() < reservSocMinValue || this.config.reserveSoc() > reservSocMaxValue) {
			enableWarning = true;
		}

		this._setRangeOfReserveSocOutsideAllowedValue(enableWarning);

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	/**
	 * Handle different {@link State} of the {@link StateMachine}.
	 *
	 * @return created {@link Context}
	 */
	private Context handleStateMachine() {
		this._setStateMachine(this.stateMachine.getCurrentState());

		var soc = this.ess.getSoc();
		var maxApparentPower = this.ess.getMaxApparentPower();

		Integer socToUse = null;
		if (!soc.isDefined()) {
			// use last valid soc value
			var lastSocValue = this.getLastValidSoc(this.ess.getSocChannel());
			if (lastSocValue.isPresent()) {
				socToUse = lastSocValue.getAsInt();
			}
		} else {
			// use current soc value
			socToUse = soc.get();
		}

		if (socToUse == null || !maxApparentPower.isDefined()) {
			this.stateMachine.forceNextState(State.NO_LIMIT);
		}
		var context = new Context(this, this.sum, maxApparentPower.get(), socToUse, this.config.reserveSoc(),
				this.meta.getIsEssChargeFromGridAllowed());
		try {
			this.stateMachine.run(context);

			this.channel(Controller.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(Controller.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}

		return context;
	}

	/**
	 * Get last defined value of an {@link IntegerReadChannel} as an
	 * {@link OptionalInt}.
	 *
	 * @param channel {@link IntegerReadChannel} to get values
	 * @return Last defined value from given {@link IntegerReadChannel}
	 */
	private OptionalInt getLastValidSoc(IntegerReadChannel channel) {
		// get first defined value
		return channel.getPastValues().values() //
				.stream() //
				.filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.findFirst();
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param minSoc supplier for the configured minSoc
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler buildEnergyScheduleHandler(Supplier<Integer> minSoc) {
		return EnergyScheduleHandler.WithOnlyOneState.<Integer>create() //
				.setContextFunction(simContext -> minSoc.get() == null //
						? null //
						: socToEnergy(simContext.ess().totalEnergy(), minSoc.get())) //
				.setSimulator((simContext, period, energyFlow, minEnergy) -> {
					if (minEnergy != null) {
						energyFlow.setEssMaxDischarge(max(0, simContext.ess.getInitialEnergy() - minEnergy));
					}
				}) //
				.build();
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
