package io.openems.impl.controller.testwrite;

import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.io.OutputNature;

@IsThingMap(type = OutputNature.class)
public class Output extends ThingMap {

	public final WriteChannel<Boolean> output1;

	public Output(OutputNature thing) {
		super(thing);
		output1 = thing.setOutput()[0].required();
	}

}
