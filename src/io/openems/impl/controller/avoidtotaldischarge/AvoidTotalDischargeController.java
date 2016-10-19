package io.openems.impl.controller.avoidtotaldischarge;

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class AvoidTotalDischargeController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	@Override
	public void run() {
		for (EssMap ess : esss) {
			try {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.getValue() < ess.minSoc.getValue() && ess.soc.getValue() >= ess.minSoc.getValue() - 5) {
					// SOC < minSoc && SOC > minSoc - 5
					ess.setActivePower.pushMaxWriteValue(0);
				} else if (ess.soc.getValue() < ess.minSoc.getValue() - 5) {
					// SOC < minSoc - 5
					Long currentMaxValue = ess.setActivePower.peekMaxWriteValue();
					if (currentMaxValue != null) {
						ess.setActivePower.pushMaxWriteValue(currentMaxValue / 5);
					} else {
						ess.setActivePower.pushMaxWriteValue(1000);
					}
				}
				/*
				 * Start ESS if it was stopped and we have a setActivePower command
				 */
				if (ess.setActivePower.hasWriteValue()) {
					String systemState = ess.systemState.getValueLabelOrNull();
					if (systemState == null || systemState != "Start") {
						log.info("ESS [" + ess.getThingId() + "] was stopped. Starting...");
						ess.setWorkState.pushWriteValue("Start");
					}
				}
			} catch (InvalidValueException | WriteChannelException e) {
				log.error(e.getMessage());
			}
		}
	}
}
