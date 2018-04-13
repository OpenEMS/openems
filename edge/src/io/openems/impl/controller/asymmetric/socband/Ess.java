package io.openems.impl.controller.asymmetric.socband;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {
	public ReadChannel<Long> soc;
	public WriteChannel<Long> setActivePowerL1;
	public WriteChannel<Long> setActivePowerL2;
	public WriteChannel<Long> setActivePowerL3;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		setActivePowerL1 = ess.setActivePowerL1().required();
		setActivePowerL2 = ess.setActivePowerL2().required();
		setActivePowerL3 = ess.setActivePowerL3().required();
		soc = ess.soc().required();
	}
}