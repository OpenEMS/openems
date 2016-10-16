package io.openems;

import java.util.Map;

import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.thing.Thing;
import io.openems.core.thing.ThingFactory;
import io.openems.demo.Demo;
import io.openems.demo.DemoFems7;
import io.openems.impl.device.commercial.FeneconCommercialEss;

public class App {

	public static void main(String[] args) throws Exception {
		Demo demo = new DemoFems7();

		JsonObject config = demo.getConfig();

		Map<String, Thing> things = ThingFactory.getFromConfig(config);
		// ThingFactory.printThings(things);

		FeneconCommercialEss ess0 = (FeneconCommercialEss) things.get("ess0");
		Channel soc = ess0.getSoc();
		ess0.getProtocol().setAsRequired(soc);

		System.out.println("Protocol Other: " + ess0.getProtocol().getOtherRanges());
		System.out.println("Protocol Required: " + ess0.getProtocol().getRequiredRanges());
	}
}
