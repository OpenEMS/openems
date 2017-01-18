package io.openems.impl.controller.supplybusswitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class Supplybus {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private HashMap<Ess, WriteChannel<Boolean>> switchEssMapping;

	private String name;

	public Supplybus(HashMap<Ess, WriteChannel<Boolean>> switchEssMapping, String name) {
		this.switchEssMapping = switchEssMapping;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Ess getActiveEss() throws SupplyBusException {
		List<Ess> activeEss = new ArrayList<>();
		for (Entry<Ess, WriteChannel<Boolean>> entry : switchEssMapping.entrySet()) {
			try {
				if (entry.getValue().value()) {
					activeEss.add(entry.getKey());
				}
			} catch (InvalidValueException e) {
				log.error("Failed to read digital output " + entry.getValue().address(), e);
			}
		}
		if (activeEss.size() > 1) {
			throw new SupplyBusException("there are more than one ess connected to the supply bus.", activeEss, this);
		} else if (activeEss.size() == 0) {
			return null;
		} else {
			return activeEss.get(0);
		}
	}

	public void disconnect() {
		for (WriteChannel<Boolean> io : switchEssMapping.values()) {
			try {
				io.pushWrite(false);
			} catch (WriteChannelException e) {
				log.error("Failed to write disconnect command to digital output " + io.address(), e);
			}
		}
	}

	public void connect(Ess ess) throws SupplyBusException {
		if (getActiveEss() == null) {
			WriteChannel<Boolean> sOn = switchEssMapping.get(ess);
			try {
				sOn.pushWrite(true);
				ess.start();
			} catch (WriteChannelException e) {
				log.error("Failed to write connect command to digital output " + sOn.address(), e);
			}
		}
	}
}
