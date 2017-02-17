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
package io.openems.impl.controller.supplybusswitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

// TODO better explanation
@ThingInfo(title = "Supply Bus Switch")
public class SupplyBusSwitchController extends Controller implements ChannelChangeListener {

	/*
	 * Constructors
	 */
	public SupplyBusSwitchController() {
		super();
	}

	public SupplyBusSwitchController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Supply-bus", description = "Collection of the switches for the supplyBus each array represents the switches for one supply bus.", type = JsonObject.class)
	public ConfigChannel<List<JsonObject>> supplyBusConfig = new ConfigChannel<List<JsonObject>>("supplyBuses", this)
			.addChangeListener(this);

	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this).addChangeListener(this);

	@ConfigInfo(title = "Primary-Ess", description = "OpenEMS is supplied by this Ess. Will reserve some load.", type = Ess.class)
	public final ConfigChannel<Ess> primaryEss = new ConfigChannel<Ess>("primaryEss", this);

	/*
	 * Fields
	 */
	private List<Supplybus> supplybuses;
	private ThingRepository repo = ThingRepository.getInstance();

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			// TODO handling for primary ess
			HashMap<Ess, Supplybus> activeEss = getActiveEss();
			List<Ess> inactiveEss = getInactiveEss(activeEss.keySet());

			for (Supplybus sb : supplybuses) {
				switch (sb.getState()) {
				case CONNECTED: {
					Ess active = sb.getActiveEss();
					// check if ess with lager soc is available
					Ess mostLoad = getLargestSoc(inactiveEss);
					if (active.soc.value() < active.minSoc.value() && mostLoad.useableSoc() > 0) {
						sb.disconnect();
						try {
							active.stop();
						} catch (WriteChannelException e) {
							log.error("Can't stop ess[" + active.id() + "]", e);
						}
					}
				}
					break;
				case CONNECTING: {
					// if not connected send connect command again
					if (!sb.isConnected()) {
						sb.connect(sb.getActiveEss());
					}
				}
					break;
				case DISCONNECTED: {
					Ess mostLoad = getLargestSoc(inactiveEss);
					// only connect if soc is larger than 4% or Ess is On-Grid
					if (mostLoad.soc.value() > 4
							|| mostLoad.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
						try {
							mostLoad.start();
							sb.connect(mostLoad);
						} catch (WriteChannelException e) {
							log.error("Can't start ess[" + mostLoad.id() + "]", e);
						}
					}
				}
					break;
				case DISCONNECTING: {
					// if not disconnected send disconnect command again
					if (!sb.isDisconnected()) {
						sb.disconnect();
					}
				}
					break;
				default:
					break;

				}
			}
			if (isOnGrid()) {
				// start all ess
				for (Ess ess : esss.value()) {
					try {
						ess.start();
					} catch (WriteChannelException e) {
						log.error("Failed to start " + ess.id(), e);
					}
				}
			}
		} catch (InvalidValueException e1) {
			log.error("failed to get collection of configured 'esss'", e1);
		} catch (SupplyBusException e) {
			e.supplybus.disconnect();
			log.error("there was more than one connection to " + e.supplybus.getName(), e);
		}
	}

	private boolean isOnGrid() throws InvalidValueException {
		for (Ess ess : esss.value()) {
			if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (supplyBusConfig.valueOptional().isPresent() && esss.valueOptional().isPresent()) {
			if (esss.valueOptional().get().size() <= supplyBusConfig.valueOptional().get().size()) {
				log.error("there must be one more ess than supply buses!");
			} else {
				supplybuses = generateSupplybuses();
			}
		}
	}

	private List<Ess> getInactiveEss(Collection<Ess> activeEss) throws InvalidValueException {
		List<Ess> inactiveEsss = new ArrayList<>();
		inactiveEsss.addAll(esss.value());
		inactiveEsss.removeAll(activeEss);
		return inactiveEsss;
	}

	private Ess getLargestSoc(List<Ess> esss) {
		Ess largestSoc = null;
		for (Ess ess : esss) {
			try {
				if (largestSoc == null || largestSoc.useableSoc() < ess.useableSoc()) {
					largestSoc = ess;
				}
			} catch (InvalidValueException e) {
				log.error("failed to read soc of " + ess.id(), e);
			}
		}
		return largestSoc;
	}

	private HashMap<Ess, Supplybus> getActiveEss() throws SupplyBusException {
		HashMap<Ess, Supplybus> activeEsss = new HashMap<>();
		for (Supplybus sb : supplybuses) {
			Ess activeEss = sb.getActiveEss();
			if (activeEss != null) {
				activeEsss.put(activeEss, sb);
			}
		}
		return activeEsss;
	}

	private List<Supplybus> generateSupplybuses() {
		if (esss.valueOptional().isPresent() && supplyBusConfig.valueOptional().isPresent()) {
			List<Supplybus> buses = new ArrayList<>();
			for (JsonObject bus : supplyBusConfig.valueOptional().get()) {
				try {
					String name = JsonUtils.getAsString(bus, "bus");
					HashMap<Ess, WriteChannel<Boolean>> switchEssMapping = new HashMap<>();
					JsonArray switches = JsonUtils.getAsJsonArray(bus, "switches");
					for (JsonElement e : switches) {
						try {
							String essName = JsonUtils.getAsString(e, "ess");
							try {
								Ess ess = getEss(essName);
								String channelAddress = JsonUtils.getAsString(e, "switchAddress");
								Optional<Channel> outputChannel = repo.getChannelByAddress(channelAddress);
								if (ess != null && outputChannel.isPresent()
										&& outputChannel.get() instanceof WriteChannel<?>) {
									switchEssMapping.put(ess, (WriteChannel<Boolean>) outputChannel.get());
								}
							} catch (InvalidValueException e1) {
								log.error(essName + " is missing in the 'esss' config parameter", e1);
							}
						} catch (ReflectionException e2) {
							log.error("can't find JsonElement 'ess' or 'switchAddress'!", e2);
						}
					}
					Supplybus sb = new Supplybus(switchEssMapping, name);
					buses.add(sb);
				} catch (ReflectionException e) {
					log.error("can't find JsonElement 'bus' or 'switches' in config parameter 'supplyBuses'!", e);
				}
			}
			return buses;
		} else {
			return null;
		}
	}

	private Ess getEss(String essName) throws InvalidValueException {
		for (Ess ess : esss.value()) {
			if (ess.id().equals(essName)) {
				return ess;
			}
		}
		return null;
	}

}
