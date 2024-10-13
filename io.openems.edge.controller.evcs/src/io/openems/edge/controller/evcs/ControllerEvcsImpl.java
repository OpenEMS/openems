package io.openems.edge.controller.evcs;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.ManagedEvcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent, ModbusSlave {

	private static final int CHARGE_POWER_BUFFER = 200;
	private static final double DEFAULT_UPPER_TARGET_DIFFERENCE_PERCENT = 0.10; // 10%

	private final Logger log = LoggerFactory.getLogger(ControllerEvcsImpl.class);
	private final ChargingLowerThanTargetHandler chargingLowerThanTargetHandler;
	private final Clock clock;

	// Time of last charge power change, used for the hysteresis
	private Instant lastInitialCharge = Instant.MIN;

	// Time of last charge pause, used for the hysteresis
	private Instant lastChargePause = Instant.MIN;

	// Last charge power, used for the hysteresis
	private int lastChargePower = 0;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManagedEvcs evcs;

	private Config config;

	public ControllerEvcsImpl() {
		this(Clock.systemDefaultZone());
	}

	protected ControllerEvcsImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcs.ChannelId.values() //
		);
		this.clock = clock;
		this.chargingLowerThanTargetHandler = new ChargingLowerThanTargetHandler(clock);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.forceChargeMinPower() < 0) {
			throw new OpenemsException("Force-Charge Min-Power [" + config.forceChargeMinPower() + "] must be >= 0");
		}

		if (config.defaultChargeMinPower() < 0) {
			throw new OpenemsException(
					"Default-Charge Min-Power [" + config.defaultChargeMinPower() + "] must be >= 0");
		}

		this.config = config;
		this.evcs._setChargeMode(config.chargeMode());

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "evcs", config.evcs_id())) {
			return;
		}
		this.evcs._setMaximumPower(null);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * If the EVCS is clustered the method will set the charge power request.
	 * Otherwise it will set directly the charge power limit.
	 */
	@Override
	public void run() throws OpenemsNamedException {

		final var isClustered = this.evcs.getIsClustered().orElse(false);

		/*
		 * Stop early if charging is disabled
		 */
		if (!this.config.enabledCharging()) {
			this.evcs.setChargePowerLimit(0);
			if (isClustered) {
				this.evcs.setChargePowerRequest(0);
				this.resetMinMaxChannels();
			}
			return;
		}

		this.adaptConfigToHardwareLimits();

		this.evcs.setEnergyLimit(this.config.energySessionLimit());

		/*
		 * Sets a fixed request of 0 if the Charger is not ready
		 */
		if (isClustered) {

			var status = this.evcs.getStatus();
			switch (status) {
			case ERROR, STARTING, UNDEFINED, NOT_READY_FOR_CHARGING, ENERGY_LIMIT_REACHED -> {
				this.evcs.setChargePowerRequest(0);
				this.resetMinMaxChannels();
				return;
			}
			case CHARGING_REJECTED, READY_FOR_CHARGING, CHARGING_FINISHED -> {
				this.evcs._setMaximumPower(null);
			}
			case CHARGING -> {
			}
			}
		}

		/*
		 * Calculates the next charging power depending on the charge mode and priority
		 */
		var nextChargePower = //
				switch (this.config.chargeMode()) {
				case EXCESS_POWER -> //
					switch (this.config.priority()) {
					case CAR -> calculateChargePowerFromExcessPower(this.sum, this.evcs);
					case STORAGE -> {
						// SoC > 97 % or always, when there is no ESS is available
						if (this.sum.getEssSoc().orElse(100) > 97) {
							yield calculateChargePowerFromExcessPower(this.sum, this.evcs);
						} else {
							yield calculateExcessPowerAfterEss(this.sum, this.evcs);
						}
					}
					};
				case FORCE_CHARGE -> this.config.forceChargeMinPower() * this.evcs.getPhasesAsInt();
				};

		var nextMinPower = //
				switch (this.config.chargeMode()) {
				case EXCESS_POWER -> this.config.defaultChargeMinPower();
				case FORCE_CHARGE -> 0;
				};
		this.evcs._setMinimumPower(nextMinPower);

		nextChargePower = Math.max(nextChargePower, nextMinPower);

		// Charging under minimum hardware power isn't possible
		var minimumHardwarePower = this.evcs.getMinimumHardwarePower().orElse(0);
		if (nextChargePower < minimumHardwarePower) {
			nextChargePower = 0;
		}

		/**
		 * Calculates the maximum Power of the Car.
		 */
		if (nextChargePower != 0) {

			int chargePower = this.evcs.getChargePower().orElse(0);

			/**
			 * Check the difference of the current charge power and the previous charging
			 * target
			 */
			if (this.chargingLowerThanTargetHandler.isLower(this.evcs)) {

				var maximumPower = this.chargingLowerThanTargetHandler.getMaximumChargePower();
				if (maximumPower != null) {
					this.evcs._setMaximumPower(maximumPower + CHARGE_POWER_BUFFER);
					this.logDebug(this.log,
							"Maximum Charge Power of the EV reduced to" + maximumPower + " W plus buffer");
				}
			} else {
				int currMax = this.evcs.getMaximumPower().orElse(0);

				/**
				 * If the charge power would increases again above the current maximum power, it
				 * resets the maximum Power.
				 */
				if (chargePower > currMax * (1 + DEFAULT_UPPER_TARGET_DIFFERENCE_PERCENT)) {
					this.evcs._setMaximumPower(null);
				}
			}
		}

		if (this.config.chargeMode().equals(ChargeMode.EXCESS_POWER)) {
			// Apply hysteresis
			nextChargePower = this.applyHysteresis(nextChargePower);
		}

		if (isClustered) {
			this.evcs.setChargePowerRequest(nextChargePower);
		} else {
			this.evcs.setChargePowerLimit(nextChargePower);
		}
		this.logDebug(this.log, "Next charge power: " + nextChargePower + " W");
	}

	/**
	 * Resetting the minimum and maximum power channels.
	 */
	private void resetMinMaxChannels() {
		this.evcs._setMinimumPower(0);
		this.evcs._setMaximumPower(null);
	}

	/**
	 * Adapt the charge limits to the given hardware limits of the EVCS.
	 */
	private void adaptConfigToHardwareLimits() {

		var maxHardwareOpt = this.evcs.getMaximumHardwarePower().asOptional();
		if (maxHardwareOpt.isPresent()) {
			int maxHW = maxHardwareOpt.get();
			if (maxHW != 0) {
				maxHW = (int) Math.ceil(maxHW / 100.0) * 100;
				if (this.config.defaultChargeMinPower() > maxHW) {
					this.configUpdate("defaultChargeMinPower", maxHW);
				}
			}
		}

	}

	/**
	 * Calculates the next charging power, depending on the current PV production
	 * and house consumption.
	 *
	 * @param sum  the {@link Sum} component
	 * @param evcs Electric Vehicle Charging Station
	 * @return the available excess power for charging
	 * @throws OpenemsNamedException on error
	 */
	private static int calculateChargePowerFromExcessPower(Sum sum, ManagedEvcs evcs) throws OpenemsNamedException {
		int buyFromGrid = sum.getGridActivePower().orElse(0);
		int essDischarge = sum.getEssDischargePower().orElse(0);
		int evcsCharge = evcs.getChargePower().orElse(0);

		return evcsCharge - buyFromGrid - essDischarge;
	}

	/**
	 * Calculate result depending on the current evcs power and grid power.
	 *
	 * @param sum  the {@link Sum} component
	 * @param evcs the {@link ManagedEvcs}
	 * @return the excess power
	 */
	private static int calculateExcessPowerAfterEss(Sum sum, ManagedEvcs evcs) {
		int buyFromGrid = sum.getGridActivePower().orElse(0);
		int evcsCharge = evcs.getChargePower().orElse(0);

		var result = evcsCharge - buyFromGrid;

		// Add a buffer in Watt to have lower priority than the ess
		result -= 200;

		return result > 0 ? result : 0;
	}

	/**
	 * Applies the hysteresis to avoid too quick changes between a charge process
	 * and a pause.
	 *
	 * @param nextChargePower the next charge power limit
	 * @return next charge power or the last power if hysteresis is active
	 */
	private int applyHysteresis(int nextChargePower) {
		int targetChargePower = nextChargePower;
		boolean showWarning = false;
		var now = Instant.now(this.clock);

		// Wait at least the EVCS-specific response time, required to increase and
		// decrease the charging power
		if (awaitLastChanges(this.evcs.getChargeState().asEnum())) {
			// Still waiting for increasing, decreasing the power or undefined
			return this.lastChargePower;
		}
		// TODO: Show info, test and check if bellow logic still needed or need to be
		// different (Change only when we would change for xSeconds)

		// New charge power limit
		if (this.lastChargePower <= 0 && nextChargePower > 0) {
			var hysteresis = Duration.ofSeconds(this.config.excessChargePauseHysteresis());
			if (this.lastChargePause.plus(hysteresis).isBefore(now)) {

				// Start charing
				this.lastInitialCharge = now;
			} else {
				// Wait for hysteresis
				showWarning = true;
				targetChargePower = this.lastChargePower;
			}
		}

		// Pause charging by limiting to zero
		if (this.lastChargePower > 0 && nextChargePower <= 0) {
			var hysteresis = Duration.ofSeconds(this.config.excessChargeHystersis());
			if (this.lastInitialCharge.plus(hysteresis).isBefore(now)) {

				// Pause charing
				targetChargePower = 0;
				this.lastChargePause = now;
			} else {
				// Wait for hysteresis
				showWarning = true;
				targetChargePower = this.lastChargePower;
			}
		}

		// Apply results
		this.lastChargePower = targetChargePower;
		this.channel(ControllerEvcs.ChannelId.AWAITING_HYSTERESIS).setNextValue(showWarning);

		return targetChargePower;
	}

	/**
	 * Check if the evcs should wait for last changes.
	 * 
	 * <p>
	 * Since the charging stations and each car have their own response time until
	 * they charge at the set power, the controller waits until everything runs
	 * normally.
	 * 
	 * @param chargeState current evcs charge state
	 * @return The cvcs should await or not
	 */
	private static boolean awaitLastChanges(ChargeState chargeState) {
		if (chargeState.equals(ChargeState.INCREASING) || chargeState.equals(ChargeState.INCREASING)) {
			// Still waiting for increasing, decreasing the power
			return true;
		}
		return false;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Controller.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Updating the configuration property to given value.
	 *
	 * @param targetProperty Property that should be changed
	 * @param requiredValue  Value that should be set
	 */
	public void configUpdate(String targetProperty, Object requiredValue) {

		Configuration c;
		try {
			var pid = this.servicePid();
			if (pid.isEmpty()) {
				this.logInfo(this.log, "PID of " + this.id() + " is Empty");
				return;
			}
			c = this.cm.getConfiguration(pid, "?");
			var properties = c.getProperties();
			var target = properties.get(targetProperty);
			var existingTarget = target.toString();
			if (!existingTarget.isEmpty()) {
				properties.put(targetProperty, requiredValue);
				c.update(properties);
			}
		} catch (IOException | SecurityException e) {
			this.logError(this.log, "ERROR: " + e.getMessage());
		}
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
