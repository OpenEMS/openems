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
package io.openems.impl.controller.symmetric.timelinecharge;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

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
import io.openems.core.utilities.AvgFiFoQueue;
import io.openems.core.utilities.ControllerUtils;

@ThingInfo(title = "Timeline charge (Symmetric)")
public class TimelineChargeController extends Controller implements ChannelChangeListener {

	/*
	 * Constructors
	 */
	public TimelineChargeController() {
		super();
	}

	public TimelineChargeController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<>("meter", this);

	@ConfigInfo(title = "Max-ApparentPower", description = "How much apparent power the grid connection can take.", type = Long.class)
	public final ConfigChannel<Long> allowedApparent = new ConfigChannel<>("allowedApparent", this);

	@ConfigInfo(title = "Charger", description = "Sets the Chargers connected to the ess.", type = Charger.class, isArray = true)
	public final ConfigChannel<Set<Charger>> chargers = new ConfigChannel<Set<Charger>>("chargers", this);

	@ConfigInfo(title = "Soc Timeline", description = "soc to hold untill the next soc point.", type = JsonArray.class)
	public final ConfigChannel<JsonArray> socTimeline = new ConfigChannel<JsonArray>("SocTimeline", this)
			.addChangeListener(this);

	/*
	 * Fields
	 */
	private AvgFiFoQueue floatingChargerPower = new AvgFiFoQueue(10, 1);
	private State currentState = State.NORMAL;
	private TreeMap<LocalDateTime, Integer> socPoints = new TreeMap<>();

	public enum State {
		NORMAL, MINSOC, CHARGESOC
	}

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			long allowedApparentCharge = allowedApparent.value() - meter.value().apparentPower.value();
			allowedApparentCharge += ControllerUtils.calculateApparentPower(ess.activePower.valueOptional().orElse(0L),
					ess.reactivePower.valueOptional().orElse(0L));
			// remove 10% for tollerance
			allowedApparentCharge *= 0.9;
			// limit activePower to apparent
			try {
				ess.setActivePower.pushWriteMin(allowedApparentCharge * -1);
			} catch (WriteChannelException e) {
				log.error("Failed to set writeMin to " + (allowedApparentCharge * -1), e);
			}
			long chargerPower = 0L;
			for (Charger c : chargers.value()) {
				try {
					chargerPower += c.power.value();
				} catch (InvalidValueException e) {
					log.error("cant read power from " + c.id(), e);
				}
			}
			floatingChargerPower.add(chargerPower);
			Entry<LocalDateTime, Integer> socPoint = getSoc();
			double requiredEnergy = ((double) ess.capacity.value() / 100.0 * socPoint.getValue())
					- ((double) ess.capacity.value() / 100.0 * ess.soc.value());
			long requiredTimeCharger = (long) (requiredEnergy / floatingChargerPower.avg() * 3600.0);
			long requiredTimeGrid = (long) (requiredEnergy / (floatingChargerPower.avg() + allowedApparentCharge)
					* 3600.0);
			log.info("RequiredTimeCharger: " + requiredTimeCharger + ", RequiredTimeGrid: " + requiredTimeGrid);
			if (floatingChargerPower.avg() >= 1000
					&& !LocalDateTime.now().plusSeconds(requiredTimeCharger).isBefore(socPoint.getKey())
					&& LocalDateTime.now().plusSeconds(requiredTimeGrid).isBefore(socPoint.getKey())) {
				// Prevent discharge -> load with Pv
				ess.setActivePower.pushWriteMax(0L);
			} else if (!LocalDateTime.now().plusSeconds(requiredTimeGrid).isBefore(socPoint.getKey())
					&& socPoint.getKey().isAfter(LocalDateTime.now())) {
				// Load with grid + pv
				long maxPower = allowedApparentCharge * -1;
				if (ess.setActivePower.writeMin().isPresent() && ess.setActivePower.writeMin().get() > maxPower) {
					maxPower = ess.setActivePower.writeMin().get();
				}
				ess.setActivePower.pushWriteMax(maxPower);
			} else {
				// soc point in the past -> Hold load
				int minSoc = getCurrentSoc();
				int chargeSoc = minSoc - 5;
				if (chargeSoc <= 1) {
					chargeSoc = 1;
				}
				switch (currentState) {
				case CHARGESOC:
					if (ess.soc.value() > minSoc) {
						currentState = State.MINSOC;
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
						} catch (WriteChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				case MINSOC:
					if (ess.soc.value() < chargeSoc) {
						currentState = State.CHARGESOC;
					} else if (ess.soc.value() >= minSoc + 5) {
						currentState = State.NORMAL;
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
					if (ess.soc.value() <= minSoc) {
						currentState = State.MINSOC;
					}
					break;
				}
			}
		} catch (InvalidValueException e) {
			log.error("Can't read value", e);
		} catch (WriteChannelException e) {
			log.error("Can't write value", e);
		}
	}

	private Entry<LocalDateTime, Integer> getSoc() {
		Entry<LocalDateTime, Integer> lastSocPoint = socPoints.floorEntry(LocalDateTime.now());
		Entry<LocalDateTime, Integer> nextSocPoint = socPoints.higherEntry(LocalDateTime.now());
		if (nextSocPoint != null) {
			return nextSocPoint;
		}
		return lastSocPoint;
	}

	private int getCurrentSoc() {
		Entry<LocalDateTime, Integer> socPoint = socPoints.floorEntry(LocalDateTime.now());
		return socPoint.getValue();
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(socTimeline)) {
			if (newValue.isPresent()) {
				JsonArray array = (JsonArray) newValue.get();
				for (JsonElement e : array) {
					if (e.isJsonObject()) {
						JsonObject point = e.getAsJsonObject();
						LocalDateTime time = LocalDateTime.parse(point.get("time").getAsString(),
								DateTimeFormatter.ISO_DATE_TIME);
						Integer soc = point.get("soc").getAsInt();
						socPoints.put(time, soc);
					}
				}
			}
		}
	}

}
