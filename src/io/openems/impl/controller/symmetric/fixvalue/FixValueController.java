package io.openems.impl.controller.symmetric.fixvalue;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class FixValueController extends Controller {

	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this, Ess.class);

	public ConfigChannel<Integer> p = new ConfigChannel<Integer>("p", this, Integer.class);
	public ConfigChannel<Integer> q = new ConfigChannel<Integer>("q", this, Integer.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				try {
					ess.setActivePower.pushWrite((long) p.value());
					ess.setReactivePower.pushWrite((long) q.value());
				} catch (WriteChannelException | InvalidValueException e) {
					log.error("Failed to write fixed P/Q value for Ess " + ess.id, e);
				}
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
