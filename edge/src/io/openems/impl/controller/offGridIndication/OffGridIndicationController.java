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
package io.openems.impl.controller.offGridIndication;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

@ThingInfo(title = "OffGridIndicationController", description = "indicates with an digitalOutput if the system is Off-Grid.")
public class OffGridIndicationController extends Controller {

	private ThingRepository repo = ThingRepository.getInstance();
	private WriteChannel<Boolean> offGridOutputChannel;
	private State currentState = State.UNKNOWN;
	private boolean isProducerDisconnected = false;
	private long timeProducerDisconnected;
	private long startTime = System.currentTimeMillis();

	private enum State {
		OFFGRID, ONGRID, SWITCHTOOFFGRID, SWITCHTOONGRID, UNKNOWN
	}

	// ConfigChannel

	@ChannelInfo(title = "The ess where the grid state should be read from.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "time to wait before switch output on.", type = Long.class)
	public ConfigChannel<Long> switchDelay = new ConfigChannel<Long>("switchDelay", this).defaultValue(10000L);

	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the Digital Output to signal off-Grid.", type = String.class)
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

	public OffGridIndicationController() {
		super();
	}

	public OffGridIndicationController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		if (startTime + 1000 * 15 <= System.currentTimeMillis()) {
			try {
				Meter meter = this.meter.value();
				Ess ess = this.ess.value();
				switch (currentState) {
				case OFFGRID:
					if (isOffGrid()) {
						if (meter.voltage.valueOptional().isPresent()
								|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
							currentState = State.SWITCHTOONGRID;
						}
					} else {
						currentState = State.SWITCHTOOFFGRID;
					}
					break;
				case ONGRID: {
					if (isOff()) {
						if (!meter.voltage.valueOptional().isPresent()
								&& ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
							currentState = State.SWITCHTOOFFGRID;
						}
					} else {
						currentState = State.SWITCHTOONGRID;
					}
				}
					break;
				case SWITCHTOOFFGRID:
					if (isOff()) {
						if (!isProducerDisconnected) {
							isProducerDisconnected = true;
							timeProducerDisconnected = System.currentTimeMillis();
						}
						if (timeProducerDisconnected + switchDelay.value() <= System.currentTimeMillis()
								&& isProducerDisconnected) {
							offGridOutputChannel.pushWrite(true);
							currentState = State.OFFGRID;
						}
					} else {
						isProducerDisconnected = false;
						offGridOutputChannel.pushWrite(false);
					}
					break;
				case SWITCHTOONGRID:
					if (isOff()) {
						currentState = State.ONGRID;
					} else {
						offGridOutputChannel.pushWrite(false);
					}
					break;
				default: {
					if (meter.voltage.valueOptional().isPresent()
							|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
						if (isOff()) {
							currentState = State.ONGRID;
						} else {
							currentState = State.SWITCHTOONGRID;
						}
					} else {
						if (isOffGrid()) {
							currentState = State.OFFGRID;
						} else {
							currentState = State.SWITCHTOOFFGRID;
						}
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
	}

	private boolean isOffGrid() throws InvalidValueException {
		return offGridOutputChannel.value() == true;
	}

	private boolean isOff() throws InvalidValueException {
		return offGridOutputChannel.value() == false;
	}

}
