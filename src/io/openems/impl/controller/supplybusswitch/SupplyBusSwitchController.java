package io.openems.impl.controller.supplybusswitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
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
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

public class SupplyBusSwitchController extends Controller implements ChannelChangeListener {

	private HashMap<String, Supplybus> supplybus;

	private ThingRepository repo = ThingRepository.getInstance();

	@ConfigInfo(title = "collection of the switches for the supplyBus each array represents the switches for one supply bus.", type = JsonObject.class)
	public ConfigChannel<List<JsonObject>> supplyBuses = new ConfigChannel<List<JsonObject>>("supplyBuses", this)
			.changeListener(this);

	@ConfigInfo(title = "all ess which can be switcht to the supplyBus", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this).changeListener(this);

	public SupplyBusSwitchController() {
		super();
	}

	public SupplyBusSwitchController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			// what happens if switch command already sent but changed state is not read yet?
			// TODO handling for primary ess
			// TODO start/stop ess
			HashMap<Ess, Supplybus> activeEss = getActiveEss();
			List<Supplybus> inactiveBuses = new ArrayList<>();
			inactiveBuses.addAll(supplybus.values());
			inactiveBuses.remove(activeEss.values());
			List<Ess> inactiveEss = getInactiveEss(activeEss.keySet());
			// check if all supplybuses are connected
			if (inactiveBuses.size() > 0) {
				for (Supplybus sb : inactiveBuses) {
					Ess largestSocEss = getLargestSoc(inactiveEss);
					sb.connect(largestSocEss);
					inactiveEss.remove(largestSocEss);
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
			} else {
				for (Entry<Ess, Supplybus> entry : activeEss.entrySet()) {
					Ess largestSocEss = getLargestSoc(inactiveEss);
					if (entry.getKey().soc.value() < entry.getKey().minSoc.value() && largestSocEss.useableSoc() > 0) {
						entry.getValue().disconnect();
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
		if (supplyBuses.valueOptional().isPresent() && esss.valueOptional().isPresent()) {
			if (esss.valueOptional().get().size() <= supplyBuses.valueOptional().get().size()) {
				log.error("there must be one more ess than supply buses!");
			} else {
				supplybus = generatreSupplybuses();
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
		for (Supplybus sb : supplybus.values()) {
			Ess activeEss = sb.getActiveEss();
			if (activeEss != null) {
				activeEsss.put(activeEss, sb);
			}
		}
		return activeEsss;
	}

	private HashMap<String, Supplybus> generatreSupplybuses() {
		if (esss.valueOptional().isPresent() && supplyBuses.valueOptional().isPresent()) {
			HashMap<String, Supplybus> buses = new HashMap<>();
			for (JsonObject bus : supplyBuses.valueOptional().get()) {
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
					buses.put(name, sb);
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
