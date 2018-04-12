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
package io.openems.impl.controller.debuglogforkaco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.ThingRepository;
import io.openems.impl.device.blueplanet50tl3.FeneconBlueplanet50TL3Ess;

@ThingInfo(title = "Output debugging information on systemlog")
public class DebugLogController extends Controller {

	private final Logger log = LoggerFactory.getLogger(DebugLogController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public DebugLogController() {
		super();
	}

	public DebugLogController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class, isOptional = true)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		FeneconBlueplanet50TL3Ess ess = this.ess.getValue().ess;
		log.info(ess.id() + ": " + ess.getClass());
		for(Channel channel : ThingRepository.getInstance().getChannels(ess)) {
			if(channel  instanceof ReadChannel<?>) {
				ReadChannel<?> c = (ReadChannel<?>) channel;
				try {
					log.info(c.id() + ": " + c.value());
				} catch (InvalidValueException e) {
				}
			}
		}
		System.out.println("-");
		System.out.println("-");
		System.out.println("-");
		System.out.println("-");
		System.out.println("-");
		System.out.println("-");

	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
