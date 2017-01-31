package io.openems.impl.controller.symmetric.balancingcosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;

public class BalancingCosPhiController extends Controller {
	@ConfigInfo(title = "The storage, which should be controlled", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "The meter which meassures the power from/to the grid", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "The cosPhi to hold on the grid meter.", type = Double.class)
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	public BalancingCosPhiController() {
		super();
	}

	public BalancingCosPhiController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			double cosPhi = this.cosPhi.value();
			double phi = Math.acos(cosPhi);
			long q = (long) ((meter.value().activePower.value() * Math.tan(phi)) - meter.value().reactivePower.value())
					* -1;
			q += ess.value().reactivePower.value();
			ess.value().power.setReactivePower(q);
			ess.value().power.writePower();
			log.info(ess.id() + " Set ReactivePower [" + ess.value().power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read value.", e);
		}
	}

}
