package io.openems.impl.controller.avoidtotaldischarge;

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;

public class AvoidTotalDischargeController extends Controller {

	@IsThingMapping
	public List<EssMap> esss = null;

	@Override
	public int getPriority() {
		return 100;
	}

	@Override
	public void run() {
		for (EssMap ess : esss) {
			log.info("AvoidTotalDischarge. SOC: " + ess.soc.toSimpleString());
			// if (ess.soc.getReadValue().getValue().asInt() <= minSoc
			// && e.soc.getReadValue().getValue().asInt() > minSoc - 5) {
			// e.activePower.setRange(e.activePower.getMinWriteValue(), new IntegerValue(0));
			// } else if (e.soc.getReadValue().getValue().asInt() <= minSoc - 5) {
			// e.activePower.setRange(e.activePower.getMinWriteValue(),
			// new IntegerValue((int) (e.activePower.getMinWriteValue().asDouble() * 0.2)));
			// }
		}
	}
}
