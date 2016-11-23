package io.openems.impl.controller.asymmetric.avoidtotaldischarge;

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class AvoidTotalDischargeController extends Controller {

	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this, Ess.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.value() < ess.minSoc.value() && ess.soc.value() >= ess.minSoc.value() - 5) {
					// SOC < minSoc && SOC >= minSoc - 5
					log.info("Avoid discharge. Set ActivePower=Max[0]");
					ess.setActivePowerL1.pushWriteMax(0L);
					ess.setActivePowerL2.pushWriteMax(0L);
					ess.setActivePowerL3.pushWriteMax(0L);
				} else if (ess.soc.value() < ess.minSoc.value() - 5) {
					// SOC < minSoc - 5
					Optional<Long> currentMinValueL1 = ess.setActivePowerL1.writeMin();
					Optional<Long> currentMinValueL2 = ess.setActivePowerL2.writeMin();
					Optional<Long> currentMinValueL3 = ess.setActivePowerL3.writeMin();
					if (currentMinValueL1.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL1.get() / 5 + "]");
						ess.setActivePowerL1.pushWriteMax(currentMinValueL1.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL1.pushWriteMax(-1000L);
					}
					if (currentMinValueL2.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL2.get() / 5 + "]");
						ess.setActivePowerL2.pushWriteMax(currentMinValueL2.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL2.pushWriteMax(-1000L);
					}
					if (currentMinValueL3.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL3.get() / 5 + "]");
						ess.setActivePowerL3.pushWriteMax(currentMinValueL3.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL3.pushWriteMax(-1000L);
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
