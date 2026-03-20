package io.openems.edge.controller.ess.sohcycle;

import java.io.IOException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.sohcycle.statemachine.Context;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class)
@Component(//
		name = "Controller.Ess.SoH.Cycle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssSohCycleImpl extends AbstractOpenemsComponent implements ControllerEssSohCycle, Controller, OpenemsComponent {

	private static final Logger log = LoggerFactory.getLogger(ControllerEssSohCycleImpl.class);
	protected static final int ZERO_WATT_POWER = 0; // [0 W]

	private final StateMachine stateMachine;
	private final RampFilter rampFilter;
	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManagedSymmetricEss ess;

	@Reference
	private Sum sum;

	/**
	 * Maximum observed MIN_CELL_VOLTAGE during the measurement charging phase.
	 * Stored here to persist across handler invocations.
	 */
	private Integer measurementChargingMinVoltage;

	/**
	 * Maximum observed MAX_CELL_VOLTAGE during the measurement charging phase.
	 * Stored here to persist across handler invocations.
	 */
	private Integer measurementChargingMaxVoltage;

	/**
	 * Measurement baseline energy in Wh captured at the beginning of the measurement
	 * cycle. This is internal controller state and is intentionally not exposed via
	 * channels to keep handlers stateless and thread-safe.
	 */
	private Long measurementStartEnergyWh;

	@VisibleForTesting
	static ControllerEssSohCycleImpl startIn(StateMachine.State initialState) {
		return new ControllerEssSohCycleImpl(initialState, 0f);
	}

	public ControllerEssSohCycleImpl() {
		this(StateMachine.State.IDLE, null);
	}

	ControllerEssSohCycleImpl(StateMachine.State initialState, Float rampInitialValue) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssSohCycle.ChannelId.values() //
		);
		this.stateMachine = new StateMachine(initialState);
		this.rampFilter = new RampFilter(rampInitialValue);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ChannelUtils.setValue(this, ControllerEssSohCycle.ChannelId.STATE_MACHINE, this.stateMachine.getCurrentState());
		if (this.config.isRunning()) {
			this.runSohMeasurement();
		} else {
			this.handleNotRunning();
		}
	}

	private void handleNotRunning() throws OpenemsNamedException {
		this.logIfEnabled("Controller Ess SoH Cycle: not running");
		this.stateMachine.forceNextState(StateMachine.State.IDLE);
		this.stateMachine.run(new Context(this, //
				this.config, //
				this.componentManager.getClock(), //
				this.ess));
	}


	private void runSohMeasurement() throws OpenemsNamedException {
		var context = new Context(this, //
				this.config, //
				this.componentManager.getClock(), //
				this.ess);
		this.stateMachine.run(context);

		/*
		 * Apply ramped setpoint (targetPower limited by rampPower) after state machine
		 * computed the desired values.
		 */
		final Float targetPower = context.getTargetPower();
		final float rampPower = context.getRampPower();

		Integer activePower = null;
		if (targetPower != null && rampPower > 0f) {
			activePower = this.rampFilter.getFilteredValueAsInteger(targetPower, rampPower);
		}
		var limitedActivePower = this.calculateAcLimit(activePower == null ? ZERO_WATT_POWER : activePower);
		this.ess.setActivePowerEquals(limitedActivePower);
	}

	private void applyConfig(Config config) {
		this.config = config;
		// Validate ess_id
		if (config.ess_id() == null || config.ess_id().isBlank()) {
			logError(log, "Ess-ID is not configured!");
			return;
		}

		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	/**
	 * Logs a message only if log verbosity is set to DEBUG_LOG.
	 *
	 * @param message the message to log
	 */
	private void logIfEnabled(String message) {
		if (this.config.logVerbosity() == LogVerbosity.DEBUG_LOG) {
			log.info(message);
		}
	}

	public Integer getMeasurementChargingMinVoltage() {
		return this.measurementChargingMinVoltage;
	}

	public void setMeasurementChargingMinVoltage(Integer value) {
		this.measurementChargingMinVoltage = value;
	}

	public Integer getMeasurementChargingMaxVoltage() {
		return this.measurementChargingMaxVoltage;
	}

	public void setMeasurementChargingMaxVoltage(Integer value) {
		this.measurementChargingMaxVoltage = value;
	}

	/**
	 * Measurement baseline getter.
	 *
	 * @return measurement start energy in Wh or null if not initialized
	 */
	public Long getMeasurementStartEnergyWh() {
		return this.measurementStartEnergyWh;
	}

	/**
	 * Measurement baseline setter.
	 *
	 * @param energyWh measurement start energy in Wh or null to clear
	 */
	public void setMeasurementStartEnergyWh(Long energyWh) {
		this.measurementStartEnergyWh = energyWh;
	}

	/**
	 * Prevents next the SoH cycle by changing the "isRunning" configuration to false.
	 * This is used to stop the cycle in case of errors or when the cycle is completed.
	 */
	public void updateConfigToNotRunning() {
		try {
			var configuration = this.cm.getConfiguration(this.servicePid(), "?");
			var properties = configuration.getProperties();
			properties.put("isRunning", false);
			configuration.update(properties);
		} catch (IOException e) {
			log.error("Failed to update isRunning configuration to false", e);
		}
	}

	/**
	 * Calculating the AC limit.
	 *
	 * <p>
	 * Calculating the AC limit depending on the current DC production.
	 *
	 * @param targetPower charge/discharge power of the battery
	 * @return AC limit
	 */
	private int calculateAcLimit(int targetPower) {

		// Calculate AC-Setpoint depending on the DC production
		int productionDcPower = this.sum.getProductionDcActualPower().orElse(ZERO_WATT_POWER);

		return productionDcPower + targetPower;
	}

}
