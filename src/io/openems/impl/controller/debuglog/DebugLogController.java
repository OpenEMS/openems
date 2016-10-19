package io.openems.impl.controller.debuglog;

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;

public class DebugLogController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	@IsThingMapping
	public MeterMap meter = null;

	@Override
	public void run() {
		log.info("esss: " + esss);
		log.info("meter: " + meter);

		for (EssMap ess : esss) {
			log.info(ess.getThingId() + ": " + ess.toString());
		}
		log.info(meter.getThingId() + ": " + meter.toString());
	}

}
