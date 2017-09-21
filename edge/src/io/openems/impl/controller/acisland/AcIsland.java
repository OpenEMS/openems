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
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

@ThingInfo(title = "Use AC-PV in offgrid situation")
public class AcIsland extends Controller {

	private ThingRepository repo = ThingRepository.getInstance();
	private Optional<WriteChannel<Boolean>> onGridOutputChannel;
	private Optional<WriteChannel<Boolean>> offGridOutputChannel;
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
						this.onGridOutputChannel = Optional.of( //
								((WriteChannel<Boolean>) ch.get()).required());
						// TODO should not be necessary to set outputChannel as required
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
						this.offGridOutputChannel = Optional.of( //
								((WriteChannel<Boolean>) ch.get()).required());
						// TODO should not be necessary to set outputChannel as required
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
		// Get all required values - or abort with error
		Ess ess;
		long maxSoc;
		long minSoc;
		long soc;
		String gridMode;
		boolean isProducerOff;
		boolean isProducerOffGrid;
		boolean isProducerOnGrid;
		long switchDelay;
		try {
			ess = this.ess.value();
			soc = ess.soc.value();
			gridMode = ess.gridMode.labelOptional().get();
			maxSoc = this.maxSoc.value();
			minSoc = this.minSoc.value();
			isProducerOff = this.isProducerOff();
			isProducerOffGrid = this.isProducerOffGrid();
			isProducerOnGrid = this.isProducerOnGrid();
			switchDelay = this.switchDelay.value();
		} catch (InvalidValueException | ConfigException e) {
			log.error(e.getMessage());
			return;
		}

		try {
			switch (currentState) {
			case OFFGRID:
				if (isProducerOffGrid || isProducerOff) {
					if (gridMode.equals(EssNature.ON_GRID)) {
						currentState = State.SWITCHTOONGRID;
					} else {
						if (soc >= maxSoc) {
							this.getOffGridOutputChannel().pushWrite(false);
						} else if (soc <= minSoc) {
							this.getOffGridOutputChannel().pushWrite(true);
						}
					}
				} else {
					currentState = State.SWITCHTOOFFGRID;
				}
				break;
			case ONGRID: {
				if (isProducerOnGrid) {
					if (gridMode.equals(EssNature.OFF_GRID)) {
						currentState = State.SWITCHTOOFFGRID;
					}
				} else {
					currentState = State.SWITCHTOONGRID;
				}
			}
				break;
			case SWITCHTOOFFGRID:
				if (isProducerOff) {
					if (!isProducerDisconnected) {
						isProducerDisconnected = true;
						timeProducerDisconnected = System.currentTimeMillis();
					}
					if (timeProducerDisconnected + switchDelay <= System.currentTimeMillis()
							&& isProducerDisconnected) {
						currentState = State.OFFGRID;
					}
				} else {
					isProducerDisconnected = false;
					getOnGridOutputChannel().pushWrite(false);
					getOffGridOutputChannel().pushWrite(false);
				}
				break;
			case SWITCHTOONGRID:
				if (isProducerOnGrid) {
					currentState = State.ONGRID;
					isProducerDisconnected = false;
				} else {
					if (isProducerOff) {
						if (!isProducerDisconnected) {
							isProducerDisconnected = true;
							timeProducerDisconnected = System.currentTimeMillis();
						}
						if (timeProducerDisconnected + switchDelay <= System.currentTimeMillis()
								&& isProducerDisconnected) {
							getOnGridOutputChannel().pushWrite(true);
						}
					} else {
						isProducerDisconnected = false;
						getOnGridOutputChannel().pushWrite(false);
						getOffGridOutputChannel().pushWrite(false);
					}
				}
				break;
			default: {
				if (gridMode.equals(EssNature.ON_GRID)) {
					currentState = State.SWITCHTOONGRID;
				} else if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
					currentState = State.SWITCHTOOFFGRID;
				}
			}
				break;

			}
		} catch (WriteChannelException | ConfigException e) {
			log.error("Failed to switch Output!", e);
		}
	}

	private boolean isProducerOff() throws InvalidValueException, ConfigException {
		WriteChannel<Boolean> onGridOutputChannel = this.getOnGridOutputChannel();
		WriteChannel<Boolean> offGridOutputChannel = this.getOffGridOutputChannel();
		return onGridOutputChannel.value() == false && offGridOutputChannel.value() == false;
	}

	private boolean isProducerOffGrid() throws InvalidValueException, ConfigException {
		WriteChannel<Boolean> onGridOutputChannel = this.getOnGridOutputChannel();
		WriteChannel<Boolean> offGridOutputChannel = this.getOffGridOutputChannel();
		return onGridOutputChannel.value() == false && offGridOutputChannel.value() == true;
	}

	private boolean isProducerOnGrid() throws InvalidValueException, ConfigException {
		WriteChannel<Boolean> onGridOutputChannel = this.getOnGridOutputChannel();
		WriteChannel<Boolean> offGridOutputChannel = this.getOffGridOutputChannel();
		return onGridOutputChannel.value() == true && offGridOutputChannel.value() == false;
	}

	private WriteChannel<Boolean> getOnGridOutputChannel() throws ConfigException {
		if (this.onGridOutputChannel.isPresent()) {
			return this.onGridOutputChannel.get();
		} else {
			throw new ConfigException("onGridOutputChannel is not available.");
		}
	}

	private WriteChannel<Boolean> getOffGridOutputChannel() throws ConfigException {
		if (this.offGridOutputChannel.isPresent()) {
			return this.offGridOutputChannel.get();
		} else {
			throw new ConfigException("offGridOutputChannel is not available.");
		}
	}
}
