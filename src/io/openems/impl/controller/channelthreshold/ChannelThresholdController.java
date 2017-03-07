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

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.hysteresis.Hysteresis;

@ThingInfo(title = "Switch channel on threshold")
public class ChannelThresholdController extends Controller {

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
	private Hysteresis thresholdHysteresis;
	private boolean isActive = false;

	/*
	 * Config
	 */
	@SuppressWarnings("unchecked")
	@ConfigInfo(title = "Channel", description = "Address of the channel that indicates the switching by the min and max threshold.", type = String.class)
	public ConfigChannel<String> thresholdChannelName = new ConfigChannel<String>("thresholdChannelAddress", this)
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
	@ConfigInfo(title = "Output", description = "Address of the digital output channel that should be switched.", type = String.class)
	public ConfigChannel<String> outputChannelName = new ConfigChannel<String>("outputChannelAddress", this)
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

	@ConfigInfo(title = "Low threshold", description = "Low threshold where the output should be switched on.", type = Long.class)
	public ConfigChannel<Long> lowerThreshold = new ConfigChannel<Long>("lowerThreshold", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				if (newValue.isPresent()) {
					createHysteresis();
				}
			});

	@ConfigInfo(title = "High threshold", description = "High threshold where the output should be switched off.", type = Long.class)
	public ConfigChannel<Long> upperThreshold = new ConfigChannel<Long>("upperThreshold", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				if (newValue.isPresent()) {
					createHysteresis();
				}
			});

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			if (thresholdHysteresis != null) {
				thresholdHysteresis.apply(thresholdChannel.value(), (state, multiplier) -> {
					try {
						switch (state) {
						case ABOVE:
							outputChannel.pushWrite(false);
							isActive = false;
							break;
						case ASC:
							if (isActive) {
								outputChannel.pushWrite(true);
							} else {
								outputChannel.pushWrite(false);
							}
							break;
						case BELOW:
							outputChannel.pushWrite(true);
							isActive = true;
							break;
						case DESC:
							if (isActive) {
								outputChannel.pushWrite(true);
							} else {
								outputChannel.pushWrite(false);
							}
							break;
						default:
							break;
						}
					} catch (WriteChannelException e) {
						log.error("failed to write outputChannel[" + outputChannel.id() + "]", e);
					}
				});
			}
		} catch (InvalidValueException e) {
			log.error("thresholdChannel has no valid value!");
		}
	}

	private void createHysteresis() {
		try {
			thresholdHysteresis = new Hysteresis(lowerThreshold.value(), upperThreshold.value());
		} catch (InvalidValueException e) {
			log.error("lower or upper Threshold is invalid! Can't create Hysteresis!");
		}
	}

}
