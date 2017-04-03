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
package io.openems.impl.controller.symmetric.avoidtotaldischargesoctimeline;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.symmetric.avoidtotaldischargesoctimeline.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Symmetric)", description = "Makes sure the battery is not going into critically low state of charge. For symmetric Ess.")
public class AvoidTotalDischargeSocTimeLineController extends Controller {

	/*
	 * Constructors
	 */
	public AvoidTotalDischargeSocTimeLineController() {
		super();
	}

	public AvoidTotalDischargeSocTimeLineController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ConfigInfo(title = "Soc timeline", description = "This option configures an minsoc at a time for an ess. If no minsoc for an ess is configured the controller uses the minsoc of the ess.", type = JsonArray.class)
	public final ConfigChannel<JsonArray> socTimeline = new ConfigChannel<JsonArray>("socTimeline", this)
			.addChangeListener(new ChannelChangeListener() {

				@Override
				public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
					if (newValue.isPresent() && newValue.get() instanceof JsonArray) {
						JsonArray timeline = (JsonArray) newValue.get();
						for (JsonElement e : timeline) {
							JsonObject obj = e.getAsJsonObject();
							int minSoc = obj.get("minSoc").getAsInt();
							int chargeSoc = obj.get("chargeSoc").getAsInt();
							LocalTime time = LocalTime.parse(obj.get("time").getAsString(),
									DateTimeFormatter.ISO_LOCAL_TIME);
							JsonArray storages = obj.get("esss").getAsJsonArray();
							for (JsonElement storage : storages) {
								Ess ess;
								try {
									ess = getEss(storage.getAsString());
									if (ess != null) {
										ess.addTime(time, minSoc, chargeSoc);
									}
								} catch (InvalidValueException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
					}
				}
			});

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			LocalTime time = LocalTime.now();
			for (Ess ess : esss.value()) {
				switch (ess.currentState) {
				case CHARGESOC:
					if (ess.soc.value() > ess.getMinSoc(time)) {
						ess.currentState = State.MINSOC;
					} else {
						try {
							Optional<Long> currentMinValue = ess.setActivePower.writeMin();
							if (currentMinValue.isPresent() && currentMinValue.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePower=Max[" + currentMinValue.get() / 5 + "]");
								ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePower=Max[-1000 W]");
								ess.setActivePower.pushWriteMax(-1000L);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				case MINSOC:
					if (ess.soc.value() < ess.getChargeSoc(time)) {
						ess.currentState = State.CHARGESOC;
					} else if (ess.soc.value() >= ess.getMinSoc(time) + 5) {
						ess.currentState = State.NORMAL;
					} else {
						try {
							long maxPower = 0;
							if (!ess.setActivePower.writeMax().isPresent()
									|| maxPower < ess.setActivePower.writeMax().get()) {
								ess.setActivePower.pushWriteMax(maxPower);
							}
						} catch (WriteChannelException e) {
							log.error(ess.id() + "Failed to set Max allowed power.", e);
						}
					}
					break;
				case NORMAL:
					if (ess.soc.value() <= ess.getMinSoc(time)) {
						ess.currentState = State.MINSOC;
					}
					break;
				}

			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

	private Ess getEss(String id) throws InvalidValueException {
		for (Ess ess : esss.value()) {
			if (ess.id().equals(id)) {
				return ess;
			}
		}
		return null;
	}

}
