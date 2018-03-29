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
package io.openems.impl.controller.symmetric.cosphicharacteristic;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.symmetric.PowerException;

@ThingInfo(title = "Cos-Phi Characteristics (Symmetric)")
public class CosPhiCharacteristicController extends Controller implements ChannelChangeListener{

	private final Logger log = LoggerFactory.getLogger(CosPhiCharacteristicController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public CosPhiCharacteristicController() {
		super();
	}

	public CosPhiCharacteristicController(String id) {
		super(id);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this).addChangeListener(this);

	@ChannelInfo(title = "Cos-Phi characteristic", description = "The points of the characteristic (x = signed activePower, y = cosPhi IEEE Power Factor Sign Convention ).", type = Double[].class, isArray = true)
	public ConfigChannel<List<Double[]>> cosPhiPoints = new ConfigChannel<List<Double[]>>("cosPhiPoints", this)
	.addChangeListener(this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			ess.value().power.applyLimitation(ess.value().limit);
		} catch (InvalidValueException | PowerException e) {
			log.error("Failed to set power limitation!",e);
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if(ess.isValuePresent()) {
			TreeMap<Long,Double> points = new TreeMap<>();
			if(cosPhiPoints.isValuePresent()) {
				for(Double[] point : cosPhiPoints.getValue()) {
					points.put(point[0].longValue(), point[1]);
				}
			}
			ess.getValue().limit.setCosPhi(0L, 0L, points);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
