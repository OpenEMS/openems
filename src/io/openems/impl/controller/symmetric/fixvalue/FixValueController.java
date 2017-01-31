package io.openems.impl.controller.symmetric.fixvalue;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class FixValueController extends Controller {
	@ConfigInfo(title = "All storage, which should be set to the p and q values.", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	@ConfigInfo(title = "The activePower to set for each storage.", type = Integer.class)
	public ConfigChannel<Integer> p = new ConfigChannel<Integer>("p", this);
	@ConfigInfo(title = "The reactivePower to set for each storage.", type = Integer.class)
	public ConfigChannel<Integer> q = new ConfigChannel<Integer>("q", this);

	public FixValueController() {
		super();
	}

	public FixValueController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
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
