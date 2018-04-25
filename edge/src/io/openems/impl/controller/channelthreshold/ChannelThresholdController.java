/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.channelthreshold;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

/*
 * Example config:
 * <pre>
{
	"class": "io.openems.impl.controller.channelthreshold.ChannelThresholdController",
	"priority": 65,
	"thresholdChannelAddress": "ess0/Soc",
	"outputChannelAddress": "output0/1",
	"lowerThreshold": 75,
	"upperThreshold": 80,
	"invertOutput": true,
	"hysteresis": 2
}
 * </pre>
 */

@ThingInfo(title = "Switch channel on threshold")
public class ChannelThresholdController extends Controller {

	private final Logger log = LoggerFactory.getLogger(ChannelThresholdController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public ChannelThresholdController() {
		super();
	}

	public ChannelThresholdController(String thingId) {
		super(thingId);
	}

	/*
	 * Fields
	 */
	private ThingRepository repo = ThingRepository.getInstance();
	private ReadChannel<Long> thresholdChannel;
	private WriteChannel<Boolean> outputChannel;

	/*
	 * Config
	 */
	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "Channel", description = "Address of the channel that indicates the switching by the min and max threshold.", type = String.class)
	public ConfigChannel<String> thresholdChannelAddress = new ConfigChannel<String>("thresholdChannelAddress", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		Optional<String> channelAddress = (Optional<String>) newValue;
		if (channelAddress.isPresent()) {
			Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
			if (ch.isPresent()) {
				thresholdChannel = (ReadChannel<Long>) ch.get();
			} else {
				log.error("Channel " + channelAddress.get() + " not found");
			}
		} else {
			log.error("'outputChannelAddress' is not configured!");
		}
	});

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "Output", description = "Address of the digital output channel that should be switched.", type = String.class)
	public ConfigChannel<String> outputChannelAddress = new ConfigChannel<String>("outputChannelAddress", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		Optional<String> channelAddress = (Optional<String>) newValue;
		if (channelAddress.isPresent()) {
			Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
			if (ch.isPresent()) {
				outputChannel = (WriteChannel<Boolean>) ch.get();
			} else {
				log.error("Channel " + channelAddress.get() + " not found");
			}
		} else {
			log.error("'outputChannelAddress' is not configured!");
		}
	});

	@ChannelInfo(title = "Low threshold", description = "Low threshold where the output should be switched on.", type = Long.class)
	public ConfigChannel<Long> lowerThreshold = new ConfigChannel<Long>("lowerThreshold", this);

	@ChannelInfo(title = "High threshold", description = "High threshold where the output should be switched off.", type = Long.class)
	public ConfigChannel<Long> upperThreshold = new ConfigChannel<Long>("upperThreshold", this);

	@ChannelInfo(title = "Hysteresis", description = "Hysteresis for lower and upper threshold", type = Long.class)
	public ConfigChannel<Long> hysteresis = new ConfigChannel<Long>("hysteresis", this);

	@ChannelInfo(title = "Invert-Output", description = "True if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<Boolean>("invertOutput", this).defaultValue(false);

	private enum State {
		UNDEFINED, /* Unknown state on first start */
		BELOW_LOW, /* Value is smaller than the low threshold */
		PASS_LOW_COMING_FROM_BELOW, /* Value just passed the low threshold. Last value was even lower. */
		PASS_LOW_COMING_FROM_ABOVE, /* Value just passed the low threshold. Last value was higher. */
		BETWEEN_LOW_AND_HIGH, /* Value is between low and high threshold */
		PASS_HIGH_COMING_FROM_BELOW, /* Value just passed the high threshold. Last value was lower. */
		PASS_HIGH_COMING_FROM_ABOVE, /* Value just passed the high threshold. Last value was higher. */
		ABOVE_HIGH /* Value is bigger than the high threshold */
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;

	/**
	 * Should the hysteresis be applied on passing high threshold?
	 */
	private boolean applyHighHysteresis = true;
	/**
	 * Should the hysteresis be applied on passing low threshold?
	 */
	private boolean applyLowHysteresis = true;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		/*
		 * Check if all parameters are available
		 */
		long value;
		long lowerThreshold;
		long upperThreshold;
		long hysteresis;
		try {
			value = this.thresholdChannel.value();
			lowerThreshold = this.lowerThreshold.value();
			upperThreshold = this.upperThreshold.value();
			hysteresis = this.hysteresis.value();
		} catch (InvalidValueException e) {
			log.error("ChannelThresholdController error: " + e.getMessage());
			return;
		}

		/*
		 * State Machine
		 */
		switch (this.state) {
		case UNDEFINED:
			if (value < lowerThreshold) {
				this.state = State.BELOW_LOW;
			} else if (value > upperThreshold) {
				this.state = State.ABOVE_HIGH;
			} else {
				this.state = State.BETWEEN_LOW_AND_HIGH;
			}
			break;

		case BELOW_LOW:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (value >= lowerThreshold) {
				this.state = State.PASS_LOW_COMING_FROM_BELOW;
				break;
			}

			this.off();
			break;

		case PASS_LOW_COMING_FROM_BELOW:
			/*
			 * Value just passed the low threshold coming from below -> turn ON
			 */
			this.on();
			this.applyLowHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			break;

		case BETWEEN_LOW_AND_HIGH:
			/*
			 * Value is between low and high threshold -> always ON
			 */
			// evaluate if hysteresis is necessary
			if (value >= lowerThreshold + hysteresis) {
				this.applyLowHysteresis = false; // do not apply low hysteresis anymore
			}
			if (value <= upperThreshold - hysteresis) {
				this.applyHighHysteresis = false; // do not apply high hysteresis anymore
			}

			/*
			 * Check LOW threshold
			 */
			if (applyLowHysteresis) {
				if (value <= lowerThreshold - hysteresis) {
					// pass low with hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			} else {
				if (value <= lowerThreshold) {
					// pass low, not applying hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			}

			/*
			 * Check HIGH threshold
			 */
			if (applyHighHysteresis) {
				if (value >= upperThreshold + hysteresis) {
					// pass high with hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			} else {
				if (value >= upperThreshold) {
					// pass high, not applying hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			}

			// Default: not switching the State -> always ON
			this.on();
			break;

		case PASS_HIGH_COMING_FROM_BELOW:
			/*
			 * Value just passed the high threshold coming from below -> turn OFF
			 */
			this.off();
			this.state = State.ABOVE_HIGH;
			break;

		case PASS_LOW_COMING_FROM_ABOVE:
			/*
			 * Value just passed the low threshold from above -> turn OFF
			 */
			this.off();
			this.state = State.BELOW_LOW;
			break;

		case PASS_HIGH_COMING_FROM_ABOVE:
			/*
			 * Value just passed the high threshold coming from above -> turn ON
			 */
			this.on();
			this.applyHighHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			break;

		case ABOVE_HIGH:
			/*
			 * Value is bigger than the high threshold -> always OFF
			 */
			if (value <= upperThreshold) {
				this.state = State.PASS_HIGH_COMING_FROM_ABOVE;
			}

			this.off();
			break;
		}
	}

	/**
	 * Switch the output ON
	 */
	private void on() {
		this.setOutput(true);
	}

	/**
	 * Switch the output OFF
	 */
	private void off() {
		this.setOutput(false);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value
	 *            true to switch ON, false to switch ON; is inverted if 'invertOutput' config is set
	 */
	private void setOutput(boolean value) {
		Optional<Boolean> currentValueOpt = this.outputChannel.valueOptional();
		boolean invertOutput = this.invertOutput.valueOptional().orElse(false);
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != (value ^ invertOutput)) {
			log.info("Set output [" + this.outputChannel.address() + "] " + (value ? "ON" : "OFF") + ".");
			try {
				outputChannel.pushWrite(value ^ invertOutput);
			} catch (WriteChannelException e) {
				log.error("ChannelThresholdController error: " + e.getMessage());
			}
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
