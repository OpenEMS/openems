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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
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
	@ChannelInfo(title = "Supply-bus", description = "Collection of the switches for the supplyBus each array represents the switches for one supply bus.", type = JsonArray.class)
	public ConfigChannel<JsonArray> supplyBusConfig = new ConfigChannel<JsonArray>("supplyBusConfig", this)
	.addChangeListener(this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this).addChangeListener(this);

	@ChannelInfo(title = "Switch-Delay", description = "delay to expire between ess disconnected and next ess connected.", type = Long.class, defaultValue = "10000")
	public final ConfigChannel<Long> switchDelay = new ConfigChannel<Long>("switchDelay", this);

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

			for (Supplybus sb : supplybuses) {
				sb.run();
			}
			for (Ess ess : esss.value()) {
				if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					// start all ess
					ess.start();
				}
				try {
					ess.setWorkState();
				} catch (WriteChannelException e) {
					log.error("Can't set Workstate for ess[" + ess.id() + "]", e);
				}
			}
		} catch (InvalidValueException e1) {
			log.error("failed to get collection of configured 'esss'", e1);
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(supplyBusConfig) || channel.equals(esss)) {
			if (supplyBusConfig.valueOptional().isPresent() && esss.valueOptional().isPresent()) {
				if (esss.valueOptional().get().size() <= supplyBusConfig.valueOptional().get().size()) {
					log.error("there must be one more ess than supply buses!");
				} else {
					supplybuses = generateSupplybuses();
				}
			}
		} else if (channel.equals(switchDelay)) {
			if (supplybuses != null) {
				for (Supplybus sb : supplybuses) {
					try {
						sb.setSwitchDelay(switchDelay.value());
					} catch (InvalidValueException e) {
						log.error("failed to read switchDelay config.", e);
					}
				}
			}
		}
	}

	private List<Supplybus> generateSupplybuses() {
		if (esss.valueOptional().isPresent() && supplyBusConfig.valueOptional().isPresent()
				&& switchDelay.valueOptional().isPresent()) {
			List<Supplybus> buses = new ArrayList<>();
			for (JsonElement bus : supplyBusConfig.valueOptional().get()) {
				try {
					String name = JsonUtils.getAsString(bus, "bus");
					String primaryEssName = JsonUtils.getAsString(bus, "primaryEss");
					JsonElement supplybusOnIndicationElement = bus.getAsJsonObject().get("supplybusOnIndication");
					JsonElement loads = bus.getAsJsonObject().get("loads");
					Optional<Channel> supplybusOnIndication = Optional.empty();
					if (supplybusOnIndicationElement != null) {
						supplybusOnIndication = repo.getChannelByAddress(supplybusOnIndicationElement.getAsString());
					}
					List<WriteChannel<Long>> loadChannels = new ArrayList<>();
					if (loads != null) {
						for (JsonElement load : loads.getAsJsonArray()) {
							Optional<Channel> loadChannel = repo.getChannelByAddress(load.getAsString());
							if (loadChannel.isPresent() && loadChannel.get() instanceof WriteChannel<?>) {
								@SuppressWarnings("unchecked") WriteChannel<Long> writeChannel = (WriteChannel<Long>) loadChannel
										.get();
								loadChannels.add(writeChannel);
							}
						}
					}
					Ess primaryEss = getEss(primaryEssName);
					HashMap<Ess, WriteChannel<Boolean>> switchEssMapping = new HashMap<>();
					JsonArray switches = JsonUtils.getAsJsonArray(bus, "switches");
					for (JsonElement e : switches) {
						try {
							String essName = JsonUtils.getAsString(e, "ess");
							try {
								Ess ess = getEss(essName);
								String channelAddress = JsonUtils.getAsString(e, "switchAddress");
								Optional<Channel> outputChannel = repo.getChannelByAddress(channelAddress);
								if (ess != null) {
									if (outputChannel.isPresent() && outputChannel.get() instanceof WriteChannel<?>) {
										@SuppressWarnings("unchecked") WriteChannel<Boolean> channel = (WriteChannel<Boolean>) outputChannel
												.get();
										channel.required();
										switchEssMapping.put(ess, channel);
									} else {
										log.error(channelAddress + " not found!");
									}
								} else {
									log.error(essName + "not found!");
								}
							} catch (InvalidValueException e1) {
								log.error(essName + " is missing in the 'esss' config parameter", e1);
							}
						} catch (ReflectionException e2) {
							log.error("can't find JsonElement 'ess' or 'switchAddress'!", e2);
						}
					}
					WriteChannel<Long> supplybusOnIndicationChannel = null;
					if (supplybusOnIndication.isPresent()) {
						if (supplybusOnIndication.get() instanceof WriteChannel<?>) {
							@SuppressWarnings("unchecked") WriteChannel<Long> writeChannel = (WriteChannel<Long>) supplybusOnIndication
									.get();
							supplybusOnIndicationChannel = writeChannel;
						}
					}
					Supplybus sb = new Supplybus(switchEssMapping, name, primaryEss, switchDelay.value(),
							supplybusOnIndicationChannel, loadChannels);
					buses.add(sb);
				} catch (ReflectionException e) {
					log.error("can't find JsonElement 'bus' or 'switches' in config parameter 'supplyBuses'!", e);
				} catch (InvalidValueException e3) {
					log.error("primaryEss not found", e3);
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
