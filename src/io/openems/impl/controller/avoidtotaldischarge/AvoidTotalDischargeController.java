package io.openems.impl.controller.avoidtotaldischarge;

import java.math.BigInteger;
import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.exception.InvalidValueException;

public class AvoidTotalDischargeController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	private final BigInteger FIVE = BigInteger.valueOf(5);

	@Override
	public void run() {
		for (EssMap ess : esss) {
			try {
				log.info("AvoidTotalDischarge. SOC: " + ess.soc.toSimpleString() + ". MinSoc: "
						+ ess.minSoc.toSimpleString());
				if (ess.soc.getValue().compareTo(ess.minSoc.getValue()) <= 0
						&& ess.soc.getValue().compareTo(ess.minSoc.getValue().subtract(FIVE)) > 0) {
					// SOC > minSoc && > minSoc - 5
					ess.setActivePower.setMaxWriteValue(BigInteger.ZERO);
				} else if (ess.soc.getValue().compareTo(ess.minSoc.getValue().subtract(FIVE)) < 0) {
					// SOC < minSoc - 5
					ess.setActivePower.setMaxWriteValue(ess.setActivePower.getMaxWriteValue().divide(FIVE));
				}
			} catch (InvalidValueException e) {
				log.error(e.getMessage());
			}
		}
	}
}
