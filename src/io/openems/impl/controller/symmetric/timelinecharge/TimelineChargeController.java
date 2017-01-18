package io.openems.impl.controller.symmetric.timelinecharge;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;

public class TimelineChargeController extends Controller {

	@ConfigInfo(title = "list of Storages to controll", type = Ess.class)
	public final ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	@ConfigInfo(title = "Primary ess is necassary to reserve Load for the control hardware.", type = Ess.class)
	public final ConfigChannel<Ess> primaryEss = new ConfigChannel<Ess>("primaryEss", this);

	@ConfigInfo(title = "grid meter", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "how much power the grid connection can take.", type = Long.class)
	public final ConfigChannel<Long> allowedApparent = new ConfigChannel<>("allowedApparent", this);

	@ConfigInfo(title = "cosPhi to hold on the grid connection.", type = Double.class)
	public final ConfigChannel<Double> cosPhi = new ConfigChannel<>("cosPhi", this);

	public TimelineChargeController() {
		super();
	}

	public TimelineChargeController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			long allowedApparentCharge = meter.value().apparentPower.value() - allowedApparent.value();
			long socSum = primaryEss.value().soc.value();
			long currentReactivePower = primaryEss.value().reactivePower.value();
			// causes faster charge for primaryEss
			if (primaryEss.value().soc.value() > 10) {
				socSum = primaryEss.value().soc.value() - 10;
			}
			for (Ess ess : esss.value()) {
				currentReactivePower += ess.reactivePower.value();
				socSum += ess.soc.value();
			}
			//
			double cosPhi = this.cosPhi.value();
			double phi = Math.acos(cosPhi);
			long q = (long) ((meter.value().activePower.value() * Math.tan(phi)) - meter.value().reactivePower.value())
					* -1;
			q += currentReactivePower;
			long p = 0L;
			// primaryEss.value().power
			// .setActivePower(ControllerUtils.calculateActivePowerFromApparentPower(apparentPower, cosPhi));
			for (Ess ess : esss.value()) {

			}
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
