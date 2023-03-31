package io.openems.edge.controller.ess.fixstateofcharge.api;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fixstateofcharge.ConfigFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.Context;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.Timedata;

public abstract class AbstractFixStateOfCharge extends AbstractOpenemsComponent
		implements FixStateOfCharge, Controller, OpenemsComponent {

	private static final long INFO_DISPLAY_TIME = 1; // h

	private final StateMachine stateMachine = new StateMachine(State.IDLE);

	private RampFilter rampFilter = new RampFilter();

	// Configured TargetTime as ZonedDateTime
	private ZonedDateTime targetDateTime;

	/**
	 * Default power factor is applied to the maximum allowed charge power of the
	 * ess, to avoid very low charge power (Default 50%).
	 */
	public static final float DEFAULT_POWER_FACTOR = 0.50F;

	/**
	 * State of charge boundaries, to reduce the charge/discharge power when the
	 * target SoC is almost reached.
	 */
	public static final int DEFAULT_TARGET_SOC_BOUNDARIES = 2; // SoC steps

	/**
	 * Dead band to stay in "AT_TARGET_SOC" even if the SoC drops or rises by one.
	 */
	public static final int DEFAULT_DEAD_BAND_SOC_DIFFERENCE = 1; // SoC steps

	/**
	 * Boundaries power factor is applied to the maximum allowed charge power of the
	 * ess, to avoid very low charge power. (Default 25%).
	 */
	public static final float BOUNDARIES_POWER_FACTOR = 0.25F;

	/**
	 * Stopwatch started when target SoC reached.
	 */
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private final Logger log = LoggerFactory.getLogger(AbstractFixStateOfCharge.class);

	private ConfigProperties config;

	protected AbstractFixStateOfCharge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			ConfigProperties config) {
		super.activate(context, id, alias, enabled);
		this.updateConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, String id, String alias, boolean enabled, ConfigProperties config)
			throws OpenemsNamedException {
		super.modified(context, id, alias, enabled);
		this.updateConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Update {@link ConfigFixStateOfCharge} for the controller.
	 *
	 * @param config to update
	 */
	private void updateConfig(ConfigProperties config) {
		this.config = config;
		this.rampFilter = new RampFilter(0f);
		this.initializeTargetTime();
		this.stateMachine.forceNextState(State.IDLE);
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.setSelfTerminationInfoChannel();
		if (!this.config.isRunning()) {

			this._setStateMachine(State.IDLE);
			this.resetChannels();
			return;
		}

		// Set last Capacity from local timedata if present
		var capacity = this.getEss().getCapacity();
		if (capacity.isDefined()) {
			this.initiateEssCapacity(capacity.get());
		}

		var context = this.handleStateMachine();
		if (context == null) {
			this.stateMachine.forceNextState(State.IDLE);
			return;
		}

		if (this.terminationConditionFulfilled()) {
			this.resetController();
			return;
		}
		this.applyTargetPower(context.getTargetPower(), context.getRampPower(),
				this.getEss().getMaxApparentPower().orElse(0));
	}

	private Context handleStateMachine() {
		this._setStateMachine(this.stateMachine.getCurrentState());

		var soc = this.getEss().getSoc();
		var maxApparentPower = this.getEss().getMaxApparentPower();

		Integer socToUse = null;
		if (!soc.isDefined()) {
			// Use last valid soc value
			var lastSocValue = AbstractFixStateOfCharge.getLastValidValue(this.getEss().getSocChannel());
			if (lastSocValue.isPresent()) {
				socToUse = lastSocValue.getAsInt();
			}
		} else {
			// Use current soc value
			socToUse = soc.get();
		}

		if (socToUse == null || !maxApparentPower.isDefined()) {
			return null;
		}

		var context = new Context(this, this.config, this.getSum(), maxApparentPower.get(), socToUse,
				this.config.getTargetSoc(), this.targetDateTime, this.getComponentManager().getClock());
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
	 * Set the given target power.
	 * 
	 * @param targetPower      target power in W
	 * @param rampPower        ramp power
	 * @param maxApparentPower maximum apparent power
	 * @throws OpenemsNamedException on error
	 */
	private void applyTargetPower(Float targetPower, float rampPower, int maxApparentPower)
			throws OpenemsNamedException {
		var activePower = this.rampFilter.getFilteredValueAsInteger(targetPower, rampPower);

		this._setDebugSetActivePowerRaw(activePower);

		if (activePower == null) {
			this._setDebugSetActivePower(null);
			this._setDebugSetActivePowerRaw(null);
			this._setDebugRampPower(rampPower);
			this.updateEssWarningChannels(null);
			return;
		}

		this.updateEssWarningChannels(activePower);

		// Calculate AC-Setpoint depending on the DC production
		activePower = this.calculateAcLimit(activePower);

		// Fit into min/max "EssPower"
		if (this.getEss() instanceof ManagedSymmetricEss) {
			var e = (ManagedSymmetricEss) this.getEss();
			var maxCharge = e.getPower().getMinPower(e, Phase.ALL, Pwr.ACTIVE);
			var maxDischarge = e.getPower().getMaxPower(e, Phase.ALL, Pwr.ACTIVE);
			activePower = TypeUtils.fitWithin(maxCharge, maxDischarge, activePower);
		} else {
			activePower = TypeUtils.fitWithin(maxApparentPower * -1, maxApparentPower, activePower);
		}

		if (activePower > 0) {
			this.getEss().setActivePowerEquals(activePower);
		} else if (activePower < 0) {
			this.getEss().setActivePowerEquals(activePower);
		} else {
			this.getEss().setActivePowerEquals(activePower);
		}

		// Set debug channels
		this._setDebugSetActivePower(activePower);
		this._setDebugRampPower(rampPower);
	}

	/**
	 * Check if the termination condition fulfilled.
	 * 
	 * <p>
	 * Depending on the termination condition configured, detect if the condition
	 * fulfilled and return true (should terminate the controller)
	 * 
	 * @return termination condition is fulfilled
	 */
	private boolean terminationConditionFulfilled() {

		if (!this.config.isConditionalTermination()) {
			return false;
		}

		switch (this.config.getEndCondition()) {
		case CAPACITY_CHANGED:
			var currCapacity = this.getEss().getCapacity();
			var lastCapacity = AbstractFixStateOfCharge.getLastValidValue(this.getEssCapacityChannel());

			if (!currCapacity.isDefined() || lastCapacity.isEmpty()) {
				return false;
			}

			// Capacity changed
			if (lastCapacity.getAsInt() != currCapacity.get()) {
				return true;
			}
			break;
		}
		return false;
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
		int productionDcPower = this.getSum().getProductionDcActualPower().orElse(0);

		return productionDcPower + targetPower;
	}

	/**
	 * Helper to parse the configured target time into LocalDateTime.
	 */
	private void initializeTargetTime() {
		ZonedDateTime targetTime = null;
		boolean showWarning = false;

		if (this.config.isTargetTimeSpecified()) {
			try {
				// Try to parse ZonedDateTime format e.g. 2023-12-15T13:47:20+01:00
				targetTime = ZonedDateTime.parse(this.config.getTargetTime());
				showWarning = false;

			} catch (DateTimeParseException e) {
				// Parse failed -> show warning
				this.logError(this.log, "Not able to parse target time: " + e.getMessage());

				targetTime = null;
				showWarning = true;
			}

		} else {
			// Do not parse & show no warning
			targetTime = null;
			showWarning = false;
		}

		// Apply results
		this.targetDateTime = targetTime;
		this.channel(FixStateOfCharge.ChannelId.NO_VALID_TARGET_TIME).setNextValue(showWarning);
	}

	/**
	 * Updating the configuration property isRunning to false.
	 */
	public void resetController() {

		this.stateMachine.forceNextState(State.IDLE);
		this._setStateMachine(State.IDLE);
		this._setCtrlWasSelfTerminated(true);
		this._setAtTargetEpochSeconds(0);

		final var property = "isRunning";
		final var requiredValue = false;

		Configuration c;
		try {
			var pid = this.servicePid();
			if (pid.isEmpty()) {
				this.logInfo(this.log, "PID of " + this.id() + " is Empty");
				return;
			}
			c = this.getConfigurationAdmin().getConfiguration(pid, "?");
			var properties = c.getProperties();
			var target = properties.get(property);
			var existingTarget = target.toString();
			if (!existingTarget.isEmpty()) {
				properties.put(property, requiredValue);
				c.update(properties);
			}
		} catch (IOException | SecurityException e) {
			this.logError(this.log, "ERROR: " + e.getMessage());
		}
	}

	/**
	 * Set info channel if the Controller was terminated.
	 * 
	 * <p>
	 * Info (currently warning) is shown for INFO_DISPLAY_TIME hours}
	 */
	private void setSelfTerminationInfoChannel() {
		final var wasTerminatedOpt = this.getCtrlWasSelfTerminated();
		var wasTerminated = false;

		// Use latest valid value if not defined
		if (!wasTerminatedOpt.isDefined()) {
			wasTerminated = AbstractFixStateOfCharge //
					.getLastValidValue(this.getCtrlWasSelfTerminatedChannel()) //
					.orElse(false);
		} else {
			wasTerminated = wasTerminatedOpt.get();
		}

		// Running normal
		if (!wasTerminated) {
			return;
		}

		if (!this.stopwatch.isRunning()) {
			this.stopwatch.start();
		}

		if (this.stopwatch.elapsed(TimeUnit.HOURS) < INFO_DISPLAY_TIME) {
			this.channel(FixStateOfCharge.ChannelId.CTRL_WAS_SELF_TERMINATED).setNextValue(true);
			return;
		}
		this.channel(FixStateOfCharge.ChannelId.CTRL_WAS_SELF_TERMINATED).setNextValue(false);
	}

	private void resetChannels() {
		this._setCtrlIsBlockingEss(false);
		this._setCtrlIsChargingEss(false);
		this._setCtrlIsDischargingEss(false);
		this._setDebugSetActivePower(null);
		this._setDebugSetActivePowerRaw(null);
		this._setDebugRampPower(null);
	}

	private void updateEssWarningChannels(Integer activePower) {
		if (activePower == null) {
			this._setCtrlIsBlockingEss(false);
			this._setCtrlIsChargingEss(false);
			this._setCtrlIsDischargingEss(false);
			return;
		}

		this._setCtrlIsBlockingEss(activePower == 0);
		this._setCtrlIsChargingEss(activePower < 0);
		this._setCtrlIsDischargingEss(activePower > 0);
	}

	/**
	 * Initiate the ess capacity if not present.
	 * 
	 * @param capacity current capacity
	 */
	private void initiateEssCapacity(int capacity) {

		if (this.getEssCapacity().isDefined()) {
			this._setEssCapacity(capacity);
			return;
		}
		this.setLastCapacity(capacity);
	}

	/**
	 * Set the latest capacity given by this timedata.
	 * 
	 * @param fallbackCapacity Fallback value if there was no value in the timedata.
	 */
	private void setLastCapacity(int fallbackCapacity) {
		var timedata = this.getTimedata();
		var componentId = this.id();
		if (timedata == null || componentId == null) {
			return;
		} else {
			timedata.getLatestValue(new ChannelAddress(componentId, FixStateOfCharge.ChannelId.ESS_CAPACITY.id()))
					.thenAccept(capacity -> {
						if (this.getEssCapacity().isDefined()) {
							// Value has been read from device in the meantime
							return;
						}

						if (capacity.isPresent()) {
							try {
								this._setEssCapacity(TypeUtils.getAsType(OpenemsType.INTEGER, capacity));
								return;
							} catch (IllegalArgumentException e) {
								// Set initial EssCapacity
								this._setEssCapacity(fallbackCapacity);
								return;
							}
						} else {
							// Set initial EssCapacity
							this._setEssCapacity(fallbackCapacity);
							return;
						}
					});
		}
	}

	/**
	 * Get last defined value of an {@link IntegerReadChannel} as an
	 * {@link OptionalInt}.
	 *
	 * @param channel {@link IntegerReadChannel} to get values
	 * @return Last defined value from given {@link IntegerReadChannel}
	 */
	public static OptionalInt getLastValidValue(IntegerReadChannel channel) {
		// Possibly shift "getLastValidValue" to AbstractReadChannels
		return channel.getPastValues().values() //
				.stream() //
				.filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.findFirst();
	}

	/**
	 * Get last defined value of an {@link StateChannel} as an Boolean Optional.
	 *
	 * @param channel {@link StateChannel} to get values
	 * @return Last defined value
	 */
	private static Optional<Boolean> getLastValidValue(StateChannel channel) {
		return channel.getPastValues().values() //
				.stream() //
				.filter(Value::isDefined) //
				.map(Value::get).findFirst();
	}

	/**
	 * Get last defined value of an {@link LongReadChannel} as an
	 * {@link OptionalInt}.
	 *
	 * @param channel {@link LongReadChannel} to get values
	 * @return Last defined value from given {@link LongReadChannel}
	 */
	public static OptionalLong getLastValidValue(LongReadChannel channel) {
		return channel.getPastValues().values() //
				.stream() //
				.filter(Value::isDefined) //
				.mapToLong(Value::get) //
				.findFirst();
	}

	/**
	 * Get the {@link ComponentManager} reference.
	 * 
	 * @return Current {@link ComponentManager}
	 */
	public abstract ComponentManager getComponentManager();

	/**
	 * Get the {@link Sum} reference.
	 * 
	 * @return Current {@link Sum}
	 */
	public abstract Sum getSum();

	/**
	 * Get the {@link ManagedSymmetricEss} reference.
	 * 
	 * @return Current {@link ManagedSymmetricEss}
	 */
	public abstract ManagedSymmetricEss getEss();

	/**
	 * Get the {@link Timedata} reference.
	 * 
	 * @return Current {@link Timedata}
	 */
	public abstract Timedata getTimedata();

	/**
	 * Get the {@link ConfigurationAdmin} reference.
	 * 
	 * @return Current {@link ConfigurationAdmin}
	 */
	public abstract ConfigurationAdmin getConfigurationAdmin();

	/**
	 * Calculate the target power.
	 * 
	 * <p>
	 * Calculate the target power based on the given properties, to reach the target
	 * soc at the target time.
	 * 
	 * @param soc        current state of charge
	 * @param targetSoc  target state of charge
	 * @param capacity   ess capacity
	 * @param clock      clock
	 * @param targetTime target time
	 * @return charge power as negative value, discharge power as positive value
	 */
	public static Integer calculateTargetPower(int soc, int targetSoc, int capacity, Clock clock,
			ZonedDateTime targetTime) {
		var remainingSoC = Math.abs(soc - targetSoc);

		// Calculate the remaining capacity with remaining soc plus one, to avoid very
		// high
		// results at the end.
		remainingSoC += 1;

		// Remaining capacity of the battery in Ws.
		var remainingCapacity = Math.round(capacity * (remainingSoC) * 36);

		// Remaining time in seconds till the target time - buffer.
		var remainingTime = ChronoUnit.SECONDS.between(ZonedDateTime.now(clock), targetTime);

		// Passed target time - do not divide by zero
		if (remainingTime <= 0) {
			return null;
		}

		return TypeUtils.getAsType(OpenemsType.INTEGER, remainingCapacity / remainingTime);
	}

	/**
	 * Calculate required time to charge/discharge.
	 * 
	 * <p>
	 * Calculate the required time to reach the target SoC based on the given
	 * properties.
	 * 
	 * @param soc       current state of charge
	 * @param targetSoc target state of charge
	 * @param capacity  ess capacity
	 * @param power     power in watt
	 * @param clock     clock
	 * @return required time in seconds
	 */
	public static Integer calculateRequiredTime(int soc, int targetSoc, int capacity, int power, Clock clock) {
		var remainingSoC = Math.abs(soc - targetSoc);

		// Calculate the remaining capacity with remaining soc plus one, to avoid very
		// high
		// results at the end.
		remainingSoC += 1;

		// Remaining capacity of the battery in Ws.
		var remainingCapacity = Math.round(capacity * (remainingSoC) * 36);

		// ReqiredTime in seconds
		return remainingCapacity / power;
	}

}
