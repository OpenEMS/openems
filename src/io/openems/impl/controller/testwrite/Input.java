package io.openems.impl.controller.testwrite;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.io.InputNature;

@IsThingMap(type = InputNature.class)
public class Input extends ThingMap {

	public final ReadChannel<Boolean> input1;

	public Input(InputNature thing) {
		super(thing);
		input1 = thing.getInput()[0].required();
	}

}
