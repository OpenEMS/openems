package io.openems.edge.project.controller.karpfsee.emergencymode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

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
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config;
	private ChannelAddress onGridIndicationController;
	private ChannelAddress offGridIndicationController;
	private ChannelAddress msrHeatingSystemController;
	private ChannelAddress blockHeatPowerPlantPermissionSignal;
	private ChannelAddress inputChannelAddress;
	private int lowThreshold = 0;
	private int highThreshold = 0;
	private int hysteresis = 0;
	private Clock clock;
	private static final int WAIT_FOR_SYSTEM_RESTART = 5;

	@Reference
	protected ComponentManager componentManager;

	private LocalDateTime lastSwitchOffgridPv = LocalDateTime.MIN;
	private final Logger log = LoggerFactory.getLogger(EmergencyMode.class);

	@Reference
	protected ConfigurationAdmin cm;

	public EmergencyMode() {
		this(Clock.systemDefaultZone());
	}

	protected EmergencyMode(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.onGridIndicationController = ChannelAddress.fromString(config.onGridIndicationController());
		this.offGridIndicationController = ChannelAddress.fromString(config.offGridIndicationController());
		this.msrHeatingSystemController = ChannelAddress.fromString(config.msrHeatingSystemController());
		this.blockHeatPowerPlantPermissionSignal = ChannelAddress
				.fromString(config.blockHeatPowerPlantPermissionSignal());
		this.lowThreshold = config.lowThreshold();
		this.highThreshold = config.highThreshold();
		this.hysteresis = config.hysteresis();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;
	private State previousState = State.UNDEFINED;
	private int currentActivePower = 0;

	/**
	 * Should the hysteresis be applied on passing high threshold?
	 */
	private boolean applyHighHysteresis = true;
	/**
	 * Should the hysteresis be applied on passing low threshold?
	 */
	private boolean applyLowHysteresis = true;

	private ChargeState getChargeState(EssFeneconCommercial40Impl ess) throws OpenemsNamedException {
		ess = this.componentManager.getComponent(this.config.ess_id());
		this.currentActivePower = ess.getActivePower().value().orElse(0);

		if ((this.currentActivePower > 0)) {
			return ChargeState.DISCHARGE;
		} else if ((this.currentActivePower < 0)) {
			return ChargeState.CHARGE;
		} else {
			return ChargeState.UNDEFINED;
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		EssFeneconCommercial40Impl ess = this.componentManager.getComponent(this.config.ess_id());

		switch (this.getGridMode()) {
		case UNDEFINED:
			/*
			 * Grid-Mode is undefined -> wait till we have some clear information
			 */
			break;
		case OFF_GRID:
			/*
			 * Off-Grid Mode -> wait till BHKW stop and System Restart. After that; Run Off-Grid
			 * Process
			 */

			if (!isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			}

			if (this.lastSwitchOffgridPv.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_SYSTEM_RESTART))) {
				return;
			}
			lastSwitchOffgridPv = LocalDateTime.now(this.clock);
			SystemState systemState = ess.channel(EssFeneconCommercial40Impl.ChannelId.SYSTEM_STATE).value().asEnum();
			if (systemState != SystemState.START) {
				break;
			}
			this.handleOffGridState(ess);
			break;

		case ON_GRID:
			/*
			 * On-Grid Mode -> Activate only On grid Mode and let BHKW runs
			 */
			this.handleOnGridState();
			break;
		}
	}

	private void handleOnGridState() throws IllegalArgumentException, OpenemsNamedException {
		if (isBlockHeatPowerPlantStopped()) {
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.START);
		}
		if (isMsrHeatingSystemControllerOff()) {
			this.setOutput(this.msrHeatingSystemController, Operation.OFF);
		}

		if (!isOnGridIndicationControllerOff()) {
			this.setOutput(this.onGridIndicationController, Operation.ON);
		}
		if (isOffGridIndicationControllerOff()) {
			this.setOutput(this.offGridIndicationController, Operation.OFF);
		}
	}

	private void handleOffGridState(EssFeneconCommercial40Impl ess)
			throws IllegalArgumentException, OpenemsNamedException {

		/*
		 * Check if all parameters are available
		 */
		int value;
		try {
			Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
			value = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
			return;
		}

		/*
		 * State Machine set to if value less than highthreshold keep
		 * this.blockHeatPowerPlantPermissionSignal, Operation.OPEN
		 */
		switch (this.state) {
		case UNDEFINED:
			if (value < this.lowThreshold) {
				this.state = State.BELOW_LOW;
			} else if (value > this.highThreshold) {
				this.state = State.ABOVE_HIGH;
			} else {
				this.state = State.BETWEEN_LOW_AND_HIGH;
			}
			break;

		case BELOW_LOW:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (value >= this.lowThreshold) {
				this.state = State.PASS_LOW_COMING_FROM_BELOW;
				break;
			}

			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.START);
			this.previousState = State.BELOW_LOW;
			break;

		case PASS_LOW_COMING_FROM_BELOW:
			/*
			 * Value just passed the low threshold coming from below -> turn ON
			 */
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.START);
			this.applyLowHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			this.previousState = State.PASS_LOW_COMING_FROM_BELOW;
			break;

		case BETWEEN_LOW_AND_HIGH:
			/*
			 * Value is between low and high threshold -> always OFF
			 */
			// evaluate if hysteresis is necessary
			if (value >= this.lowThreshold + hysteresis) {
				this.applyLowHysteresis = false; // do not apply low hysteresis anymore
			}
			if (value < this.highThreshold - hysteresis) {
				this.applyHighHysteresis = false; // do not apply high hysteresis anymore
			}

			if (value >= this.highThreshold) {
				this.applyHighHysteresis = false;
			}
			if (this.previousState == State.PASS_HIGH_COMING_FROM_ABOVE && applyHighHysteresis) {
				this.state = State.PASS_HIGH_COMING_FROM_ABOVE;
				break;
			}
			if (this.getChargeState(ess) == ChargeState.DISCHARGE && applyHighHysteresis) {
				this.state = State.PASS_HIGH_COMING_FROM_ABOVE;
				break;
			}
			/*
			 * Check LOW threshold
			 */
			if (applyLowHysteresis) {
				if (value <= this.lowThreshold - hysteresis) {
					// pass low with hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			} else {
				if (value <= this.lowThreshold) {
					// pass low, not applying hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			}

			/*
			 * Check HIGH threshold
			 */
			if (applyHighHysteresis) {
				if (value >= this.highThreshold + hysteresis) {
					// pass high with hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			} else {
				if (value >= this.highThreshold) {
					// pass high, not applying hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			}

			// Default: not switching the State -> always OFF
			if (isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.START);
			}
			break;

		case PASS_HIGH_COMING_FROM_BELOW:
			this.state = State.ABOVE_HIGH;
			this.previousState = State.PASS_HIGH_COMING_FROM_BELOW;
			break;

		case PASS_LOW_COMING_FROM_ABOVE:
			/*
			 * Value just passed the low threshold from above -> turn OFF
			 */
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.START);
			this.state = State.BELOW_LOW;
			this.previousState = State.PASS_LOW_COMING_FROM_ABOVE;
			break;

		case PASS_HIGH_COMING_FROM_ABOVE:
			/*
			 * Value just passed the high threshold coming from above -> turn ON
			 */
			if (!isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			}
			this.applyHighHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			this.previousState = State.PASS_HIGH_COMING_FROM_ABOVE;
			break;

		case ABOVE_HIGH:
			if (value <= this.highThreshold) {
				this.state = State.PASS_HIGH_COMING_FROM_ABOVE;
			}
			this.previousState = State.ABOVE_HIGH;
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			break;
		}
		if (this.isOnGridIndicationControllerOff()) {
			this.setOutput(this.onGridIndicationController, Operation.OFF);
		}
		if (!this.isMsrHeatingSystemControllerOff()) {
			this.setOutput(this.msrHeatingSystemController, Operation.ON);
		}
		if (!this.isOffGridIndicationControllerOff()) {
			this.setOutput(this.offGridIndicationController, Operation.ON);
		}

	}

	/* Off == Set_Zero */
	private boolean isOnGridIndicationControllerOff() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel onGridIndicationController = this.componentManager
				.getChannel(this.onGridIndicationController);
		return onGridIndicationController.value().orElse(false);
	}

	/* Off == Set_Zero */
	private boolean isOffGridIndicationControllerOff() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel offGridIndicationController = this.componentManager
				.getChannel(this.offGridIndicationController);
		return offGridIndicationController.value().orElse(false);
	}

	/* Off == Set_Zero */
	private boolean isMsrHeatingSystemControllerOff() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel msrHeatingSystemController = this.componentManager
				.getChannel(this.msrHeatingSystemController);
		return msrHeatingSystemController.value().orElse(false);
	}

	/* Stopped == Set_One */
	private boolean isBlockHeatPowerPlantStopped() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel blockHeatPowerPlantPermissionSignal = this.componentManager
				.getChannel(this.blockHeatPowerPlantPermissionSignal);
		return blockHeatPowerPlantPermissionSignal.value().orElse(false);
	}

	/**
	 * Gets the Grid-Mode of ESS.
	 * 
	 * @return the Grid-Mode
	 */
	private GridMode getGridMode() {
		SymmetricEss ess;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			return GridMode.UNDEFINED;
		}
		GridMode essGridMode = ess.getGridMode().value().asEnum();
		if ((essGridMode == GridMode.ON_GRID)) {
			return GridMode.ON_GRID;
		} else if ((essGridMode == GridMode.OFF_GRID)) {
			return GridMode.OFF_GRID;
		} else {
			return GridMode.UNDEFINED;
		}
	}

	/**
	 * Set Switch to Close or Open Operation.
	 * 
	 * @param channelAddress the Address of the BooleanWriteChannel
	 * @param operation      Close --> Make line connection; <br/>
	 *                       Open --> Make line disconnection
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(ChannelAddress channelAddress, Operation operation)
			throws IllegalArgumentException, OpenemsNamedException {
		boolean switchedOutput = false;
		BooleanWriteChannel channel = this.componentManager.getChannel(channelAddress);
		switch (operation) {
		case STOP:
			switchedOutput = this.setOutput(channel, true);
			break;
		case START:
			switchedOutput = this.setOutput(channel, false);
			break;
		case OFF:
			switchedOutput = this.setOutput(channel, false);
			break;
		case ON:
			switchedOutput = this.setOutput(channel, true);
			break;
		case UNDEFINED:
			break;
		}
		return switchedOutput;
	}

	/**
	 * Sets the Output.
	 * 
	 * @param channel the BooleanWriteChannel
	 * @param value   true to set the output, false to unset it
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(BooleanWriteChannel channel, boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		if (channel.value().asOptional().equals(Optional.of(value))) {
			// it is already in the desired state
			return false;
		} else {
			channel.setNextWriteValue(value);
			return true;
		}
	}
}
