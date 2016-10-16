package io.openems;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.thing.Thing;
import io.openems.core.databus.DataBus;
import io.openems.core.thing.ThingFactory;
import io.openems.demo.Demo;
import io.openems.demo.DemoFems7;
import io.openems.impl.device.commercial.FeneconCommercialEss;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		Demo demo = new DemoFems7();

		JsonObject config = demo.getConfig();

		Map<String, Thing> things = ThingFactory.getFromConfig(config);
		// ThingFactory.printThings(things);

		FeneconCommercialEss ess0 = (FeneconCommercialEss) things.get("ess0");
		Channel soc = ess0.getSoc();
		ess0.getProtocol().setAsRequired(soc);

		DataBus dataBus = new DataBus();
		for (Entry<String, Thing> thing : things.entrySet()) {
			dataBus.addThing(thing.getKey(), thing.getValue());
		}
		dataBus.printAll();

		log.info("Protocol Other: " + ess0.getProtocol().getOtherRanges());
		log.info("Protocol Required: " + ess0.getProtocol().getRequiredRanges());

		Thread.sleep(3000);

		log.info("ess0/soc: " + dataBus.getValue("ess0", "soc"));
	}
}
