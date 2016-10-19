package io.openems.impl.controller.avoidtotaldischarge;

import java.math.BigInteger;
import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class AvoidTotalDischargeController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	private final BigInteger FIVE = BigInteger.valueOf(5);

	@Override
	public void run() {
		for (EssMap ess : esss) {
			try {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.getValue().compareTo(ess.minSoc.getValue()) <= 0
						&& ess.soc.getValue().compareTo(ess.minSoc.getValue().subtract(FIVE)) > 0) {
					// SOC > minSoc && > minSoc - 5
					ess.setActivePower.pushMaxWriteValue(BigInteger.ZERO);
				} else if (ess.soc.getValue().compareTo(ess.minSoc.getValue().subtract(FIVE)) < 0) {
					// SOC < minSoc - 5
					BigInteger currentMaxValue = ess.setActivePower.peekMaxWriteValue();
					if (currentMaxValue != null) {
						ess.setActivePower.pushMaxWriteValue(currentMaxValue.divide(FIVE));
					} else {
						ess.setActivePower.pushMaxWriteValue(BigInteger.valueOf(1000));
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
