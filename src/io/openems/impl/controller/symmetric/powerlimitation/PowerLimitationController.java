package io.openems.impl.controller.symmetric.powerlimitation;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class PowerLimitationController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Long> pMin = new ConfigChannel<Long>("pMin", this, Long.class);
	public ConfigChannel<Long> pMax = new ConfigChannel<Long>("pMax", this, Long.class);
	public ConfigChannel<Long> qMin = new ConfigChannel<Long>("qMin", this, Long.class);
	public ConfigChannel<Long> qMax = new ConfigChannel<Long>("qMax", this, Long.class);

	@Override public void run() {
		try {
			try {
				ess.value().setActivePower.pushWriteMax(pMax.value());
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Max P value for Ess " + ess.value().id, e);
			}
			try {
				ess.value().setActivePower.pushWriteMin(pMin.value());
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Min P value for Ess " + ess.value().id, e);
			}
			try {
				ess.value().setReactivePower.pushWriteMin(qMin.value());
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Min Q value for Ess " + ess.value().id, e);
			}
			try {
				ess.value().setReactivePower.pushWriteMax(qMax.value());
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Max Q value for Ess " + ess.value().id, e);
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
