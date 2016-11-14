package io.openems.impl.controller.feneconprosetup;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class FeneconProSetupController extends Controller {

	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this, Ess.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				if (ess.workMode.labelOptional().isPresent() && !ess.workMode.labelOptional().equals("Remote")) {
					if (ess.setupMode.labelOptional().isPresent()
							&& ess.setupMode.labelOptional().equals(EssNature.ON)) {
						if (ess.setupFinished) {
							ess.setupMode.pushWriteFromLabel(EssNature.OFF);
						} else {
							ess.pcsMode.pushWriteFromLabel("Remote");
							ess.setupFinished = true;
						}
					} else {
						ess.setupMode.pushWriteFromLabel(EssNature.ON);
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error("Failed to Finish Setup", e);
		}
	}

}
