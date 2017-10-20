package io.openems.impl.controller.asymmetricsymmetriccombination;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "starts power calculation of AsymmetricSymmetricCombination Ess device")
public class AsymmetricSymmetricCombinationController extends Controller {

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);



	public AsymmetricSymmetricCombinationController() {
		super();
	}



	public AsymmetricSymmetricCombinationController(String thingId) {
		super(thingId);
	}



	@Override
	public void run() {
		try {
			ess.value().calculate();
		} catch (WriteChannelException | InvalidValueException e) {
			log.error("failed to write combined power", e);
		}
	}

}
