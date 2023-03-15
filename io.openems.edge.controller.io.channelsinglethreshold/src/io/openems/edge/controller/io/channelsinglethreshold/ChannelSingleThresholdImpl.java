package io.openems.edge.controller.io.channelsinglethreshold;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.controller.api.Controller;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.ChannelSingleThreshold", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ChannelSingleThresholdImpl extends AbstractOpenemsComponent
		implements ChannelSingleThreshold, Controller, OpenemsComponent, ModbusSlave {

	@Reference
	private ComponentManager componentManager;

	private final Logger log = LoggerFactory.getLogger(ChannelSingleThresholdImpl.class);
	private final Set<ChannelAddress> outputChannelAdresses = new HashSet<>();

	private Config config;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	/**
	 * The current state in the State Machine.
	 */
	private State state = State.UNDEFINED;

	public ChannelSingleThresholdImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelSingleThreshold.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private synchronized void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;

		// Parse Output Channels
		this.outputChannelAdresses.clear();
		for (String channel : config.outputChannelAddress()) {
			if (channel.isEmpty()) {
				continue;
			}
			this.outputChannelAdresses.add(ChannelAddress.fromString(channel));
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var outputChannels = this.getOutputChannels();

		switch (this.config.mode()) {
		case ON:
			this.setOutputs(outputChannels, true);
			break;
		case OFF:
			this.setOutputs(outputChannels, false);
			break;
		case AUTOMATIC:
			this.automaticMode(outputChannels);
			break;
		}
	}

	/**
	 * From the configured outputChannelAddresses get the actual Channel Objects.
	 *
	 * @return the list of Channels
	 * @throws Exception if no configured Channel is available; if at least one
	 *                   Channel is available, that one is returned even if other
	 *                   configured Channels were not available.
	 */
	private List<WriteChannel<Boolean>> getOutputChannels() throws OpenemsNamedException {
		OpenemsNamedException exceptionHappened = null;
		List<WriteChannel<Boolean>> result = new ArrayList<>();
		for (ChannelAddress channelAddress : this.outputChannelAdresses) {
			try {
				result.add(this.componentManager.getChannel(channelAddress));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
				exceptionHappened = e;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				exceptionHappened = new OpenemsException(e.getMessage());
			}
		}
		if (result.isEmpty() && exceptionHappened != null) {
			throw exceptionHappened;
		}
		return result;
	}

	/**
	 * Automated control.
	 *
	 * @param outputChannels the configured output channels
	 * @throws OpenemsNamedException on error
	 */
	private void automaticMode(List<WriteChannel<Boolean>> outputChannels) throws OpenemsNamedException {

		var inputChannelAddress = ChannelAddress.fromString(this.config.inputChannelAddress());

		// Get average input value of the last 'minimumSwitchingTime' seconds
		IntegerReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		var values = inputChannel.getPastValues().tailMap(
				LocalDateTime.now(this.componentManager.getClock()).minusSeconds(this.config.minimumSwitchingTime()),
				true).values();

		// make sure we have at least one value
		if (values.isEmpty()) {
			values = new ArrayList<>();
			values.add(inputChannel.value());
		}

		var inputValueOpt = values.stream().filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.average();
		int inputValue;
		if (inputValueOpt.isPresent()) {
			inputValue = (int) Math.round(inputValueOpt.getAsDouble());

			/*
			 * Power value (switchedLoadPower) of the output device is added to the input
			 * channel value to avoid immediate switching based on threshold - e.g. helpful
			 * when the input channel is the Grid Active Power.
			 *
			 * Example use case: if the feed-in is more than threshold, the output device is
			 * switched on and next second feed-in reduces below threshold and immediately
			 * switches off the device.
			 */
			if (outputChannels.stream()
					// At least one output channel is set
					.anyMatch(channel -> channel.value().get() == Boolean.TRUE)) {
				inputValue -= this.config.switchedLoadPower();
			}
		} else {
			// no input value available
			inputValue = -1; // is ignored later
			this.changeState(State.UNDEFINED);
		}

		// Evaluate State Machine
		switch (this.state) {
		case UNDEFINED:
			/*
			 * Starting... state is still undefined
			 */
			if (inputValueOpt.isPresent()) {
				if (inputValue <= this.config.threshold()) {
					this.changeState(State.BELOW_THRESHOLD);
				} else {
					this.changeState(State.ABOVE_THRESHOLD);
				}
			}
			break;

		case BELOW_THRESHOLD:
			/*
			 * Value is smaller or equal the low threshold -> always OFF
			 */
			if (inputValue > this.config.threshold()) {
				this.changeState(State.ABOVE_THRESHOLD);
				//this._setBelowThreshold(false);
				//this._setAboveThreshold(true);				
			}
			break;

		case ABOVE_THRESHOLD:
			/*
			 * Value is bigger than the high threshold -> always ON
			 */
			if (inputValue <= this.config.threshold()) {
				this.changeState(State.BELOW_THRESHOLD);
				//this._setBelowThreshold(true);
				//this._setAboveThreshold(false);					
			}
			break;
		}

		// Turn output ON or OFF depending on current state
		switch (this.state) {
		case UNDEFINED:
			/*
			 * Still Undefined -> always OFF
			 */
			this.setOutputs(outputChannels, false);
			break;

		case BELOW_THRESHOLD:
			/*
			 * Value is below threshold -> always OFF (or invert)
			 */
			this.setOutputs(outputChannels, false ^ this.config.invert());
			this._setRegulationActive(false);
			break;

		case ABOVE_THRESHOLD:
			/*
			 * Value is above threshold -> always ON (or invert)
			 */
			this.setOutputs(outputChannels, true ^ this.config.invert());
			this._setRegulationActive(true);
			break;
		}
	}

	/**
	 * A flag to maintain change in the state.
	 *
	 * @param nextState the target state
	 */
	private void changeState(State nextState) {
		var hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
		if (this.state != nextState) {
			if (this.lastStateChange.plus(hysteresis).isBefore(LocalDateTime.now(this.componentManager.getClock()))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.componentManager.getClock());
				this._setAwaitingHysteresis(false);
			} else {
				this._setAwaitingHysteresis(true);
			}
		} else {
			this._setAwaitingHysteresis(false);
		}
	}

	/**
	 * Helper function to switch multiple outputs if they were not switched before.
	 *
	 * @param outputChannels the output channels
	 * @param value          true to switch ON, false to switch ON
	 * @throws OpenemsNamedException on error
	 */
	private void setOutputs(List<WriteChannel<Boolean>> outputChannels, boolean value) throws OpenemsNamedException {
		for (WriteChannel<Boolean> outputChannel : outputChannels) {
			var currentValue = outputChannel.value();
			if (!currentValue.isDefined() || currentValue.get() != value) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (value ? "ON" : "OFF") + ".");
				outputChannel.setNextWriteValue(value);
			}
		}
	}
	
	// Export the current status via modbus
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {

		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //				
				ModbusSlaveNatureTable.of(ChannelSingleThreshold.class, accessMode, 100) //
					.channel(0, ChannelSingleThreshold.ChannelId.REGULATION_ACTIVE, ModbusType.UINT16)
					.build()
		);

	}


}


