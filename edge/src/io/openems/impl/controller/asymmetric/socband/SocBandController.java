package io.openems.impl.controller.asymmetric.socband;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "State-of-charge band", description = "controlling storage system to keep it within a certain state-of-charge band.")
public class SocBandController extends Controller {

	public SocBandController() {
		super();
	}

	public SocBandController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "ActivePower L1", description = "Fixed active power for phase L1.", type = Long.class)
	public final ConfigChannel<Long> activePowerL1 = new ConfigChannel<>("activePowerL1", this);

	@ChannelInfo(title = "ActivePower L2", description = "Fixed active power for phase L2.", type = Long.class)
	public final ConfigChannel<Long> activePowerL2 = new ConfigChannel<>("activePowerL2", this);

	@ChannelInfo(title = "ActivePower L3", description = "Fixed active power for phase L3.", type = Long.class)
	public final ConfigChannel<Long> activePowerL3 = new ConfigChannel<>("activePowerL3", this);

	@ChannelInfo(title = "Min SOC", description = ".", type = Long.class)
	public final ConfigChannel<Long> minSoc = new ConfigChannel<>("minSoc", this);

	@ChannelInfo(title = "Max Soc", description = ".", type = Long.class)
	public final ConfigChannel<Long> maxSoc = new ConfigChannel<>("maxSoc", this);

	public State currentState = State.UP;

	public enum State {
		UP, DOWN;
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();

			switch (currentState) {
			case DOWN:
				if (ess.soc.value() <= minSoc.value()) {
					currentState = State.UP;
				}
				System.out.println("Push 3000");
				ess.setActivePowerL1.pushWrite(activePowerL1.value());
				ess.setActivePowerL2.pushWrite(activePowerL2.value());
				ess.setActivePowerL3.pushWrite(activePowerL3.value());
				break;
			case UP:
				if (ess.soc.value() >= maxSoc.value()) {
					currentState = State.DOWN;
				}
				System.out.println("Push -3000");
				ess.setActivePowerL1.pushWrite(activePowerL1.value() * -1);
				ess.setActivePowerL2.pushWrite(activePowerL1.value() * -1);
				ess.setActivePowerL3.pushWrite(activePowerL1.value() * -1);
				break;
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}
}
