package io.openems.impl.controller.feneconprosetup;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class FeneconProSetupController extends Controller {

	@ConfigInfo(title = "Storages of type FeneconPro to run initial setup commands", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	public FeneconProSetupController() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FeneconProSetupController(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				if (ess.pcsMode.labelOptional().isPresent() && ess.pcsMode.labelOptional().get().equals("Debug")) {
					if (ess.setupMode.labelOptional().isPresent()
							&& ess.setupMode.labelOptional().get().equals(EssNature.ON)) {
						ess.setPcsMode.pushWriteFromLabel("Remote");
						log.info("Set " + ess.id() + " to Remote mode.");
					} else {
						log.info(ess.id() + " is not in Remote mode. Go to Setting Mode.");
						ess.setSetupMode.pushWriteFromLabel(EssNature.ON);
					}
				} else {
					if (ess.setupMode.labelOptional().isPresent()
							&& ess.setupMode.labelOptional().get().equals(EssNature.ON)) {
						ess.setSetupMode.pushWriteFromLabel(EssNature.OFF);
						log.info(ess.id() + " Switch setting mode Off");
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error("Failed to Finish Setup", e);
		}
	}

}
