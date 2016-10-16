package io.openems.impl.controller.avoidtotaldischarge;

import io.openems.api.channel.IsRequired;
import io.openems.api.controller.ThingMap;
import io.openems.api.value.Value;

public class EssMap extends ThingMap {

	@IsRequired(itemId = "Soc")
	public Value soc;

	public EssMap(String thingId) {
		super(thingId);
	}
}
