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
 * {
 *   "class": "io.openems.impl.controller.channelthreshold.ChannelThresholdController",
 *   "priority": 65,
 *   "thresholdChannelAddress": "ess0/Soc",
 *   "outputChannelAddress": "output0/1",
 *   "lowerThreshold": 75,
 *   "upperThreshold": 80,
 *   "invertOutput": true,
 *   "hysteresis": 2
 * }
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
	private boolean isActive = false;

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

	/*
	 * Methods
	 */
	@Override
	public void run() {
		// Check if all parameters are available
		long threshold;
		long lowerThreshold;
		long upperThreshold;
		long hysteresis;
		boolean invertOutput;
		try {
			threshold = this.thresholdChannel.value();
			lowerThreshold = this.lowerThreshold.value();
			upperThreshold = this.upperThreshold.value();
			hysteresis = this.hysteresis.value();
			invertOutput = this.invertOutput.value();
		} catch (InvalidValueException e) {
			log.error("ChannelThresholdController error: " + e.getMessage());
			return;
		}
		try {
			if (isActive) {
				if (threshold < lowerThreshold || threshold > upperThreshold + hysteresis) {
					isActive = false;
				} else {
					on(invertOutput);
				}
			} else {
				if (threshold >= lowerThreshold + hysteresis && threshold <= upperThreshold) {
					isActive = true;
				} else {
					off(invertOutput);
				}
			}
		} catch (WriteChannelException e) {
			log.error("Failed to write Channel[" + outputChannel.address() + "]: " + e.getMessage());
		}
	}

	private void on(boolean invertOutput) throws WriteChannelException {
		Optional<Boolean> currentValueOpt = this.outputChannel.valueOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != (true ^ invertOutput)) {
			log.info("Set output [" + this.outputChannel.address() + "] ON.");
			outputChannel.pushWrite(true ^ invertOutput);
		}
	}

	private void off(boolean invertOutput) throws WriteChannelException {
		Optional<Boolean> currentValueOpt = this.outputChannel.valueOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != (false ^ invertOutput)) {
			log.info("Set output [" + this.outputChannel.address() + "] OFF.");
			outputChannel.pushWrite(false ^ invertOutput);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
