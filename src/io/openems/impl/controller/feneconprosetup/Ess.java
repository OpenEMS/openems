package io.openems.impl.controller.feneconprosetup;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.pro.FeneconProEss;

@IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> workMode;
	public WriteChannel<Long> pcsMode;
	public WriteChannel<Long> setupMode;

	public boolean setupFinished = false;

	public Ess(FeneconProEss ess) {
		super(ess);
		workMode = ess.workMode.required();
		pcsMode = ess.setPcsMode;
		setupMode = ess.setSetupMode;
	}

}
