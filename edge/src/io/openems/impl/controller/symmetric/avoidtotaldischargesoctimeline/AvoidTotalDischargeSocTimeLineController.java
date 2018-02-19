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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.PowerException;
import io.openems.impl.controller.symmetric.avoidtotaldischargesoctimeline.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Symmetric)", description = "Makes sure the battery is not going into critically low state of charge. For symmetric Ess.")
public class AvoidTotalDischargeSocTimeLineController extends Controller implements ChannelChangeListener {

	private ThingStateChannels thingState = new ThingStateChannels(this);
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
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this).addChangeListener(this);

	@ChannelInfo(title = "Soc timeline", description = "This option configures an minsoc at a time for an ess. If no minsoc for an ess is configured the controller uses the minsoc of the ess.", type = JsonArray.class)
	public final ConfigChannel<JsonArray> socTimeline = new ConfigChannel<JsonArray>("socTimeline", this)
	.addChangeListener(this);
	@ChannelInfo(title = "Next Discharge", description = "Next Time, the ess will discharge completely.", type = String.class, defaultValue = "2018-03-09")
	public final ConfigChannel<String> nextDischarge = new ConfigChannel<String>("nextDischarge", this)
	.addChangeListener(this);
	@ChannelInfo(title = "Discharge Period", description = "The Period of time between two Discharges.https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-", type = String.class, defaultValue = "P4W")
	public final ConfigChannel<String> dischargePeriod = new ConfigChannel<String>("dischargePeriod", this)
	.addChangeListener(this);
	@ChannelInfo(title = "Discharge Start Time", description = "The time of the Day to start Discharging.", type = String.class, defaultValue = "12:00:00")
	public final ConfigChannel<String> dischargeTime = new ConfigChannel<String>("dischargeTime", this)
	.addChangeListener(this);
	@ChannelInfo(title = "Enable Discharge", description = "This option allowes the system to discharge the ess according to the nextDischarge completely. This improves the soc calculation.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> enableDischarge = new ConfigChannel<Boolean>("EnableDischarge", this);

	private LocalDate nextDischargeDate;
	private LocalTime dischargeStartTime;
	private LocalDateTime dischargeStart;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			LocalTime time = LocalTime.now();
			for (Ess ess : esss.value()) {
				if(dischargeStart != null && dischargeStart.isBefore(LocalDateTime.now()) && ess.currentState != Ess.State.EMPTY ) {
					ess.currentState = Ess.State.EMPTY;
				}
				switch (ess.currentState) {
				case CHARGESOC:
					if (ess.soc.value() > ess.getMinSoc(time)) {
						ess.currentState = State.MINSOC;
					} else {
						try {
							ess.maxActivePowerLimit.setP(Math.abs(ess.maxNominalPower.valueOptional().orElse(1000L))*-1);
							ess.power.applyLimitation(ess.maxActivePowerLimit);
						} catch (PowerException e) {
							log.error("Failed to set Power!",e);
						}
					}
					break;
				case MINSOC:
					if (ess.soc.value() < ess.getChargeSoc(time)) {
						ess.currentState = State.CHARGESOC;
					} else if (ess.soc.value() >= ess.getMinSoc(time) + 5) {
						ess.currentState = State.NORMAL;
					} else {
						ess.maxActivePowerLimit.setP(0L);
						try {
							ess.power.applyLimitation(ess.maxActivePowerLimit);
						} catch (PowerException e) {
							log.error("Failed to set Power!",e);
						}
					}
					break;
				case NORMAL:
					if (ess.soc.value() <= ess.getMinSoc(time)) {
						ess.currentState = State.MINSOC;
					}
					break;
				case EMPTY:
					if (ess.allowedDischarge.value() == 0 || ess.soc.value() < 1) {
						// Ess is Empty set Date and charge to minSoc
						addPeriod();
						ess.currentState = State.CHARGESOC;
					}else {
						//Force discharge with max power
						try {
							ess.minActivePowerLimit.setP(Math.abs(ess.maxNominalPower.value()));
							ess.power.applyLimitation(ess.minActivePowerLimit);
						} catch (PowerException e) {
							log.error("Failed to force Discharge!", e);
						}
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

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(esss) || channel.equals(socTimeline)) {
			if (esss.valueOptional().isPresent() && socTimeline.valueOptional().isPresent()
					&& socTimeline.valueOptional().get() instanceof JsonArray) {
				JsonArray timeline = socTimeline.valueOptional().get();
				for (JsonElement e : timeline) {
					JsonObject obj = e.getAsJsonObject();
					int minSoc = obj.get("minSoc").getAsInt();
					int chargeSoc = obj.get("chargeSoc").getAsInt();
					LocalTime time = LocalTime.parse(obj.get("time").getAsString(), DateTimeFormatter.ISO_LOCAL_TIME);
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
		} else if (this.nextDischarge.equals(channel)) {
			if (newValue.isPresent()) {
				nextDischargeDate = LocalDate.parse((String) newValue.get());
			} else {
				nextDischargeDate = null;
			}
		}  else if(this.dischargeTime.equals(channel)) {
			if (newValue.isPresent()) {
				this.dischargeStartTime = LocalTime.parse((String) newValue.get());
			} else {
				this.dischargeStartTime = null;
			}
		}
		if (nextDischargeDate != null && nextDischargeDate.isBefore(LocalDate.now())) {
			addPeriod();
		}
		if(nextDischargeDate != null && dischargeStartTime != null) {
			dischargeStart = nextDischargeDate.atTime(dischargeStartTime);
		}else {
			dischargeStart = null;
		}
	}

	private void addPeriod() {
		if (this.nextDischargeDate != null && dischargePeriod.isValuePresent()) {
			this.nextDischargeDate = this.nextDischargeDate.plus(Period.parse(dischargePeriod.getValue()));
			nextDischarge.updateValue(this.nextDischargeDate.toString(), true);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
