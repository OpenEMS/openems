package io.openems.edge.project.hofgutkarpfsee;

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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;
import io.openems.edge.ess.fenecon.commercial40.SystemState;

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
	private Clock clock;
	private static final int WAIT_FOR_SYSTEM_RESTART = 120;
	private LocalDateTime lastSwitchStartMode = LocalDateTime.MIN;
	private boolean executed = false;

	@Reference
	protected ComponentManager componentManager;

//	private LocalDateTime lastSwitchOffgridPv = LocalDateTime.MIN;
	private final Logger log = LoggerFactory.getLogger(EmergencyMode.class);

	@Reference
	protected ConfigurationAdmin cm;

	public EmergencyMode() {
		this(Clock.systemDefaultZone());
	}

	// Clock clock
	public EmergencyMode(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values(), //
				ChannelId.values()//
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
	private int currentActivePower = 0;

	/**
	 * Should the hysteresis be applied on passing high threshold?
	 */
	private boolean applyHysteresis = true;

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
		this.setDebugVlauesofWagoChannels();
		switch (this.getGridMode()) {
		/*
		 * Grid-Mode is undefined -> wait till we have some clear information
		 */
		case UNDEFINED:
			this.setOutput(blockHeatPowerPlantPermissionSignal, Operation.STOP);
			break;

		/*
		 * Off-Grid Mode -> wait till BHKW stop and System Restart. After that; Let it
		 * Run Off-Grid Process
		 */
		case OFF_GRID:
			SystemState systemState = ess.channel(EssFeneconCommercial40Impl.ChannelId.SYSTEM_STATE).value().asEnum();
			if (this.lastSwitchStartMode.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_SYSTEM_RESTART))) {
				return;
			}
			lastSwitchStartMode = LocalDateTime.now(this.clock);

			if (systemState != SystemState.START) {
				this.setOutput(blockHeatPowerPlantPermissionSignal, Operation.STOP);
				break;
			}

//			this.lastSwitchOffGridMode.getMinute();
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

	private enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DEBUG_DIGITAL_OUTPUT_BHKW(Doc.of(OpenemsType.INTEGER)), //
		DEBUG_DIGITAL_OUTPUT_MSR(Doc.of(OpenemsType.INTEGER)), //
		DEBUG_DIGITAL_OUTPUT_ON(Doc.of(OpenemsType.INTEGER)), //
		DEBUG_DIGITAL_OUTPUT_OFF(Doc.of(OpenemsType.INTEGER));//

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public void setDebugVlauesofWagoChannels() throws IllegalArgumentException, OpenemsNamedException {
		boolean m1c1 = this.isBlockHeatPowerPlantStopped();
		if (m1c1) {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_BHKW).setNextValue(1);
		} else {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_BHKW).setNextValue(0);
		}

		boolean m1c2 = this.isMsrHeatingSystemControllerOn();
		if (m1c2) {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_MSR).setNextValue(1);
		} else {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_MSR).setNextValue(0);
		}

		boolean m2c1 = this.isOnGridIndicationControllerOn();
		if (m2c1) {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_ON).setNextValue(1);
		} else {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_ON).setNextValue(0);
		}

		boolean m2c2 = this.isOffGridIndicationControllerOn();
		if (m2c2) {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_OFF).setNextValue(1);
		} else {
			this.channel(ChannelId.DEBUG_DIGITAL_OUTPUT_OFF).setNextValue(0);
		}
	}

	private void handleOnGridState() throws IllegalArgumentException, OpenemsNamedException {

		if (isBlockHeatPowerPlantStopped()) {
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.RUN);
		}

		if (isMsrHeatingSystemControllerOn()) {
			this.setOutput(this.msrHeatingSystemController, Operation.OFF);
		}

		if (isOffGridIndicationControllerOn()) {
			this.setOutput(this.offGridIndicationController, Operation.OFF);
		}

		if (!isOnGridIndicationControllerOn()) {
			this.setOutput(this.onGridIndicationController, Operation.ON);
		}

	}

	private void handleOffGridState(EssFeneconCommercial40Impl ess)
			throws IllegalArgumentException, OpenemsNamedException {

		if (this.isOnGridIndicationControllerOn()) {
			this.setOutput(this.onGridIndicationController, Operation.OFF);
		}
		if (!this.isMsrHeatingSystemControllerOn()) {
			this.setOutput(this.msrHeatingSystemController, Operation.ON);
		}
		if (!this.isOffGridIndicationControllerOn()) {
			this.setOutput(this.offGridIndicationController, Operation.ON);
		}

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
			if (value >= this.threshold) {
				this.applyHysteresis = false;
				this.state = State.ABOVE_THRESHOLD;
				break;
			}

			// evaluate if hysteresis is necessary
			if (value < this.threshold - hysteresis) {
				this.applyHysteresis = false; // do not apply high hysteresis anymore
			}

			if (this.previousState == State.PASS_THRESHOLD_COMING_FROM_ABOVE && applyHysteresis) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
				break;
			}
			if (this.getChargeState(ess) == ChargeState.DISCHARGE && applyHysteresis) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
				break;
			}

			if (applyHysteresis) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.RUN);
			}
			this.state = State.BELOW_THRESHOLD_AND_HYSTERESIS;
			break;

		case BELOW_THRESHOLD_AND_HYSTERESIS:
			this.previousState = State.BELOW_THRESHOLD_AND_HYSTERESIS;
			if (isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.RUN);
			}
			this.state = State.UNDEFINED;
			break;

		case PASS_THRESHOLD_COMING_FROM_ABOVE:
			if (!isBlockHeatPowerPlantStopped()) {
				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			}
			this.applyHysteresis = true;
			this.state = State.UNDEFINED;
			this.previousState = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
			break;

		case ABOVE_THRESHOLD:
			if (value <= this.threshold) {
				this.state = State.PASS_THRESHOLD_COMING_FROM_ABOVE;
				break;
			}
			this.previousState = State.ABOVE_THRESHOLD;
			this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.STOP);
			this.state = State.UNDEFINED;
			break;
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
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private GridMode getGridMode() throws IllegalArgumentException, OpenemsNamedException {
		SymmetricEss ess;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			return GridMode.UNDEFINED;
		}
		GridMode essGridMode = ess.getGridMode().value().asEnum();
		if ((essGridMode == GridMode.ON_GRID)) {
			this.executed = false;
			return GridMode.ON_GRID;
		} else if ((essGridMode == GridMode.OFF_GRID)) {
			if (!executed) {
				this.setOutput(blockHeatPowerPlantPermissionSignal, Operation.STOP);
				this.executed = true;
			}
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
