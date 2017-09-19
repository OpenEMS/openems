package io.openems.impl.controller.riedmann;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "SystemStopController")
public class SystemStopController extends Controller {

	/*
	 * Config-Channel
	 */
	@ChannelInfo(title = "System Stop", description = "This configuration stops the system.", type = Boolean.class)
	public ConfigChannel<Boolean> signalSystemStop = new ConfigChannel<Boolean>("signalSystemStop", this);

	@ChannelInfo(title = "SPS", description = "The sps which should be controlled.", type = Custom.class)
	public ConfigChannel<Custom> sps = new ConfigChannel<>("sps", this);
	@ChannelInfo(title = "ESS", description = "The ess to stop on system stop. Also used for Off-Grid indication for the SPS. ", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	/*
	 * Attributes
	 */

	public SystemStopController() {
		super();
	}

	public SystemStopController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Custom sps = this.sps.value();
			// Grid-Mode
			try {
				if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
					sps.signalGridOn.pushWrite(0L);
				} else {
					sps.signalGridOn.pushWrite(1L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to set off-Grid indication to sps.", e);
			}
			// Stop
			try {
				if (signalSystemStop.value()) {
					ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
					sps.signalSystemStop.pushWrite(1L);
				} else {
					sps.signalSystemStop.pushWrite(0L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to set system stop!", e);
			}
		} catch (InvalidValueException e) {
			log.error("Can't read value!", e);
		}
	}

}
