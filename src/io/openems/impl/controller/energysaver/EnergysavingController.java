package io.openems.impl.controller.energysaver;

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class EnergysavingController extends Controller {

	@IsThingMapping
	public List<Ess> esss = null;

	private Long lastTimeValueWritten = System.currentTimeMillis();

	@Override
	public void run() {
		for (Ess ess : esss) {
			try {
				/*
				 * Start ESS if it was stopped and we have a setActivePower command
				 */
				if (ess.setActivePower.hasWriteValue() && ess.setActivePower.getValue() != 0) {
					lastTimeValueWritten = System.currentTimeMillis();
					String systemState = ess.systemState.getValueLabelOrNull();
					if (systemState == null || systemState != EssNature.START) {
						log.info("ESS [" + ess.getThingId() + "] was stopped. Starting...");
						ess.setWorkState.pushWriteValue(EssNature.START);
					}
				} else {
					/*
					 * Stop Ess if no values were written since two minutes
					 */
					if (lastTimeValueWritten + 2 * 60 * 1000 < System.currentTimeMillis()) {
						String systemState = ess.systemState.getValueLabelOrNull();
						if (systemState == null || systemState != EssNature.STOP) {
							log.info("ESS [" + ess.getThingId()
									+ "] had no written value since two minutes. Stopping...");
							ess.setWorkState.pushWriteValue(EssNature.STOP);
						}
					}
				}
			} catch (WriteChannelException | InvalidValueException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
