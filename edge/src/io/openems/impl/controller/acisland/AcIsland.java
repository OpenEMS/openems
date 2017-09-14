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
package io.openems.impl.controller.acisland;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

public class AcIsland extends Controller {

	private ThingRepository repo = ThingRepository.getInstance();
	private WriteChannel<Boolean> onGridOutputChannel;
	private WriteChannel<Boolean> offGridOutputChannel;
	private State currentState = State.UNKNOWN;
	private boolean isProducerDisconnected = false;
	private long timeProducerDisconnected;

	private enum State {
		OFFGRID, ONGRID, SWITCHTOOFFGRID, SWITCHTOONGRID, UNKNOWN
	}

	// ConfigChannel

	@ChannelInfo(title = "soc to disconnect the producer if the system is Off-Grid.", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this).defaultValue(85L);
	@ChannelInfo(title = "soc to connect the producer if the system is Off-Grid.", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this).defaultValue(70L);

	@ChannelInfo(title = "The ess where the grid state should be read from.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "time to wait before switch output on.", type = Long.class)
	public ConfigChannel<Long> switchDelay = new ConfigChannel<Long>("switchDelay", this).defaultValue(10000L);

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the Digital Output where the on grid connection of producer is connected to.", type = String.class)
	public ConfigChannel<String> onGridOutputChannelAddress = new ConfigChannel<String>("onGridOutputChannelAddress",
			this).addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						onGridOutputChannel = (WriteChannel<Boolean>) ch.get();
						onGridOutputChannel.required();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'onGridOutputChannelAddress' is not configured!");
				}
			});

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the Digital Output where the off grid connection of producer is connected to.", type = String.class)
	public ConfigChannel<String> offGridOutputChannelAddress = new ConfigChannel<String>("offGridOutputChannelAddress",
			this).addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						offGridOutputChannel = (WriteChannel<Boolean>) ch.get();
						offGridOutputChannel.required();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'offGridOutputChannelAddress' is not configured!");
				}
			});

	public AcIsland() {
		super();
	}

	public AcIsland(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			switch (currentState) {
			case OFFGRID:
				if (isProducerOffGrid() || isProducerOff()) {
					if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
						currentState = State.SWITCHTOONGRID;
					} else {
						if (ess.soc.value() >= maxSoc.value()) {
							offGridOutputChannel.pushWrite(false);
						} else if (ess.soc.value() <= minSoc.value()) {
							offGridOutputChannel.pushWrite(true);
						}
					}
				} else {
					currentState = State.SWITCHTOOFFGRID;
				}
				break;
			case ONGRID: {
				if (isProducerOnGrid()) {
					if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
						currentState = State.SWITCHTOOFFGRID;
					}
				} else {
					currentState = State.SWITCHTOONGRID;
				}
			}
				break;
			case SWITCHTOOFFGRID:
				if (isProducerOff()) {
					if (!isProducerDisconnected) {
						isProducerDisconnected = true;
						timeProducerDisconnected = System.currentTimeMillis();
					}
					if (timeProducerDisconnected + switchDelay.value() <= System.currentTimeMillis()
							&& isProducerDisconnected) {
						currentState = State.OFFGRID;
					}
				} else {
					isProducerDisconnected = false;
					onGridOutputChannel.pushWrite(false);
					offGridOutputChannel.pushWrite(false);
				}
				break;
			case SWITCHTOONGRID:
				if (isProducerOnGrid()) {
					currentState = State.ONGRID;
					isProducerDisconnected = false;
				} else {
					if (isProducerOff()) {
						if (!isProducerDisconnected) {
							isProducerDisconnected = true;
							timeProducerDisconnected = System.currentTimeMillis();
						}
						if (timeProducerDisconnected + switchDelay.value() <= System.currentTimeMillis()
								&& isProducerDisconnected) {
							onGridOutputChannel.pushWrite(true);
						}
					} else {
						isProducerDisconnected = false;
						onGridOutputChannel.pushWrite(false);
						offGridOutputChannel.pushWrite(false);
					}
				}
				break;
			default: {
				if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					currentState = State.SWITCHTOONGRID;
				} else if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
					currentState = State.SWITCHTOOFFGRID;
				}
			}
				break;

			}
		} catch (InvalidValueException e) {
			log.error("Failed to read value!", e);
		} catch (WriteChannelException e) {
			log.error("Failed to switch Output!", e);
		}
	}

	private boolean isProducerOff() throws InvalidValueException {
		return onGridOutputChannel.value() == false && offGridOutputChannel.value() == false;
	}

	private boolean isProducerOffGrid() throws InvalidValueException {
		return onGridOutputChannel.value() == false && offGridOutputChannel.value() == true;
	}

	private boolean isProducerOnGrid() throws InvalidValueException {
		return onGridOutputChannel.value() == true && offGridOutputChannel.value() == false;
	}

}
