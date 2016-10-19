package io.openems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.core.databus.Databus;
import io.openems.core.thing.ThingFactory;
import io.openems.demo.Demo;
import io.openems.demo.DemoFems7;
import io.openems.impl.device.commercial.FeneconCommercialEss;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		Demo demo = new DemoFems7();

		JsonObject config = demo.getConfig();

		Databus databus = ThingFactory.getFromConfig(config);
		databus.printAll();

		// TODO: this should be done by scheduler
		FeneconCommercialEss ess0 = (FeneconCommercialEss) databus.getThing("ess0");
		Channel soc = ess0.soc();
		ess0.getProtocol().setAsRequired(soc);

		//
		// log.info("Protocol Other: " + ess0.getProtocol().getOtherRanges());
		// log.info("Protocol Required: " + ess0.getProtocol().getRequiredRanges());
		//
		Thread.sleep(3000);

		log.info("ess0/soc: " + databus.getValue("ess0", "Soc"));
	}
}
