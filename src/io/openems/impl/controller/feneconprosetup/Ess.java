package io.openems.impl.controller.feneconprosetup;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.pro.FeneconProEss;

@IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> workMode;
	public WriteChannel<Long> setPcsMode;
	public WriteChannel<Long> setSetupMode;
	public ReadChannel<Long> pcsMode;
	public ReadChannel<Long> setupMode;

	public Ess(FeneconProEss ess) {
		super(ess);
		workMode = ess.workMode.required();
		setPcsMode = ess.setPcsMode;
		setSetupMode = ess.setSetupMode;
		pcsMode = ess.pcsMode;
		setupMode = ess.setupMode;
	}

}
