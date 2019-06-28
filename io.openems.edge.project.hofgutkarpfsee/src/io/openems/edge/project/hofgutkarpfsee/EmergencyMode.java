package io.openems.edge.project.hofgutkarpfsee;

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
@Component(name = "EmergencyMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config;
	private ChannelAddress onGridIndicationController;
	private ChannelAddress offGridIndicationController;
	private ChannelAddress msrHeatingSystemController;
	private ChannelAddress blockHeatPowerPlantPermissionSignal;
	private ChannelAddress inputChannelAddress;
	private int threshold = 0;
	private int hysteresis = 0;
//	private Clock clock;
//	private static final int WAIT_FOR_SYSTEM_RESTART = 10;

	@Reference
	protected ComponentManager componentManager;

//	private LocalDateTime lastSwitchOffgridPv = LocalDateTime.MIN;
	private final Logger log = LoggerFactory.getLogger(EmergencyMode.class);

	@Reference
	protected ConfigurationAdmin cm;

//	public EmergencyMode() {
//		this(Clock.systemDefaultZone());
//	}

	// Clock clock
	public EmergencyMode() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values() //
		);
//		this.clock = clock;
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
		this.threshold = config.threshold();
		this.hysteresis = config.hysteresis();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;
	private State previousState = State.UNDEFINED;
	private GridMode previousGridState = GridMode.UNDEFINED;
	private int currentActivePower = 0;

	/**
	 * Should the hysteresis be applied on passing high threshold?
	 */
	private boolean applyHighHysteresis = true;

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
		/*
		 * Grid-Mode is undefined -> wait till we have some clear information
		 */
		case UNDEFINED:
			this.previousGridState = GridMode.UNDEFINED;
			break;

		/*
		 * Off-Grid Mode -> wait till BHKW stop and System Restart. After that; Let it
		 * Run Off-Grid Process
		 */
		case OFF_GRID:
			this.previousGridState = GridMode.OFF_GRID;
			if (!isBlockHeatPowerPlantStopped() && (this.previousGridState != GridMode.OFF_GRID)) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			}

//			if (this.lastSwitchOffgridPv.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_SYSTEM_RESTART))) {
//				return;
//			}
//			lastSwitchOffgridPv = LocalDateTime.now(this.clock);
			// TODO
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
			this.previousGridState = GridMode.ON_GRID;
			break;
		}
	}

	private void handleOnGridState() throws IllegalArgumentException, OpenemsNamedException {
		if (isBlockHeatPowerPlantStopped()) {
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.RUN);
		}
		if (isMsrHeatingSystemControllerOn()) {
			this.setOutput(this.msrHeatingSystemController, Operation.OFF);
		}

		if (!isOnGridIndicationControllerOn()) {
			this.setOutput(this.onGridIndicationController, Operation.ON);
		}
		if (isOffGridIndicationControllerOn()) {
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

			if (value > this.threshold) {
				this.state = State.ABOVE_THRESHOLD;
				break;
			}

			// evaluate if hysteresis is necessary
			if (value < this.threshold - hysteresis) {
				this.applyHighHysteresis = false; // do not apply high hysteresis anymore
			}
			if (value >= this.threshold) {
				this.applyHighHysteresis = false;
			}

			if (this.previousState == State.PASS_THRESHOLD_COMING_FROM_ABOVE && applyHighHysteresis) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
				break;
			}
			if (this.getChargeState(ess) == ChargeState.DISCHARGE && applyHighHysteresis) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
				break;
			}
			if (applyHighHysteresis) {
				if (value >= this.threshold + hysteresis) {
					// pass high with hysteresis
					this.state = State.PASS_THRESHOLD_COMING_FROM_BELOW;
					break;
				}
			} else {
				if (value >= this.threshold) {
					// pass high, not applying hysteresis
					this.state = State.PASS_THRESHOLD_COMING_FROM_BELOW;
					break;
				}
			}

			if (isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.RUN);
			}
			break;

		case PASS_THRESHOLD_COMING_FROM_BELOW:
			this.state = State.ABOVE_THRESHOLD;
			this.previousState = State.PASS_THRESHOLD_COMING_FROM_BELOW;
			break;

		case PASS_THRESHOLD_COMING_FROM_ABOVE:
			if (!isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			}
			this.applyHighHysteresis = true;
			this.state = State.UNDEFINED;
			this.previousState = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
			break;

		case ABOVE_THRESHOLD:
			if (value <= this.threshold) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
			}
			this.previousState = State.ABOVE_THRESHOLD;
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			break;
		}
		if (this.isOnGridIndicationControllerOn()) {
			this.setOutput(this.onGridIndicationController, Operation.OFF);
		}
		if (!this.isMsrHeatingSystemControllerOn()) {
			this.setOutput(this.msrHeatingSystemController, Operation.ON);
		}
		if (!this.isOffGridIndicationControllerOn()) {
			this.setOutput(this.offGridIndicationController, Operation.ON);
		}

	}

	private boolean isOnGridIndicationControllerOn() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel onGridIndicationController = this.componentManager
				.getChannel(this.onGridIndicationController);
		return onGridIndicationController.value().orElse(false);
	}

	private boolean isOffGridIndicationControllerOn() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel offGridIndicationController = this.componentManager
				.getChannel(this.offGridIndicationController);
		return offGridIndicationController.value().orElse(false);
	}

	private boolean isMsrHeatingSystemControllerOn() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel msrHeatingSystemController = this.componentManager
				.getChannel(this.msrHeatingSystemController);
		return msrHeatingSystemController.value().orElse(false);
	}

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
		case RUN:
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
